package com.hackathon.platform.plagiarism;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.dto.FunctionMatchResponse;
import com.hackathon.platform.dto.MatchedRangeResponse;
import com.hackathon.platform.dto.PlagiarismDiffResponse;
import com.hackathon.platform.dto.SourceFileResponse;
import com.hackathon.platform.dto.SubmissionSimilarityResponse;
import com.hackathon.platform.model.Level;
import com.hackathon.platform.model.PlagiarismRun;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.model.SubmissionSimilarity;
import com.hackathon.platform.plagiarism.ast.AstFunctionSpan;
import com.hackathon.platform.plagiarism.embedding.EmbeddingService;
import com.hackathon.platform.plagiarism.embedding.EmbeddingSimilarityCalculator;
import com.hackathon.platform.plagiarism.embedding.FunctionEmbeddingStore;
import com.hackathon.platform.plagiarism.fingerprint.Winnowing;
import com.hackathon.platform.plagiarism.fingerprint.Winnowing.FingerprintResult;
import com.hackathon.platform.plagiarism.normalize.NormalizedToken;
import com.hackathon.platform.plagiarism.normalize.StructuralNormalizationResult;
import com.hackathon.platform.plagiarism.normalize.StructuralNormalizer;
import com.hackathon.platform.repository.LeaderboardEntry;
import com.hackathon.platform.repository.LevelRepository;
import com.hackathon.platform.repository.PlagiarismRunRepository;
import com.hackathon.platform.repository.SubmissionRepository;
import com.hackathon.platform.repository.SubmissionSimilarityRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.service.StorageService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batch, admin-triggered plagiarism check across the top N teams on a level's (or event's)
 * leaderboard. Deliberately NOT run on every submission: it's meant to run once a level (or the
 * whole event) has closed, against each team's best/final submission.
 * Two independent similarity signals feed the combined score saved per pair
 */
@Service
@RequiredArgsConstructor
public class PlagiarismCheckService {

    private static final Logger logger =  LoggerFactory.getLogger(PlagiarismCheckService.class);

    private final SubmissionRepository submissionRepo;
    private final SubmissionSimilarityRepository similarityRepo;
    private final PlagiarismRunRepository runRepo;
    private final LevelRepository levelRepo;
    private final TeamRepository teamRepo;
    private final StorageService storageService;
    private final AzureBlobConfig blobConfig;
    private final StructuralNormalizer structuralNormalizer;
    private final EmbeddingService embeddingService;
    private final FunctionEmbeddingStore functionEmbeddingStore;
    private final EmbeddingSimilarityCalculator embeddingSimilarityCalculator;
    private final Winnowing winnowing;
    private final PlagiarismProperties props;

    /**
     * A submission's normalized tokens (with source offsets, for diff highlighting), the raw
     * content of every source file it contained (for rendering diff view), and any function
     * spans discovered per file.
     */
    private record SubmissionSource(
        Map<String, String> filesByName,
        List<NormalizedToken> tokens,
        Map<String, List<AstFunctionSpan>> functionsByFile
    ) [
        List<String> tokenTexts() {
            List<String> texts = new ArrayList<>(tokens.size());
            for(NormalizedToken t : tokens) {
                texts.add(t.text());
            }
            return texts;

        }

    ]

    /** One compared pair before the final flag decision */
    private record PairResult(
        Submission a,
        Submission b,
        double structuralScore,
        Double embeddingScore,
        double combinedScore,
        int matchedKgramCount
    ) {}

    /**Executes one plagiarism run */
    @Transactional
    public void execute(Long runId) {
        PlagiarismRun run =
            runRepo.findById(runId).orElseThrow(() -> new IllegalArgumentException("run not found"));
        run.setStatus("RUNNING");
        runRepo.save(run);

        try {
            int compared = 0;
            int flagged = 0;
            List<Short> levelIds =
                run.getLevelId() != null
                    ? List.of(run.getLevelId())
                    : levelRepo.findAll().stream().map(Level::getId).toList();
            
            for(short levelId : levelIds) {
                
                RunOutcome outcome =  runForLevel(run.getEventId(), levelId, run.getTopN());
                compared += outcome.compared;
                flagged += outcome.flagged;

            }

            run.setPairsCompared(compared);
            run.setPairsFlagged(flagged);
            run.setStatus("COMPLETED");
            run.setCompletedAt(java.time.Instant.now());

        } catch (Exception e) {
            logger.error("Plagiarism run {} failed", runId, e);
            run.setStatus("FAILED");
            run.setErrorMessage(truncate(e.getMessage(), 500));
            run.setCompletedAt(java.time.Instant.now());

        }
        runRepo.save(run);
    }

    private record RunOutcome(int compared, int flagged) {}

    /**Runs the check for single level. Clear any prior results for this */
    private RunOutcome runForLevel(UUID eventId, short levelId, int topN) {
        List<LeaderboardEntry> leaderboard =
            submissionRepo.findLeaderboardByEventIdAndLevelId(eventId, levelId);
        List<UUID> topTeamIds =
            leaderboard.stream().limit(topN).map(LeaderboardEntry::getTeamId).toList();
        if(topTeamIds.isEmpty()) {
            return new RunOutcome(0,0);

        }

        List<Submission> submissions =
            submissionRepo.findBestScoredForTeamsAndLevel(levelId, topTeamIds);
        if(submissions.size() < 2) {
            logger.info(
                "Plagiarism check: fewer than 2 scored submissions for event {} level {}, skipping",
                eventId,
                levelId
            );
            return new RunOutcome(0,0);
        }

        //Fingerprint every sub once
        Map<Long, FingerprintResult> fingerprints = new HashMap<>();
        for(Submission sub : submissions) {
            SubmissionSource source = fetchAndNormalize(sub);
            List<String> tokens = source.tokenTexts();
            if(tokens.size() < props.getMinTokenCount()) {
                logger.debug(
                    "Submission {} has only {} normalized tokens, skipping (too trivial to compare)",
                    sub.getId(),
                    tokens.size()
                );
                
                continue;
            }
            fingerprints.put(
                sub.getId(),
                winnowing.fingerprint(tokens, props.getKgramSize(), props.getWindowSize())
            );

            embeddingService.embedAndStore(sub.getId(), source.filesByName(), source.functionsByFile());
        }

        //Centering embedding signal needs to see every function embedding across the whole level's run before any pair is compared.
        List<Long> embeddedSubmissionIds = new ArrayList<>(fingerprint.keySet());
        List<FunctionEmbeddingStore.StoredEmbedding> allEmbeddings =
            functionEmbeddingStore.findBySubmissionIds(embeddedSubmissionIds);

        Map<Long, List<FunctionEmbeddingStore.StoredEmbedding>> rawEmbeddingsBySubmission =
            allEmbeddings.stream()
                .collect(java.util.stream.Collectors.groupingBy(FunctionEmbeddingStore.StoredEmbedding::submissionId));

        float[] levelMeanVector =
            embeddingSimilarityCalculator.meanVector(
                allEmbeddings.stream().map(FunctionEmbeddingStore.StoredEmbedding::vector).toList()
            );

        Map<Long, List<FunctionEmbeddingStore.StoredEmbedding>> centeredEmbeddingsBySubmission = new HashMap<>();
        for (Long submissionId : embeddedSubmissionIds){

            List<FunctionEmbeddingStore.StoredEmbedding> raw =
                rawEmbeddingsBySubmission.getOrDefault(submissionId, List.of());
            centeredEmbeddingsBySubmission.put(
                submissionId, embeddingSimilarityCalculator.centerAll(raw, levelMeanVector)
            );

        }

        similarityRepo.deleteByEventIdAndLevelId(eventId, levelId);

        //Pass 1: compute every pair's scores without deciding flagged yet. Relative threshold needs whole level.
        List<PairResult> pairResults = new ArrayList<>();

        for(int i = 0; i < submissions.size(); i++) {
            Submission a = submissions.get(i);
            FingerprintResult fpA = fingerprints.get(a.getId());
            if(fpA == null) {
                continue;

            }

            for(int j = i + 1; j < submissions.size(); j++) {
                Submission b = submissions.get(j);
                if(a.getTeamId().equals(b.getTeamId())) {
                    continue;
                }
                FingerprintResult fpB = fingerprints.get(b.getId());
                if(fpB == null) {
                    continue;
                }

                double structural = winnowing.jaccard(fpA.hashes(), fpB.hashes());
                int matchedCount = countSharedHashes(fpA, fpB);
                boolean isFlagged = structural >= props.getFlagThreshold();
                compared++;
                if(isFlagged) {
                    flagged++;
                }

                Long lower = Math.min(a.getId(), b.getId());
                Long higher = Math.max(a.getId(), b.getId());
                UUID teamLower = a.getId().equals(lower) ? a.getTeamId() : b.getTeamId();
                UUID teamHigher = a.getId().equals(lower) ? b.getTeamId() : a.getTeamId();

                BigDecimal score = BigDecimal.valueOf(structural).setScale(4, RoundingMode.HALF_UP);
                toSave.add(
                    new SubmissionSimilarity(
                        eventId,
                        levelId,
                        lower,
                        higher,
                        teamLower,
                        teamHigher,
                        score,
                        null, // embedding_score reserved for future ML similarity signal
                        score, // combined_score is just structural score for now. Need to add second signal.
                        matchedCount,
                        isFlagged
                    )
                );

            }
        }
        similarityRepo.saveAll(toSave);
        return new RunOutcome(compared, flagged);
    }

    private int countSharedHashes(FingerprintResult a, FingerprintResult b) {
        var bHashes = b.hashes();
        return (int) a.hashes().stream().filter(bHashes::contains).count();

    }

    private List<String> fetchAndNormalize(Submission sub) {
        byte[] bytes = downloadSourceBytes(sub);
        if(isZip(bytes)) {
            return normalizeZipArchive(bytes);

        }

        //Defensive fallback in case non-zip
        CodeNormalizer.Lang lang = normalizer.detectLanguage(sub.getSourceFileName());
        return normalizer.normalize(new String(bytes, StandardCharsets.UTF_8), lang);

    }

    private static final java.util.Set<String> SOURCE_EXTENSIONS =
        java.util.Set.of(
            "java", "py", "c", "h", "cpp", "hpp", "cs", "js", "ts", "jsx", "tsx", "go", "rs", "kt", "swift"
        );
    
    private boolean isZip(byte[] bytes) {

        return bytes.length >= 4
            && bytes[0] == 'P'
            && bytes[1] == 'K'
            && (bytes[2] == 3 || bytes[2] == 5);
    }

    /**Submissions are uploaded as a zipped source archive. This unpacks every recognized source file
     * in the archive, normalizes each, and concats the results in a stable order so fingerprinting sees
     * one continuous structural token stream per submission.
     */
    private List<String> normalizeZipArchive(byte[] zipBytes) {

        java.util.Map<String, byte[]> entries = new java.util.TreeMap<>();
        try(java.util.zip.ZipInputStream zis =
            new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
          java.util.zip.ZipEntry entry;
          while ((entry = zis.getNextEntry()) != null) {
            if(entry.isDirectory()) {
                continue;
            }

            String name = entry.getName();
            String ext =  extensionOf(name);
            if(!SOURCE_EXTENSIONS.contains(ext)) {
                continue;
            }

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            zis.transferTo(buf);
            entries.put(name, buf.toByteArray());

          }    

        } catch (IOException e) {
            logger.warn("Could not read submission zip archive: {}", e.getMessage());
            return List.of();
        }

        List<String> tokens =  new ArrayList<>();
        for(var e : entries.entrySet()) {
          CodeNormalizer.Lang lang = normalizer.detectLanguage(e.getKey());
          tokens.addAll(normalizer.normalize(new String(e.getValue(), StandardCharsets.UTF_8), lang));

        }
        return tokens;
        

    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);

    }

    private byte[] downloadSourceBytes(Submission sub) {
        try (InputStream in =
            storageService.download(
                blobConfig.getSubmissionsContainer(), sub.getSourceCodeStorageKey()
            )) {

          ByteArrayOutputStream buf = new ByteArrayOutputStream();
          in.transferTo(buf);
          return buf.toByteArray();

        } catch (IOException e) {
            logger.warn(
                "Could not download source for submission {} ({}): {}",
                sub.getId(),
                sub.getSourceCodeStorageKey(),
                e.getMessage()
            );
            return new byte[0];

        }
    }

    @Transactional(readOnly = true)
    public List<SubmissionSimilarityResponse> getResults(UUID eventId, Short levelId, boolean onlyFlagged) {
        List<SubmissionSimilarity> rows =
            levelId != null
                ? (onlyFlagged
                    ? similarityRepo.findByEventIdAndLevelIdAndFlaggedTrueOrderByCombinedScoreDesc(
                        eventId, levelId
                    )
                    : similarityRepo.findByEventIdAndLevelIdOrderByCombinedScoreDesc(eventId, levelId))
                : similarityRepo.findByEventIdOrderByCombinedScoreDesc(eventId);
        
        Map<UUID, String> teamNames = new HashMap<>();
        List<SubmissionSimilarityResponse> out = new ArrayList<>(rows.size());

        for(SubmissionSimilarity row : rows) {
            if(onlyFlagged && levelId == null && !row.isFlagged()) {
                continue;
            }
            String nameA =
                teamNames.computeIfAbsent(
                    row.getTeamIdA(), id -> teamRepo.findById(id).map(t -> t.getTeamName()).orElse("?")
                );
            String nameB =
                teamNames.computeIfAbsent(
                    row.getTeamIdB(), id -> teamRepo.findById(id).map(t -> t.getTeamName()).orElse("?")
                );
            
            out.add(
                new SubmissionSimilarityResponse(
                    row.getId(),
                    row.getLevelId(),
                    row.getSubmissionIdA(),
                    row.getSubmissionIdB(),
                    row.getTeamIdA(),
                    nameA,
                    row.getTeamIdB(),
                    nameB,
                    row.getStructuralScore(),
                    row.getEmbeddingScore(),
                    row.getCombinedScore(),
                    row.getMatchedKgramCount(),
                    row.isFlagged(),
                    row.getRunAt()
                )
            );
        }
        return out;
    }

    /**
     * Recomputes normalized tokens for flagged pair on demand (not persisted) for the admin UI diff highlighting.
     * Wow moment is showing which code matched, not just score.
     */
    @Transactional(readOnly = true)
    public PlagiarismDiffResponse getDiff(Long submissionIdA, Long submissionIdB) {
        Submission a =
            submissionRepo
                .findById(submissionIdA)
                .orElseThrow(() -> new IllegalArgumentException("submission not found: " + submissionIdA));
        
        Submission b =
            submissionRepo
                .findById(submissionIdB)
                .orElseThrow(() -> new IllegalArgumentException("submission not found: " + submissionIdB));

        List<String> tokensA = fetchAndNormalize(a);
        List<String> tokensB = fetchAndNormalize(b);

        FingerprintResult fpA =  winnowing.fingerprint(tokensA, props.getKgramSize(), props.getWindowSize());
        FingerprintResult fpB =  winnowing.fingerprint(tokensB, props.getKgramSize(), props.getWindowSize());

        double structural = winnowing.jaccard(fpA.hashes(), fpB.hashes());

        var sharedHashes = fpA.hashes();
        sharedHashes.retainAll(fpB.hashes());

        List<int[]> rangesA = matchedRanges(fpA, sharedHashes, props.getKgramSize());
        List<int[]> rangesB = matchedRanges(fpB, sharedHashes, props.getKgramSize());

        return new PlagiarismDiffResponse(submissionIdA, submissionIdB, tokensA, tokensB, rangesA, rangesB, structural);

        
    }

    private List<int[]> matchedRanges(
        FingerprintResult fp, java.util.Set<Long> sharedHashes, int kgramSize
    ) {
        List<int[]> ranges = new ArrayList<>();
        for(var f : fp.fingerprints()) {
            if(sharedHashes.contains(f.hash())) {
                ranges.add(new int[] {f.position(), f.position() + kgramSize});

            }
        }

        ranges.sort((x,y) -> Integer.compare(x[0], y[0]));
        return ranges;

    }

    private String truncate(String s, int max) {
        if(s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }


}