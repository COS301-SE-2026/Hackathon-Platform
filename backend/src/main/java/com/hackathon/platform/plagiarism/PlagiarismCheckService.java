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
    ) {
        List<String> tokenTexts() {
            List<String> texts = new ArrayList<>(tokens.size());
            for(NormalizedToken t : tokens) {
                texts.add(t.text());
            }
            return texts;

        }

    }

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
        List<Long> embeddedSubmissionIds = new ArrayList<>(fingerprints.keySet());
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
                
                Double embedding =
                    embeddingSimilarityCalculator
                        .symmetricBestMatch(
                            centeredEmbeddingsBySubmission.getOrDefault(a.getId(), List.of()),
                            centeredEmbeddingsBySubmission.getOrDefault(b.getId(), List.of()))
                        .orElse(null);

                double combined = combinedScore(structural, embedding);

                pairResults.add(new PairResult(a, b, structural, embedding, combined, matchedCount));
            }
        }

        // Pass 2: decide flagged per pair using both the absolute threshold and the level-relative statistical threshold. 
        double meanCombined = mean(pairResults);
        double stddevCombined = stddev(pairResults, meanCombined);
        boolean relativeThresholdTrusted =
            props.isUseRelativeThreshold() && pairResults.size() >= props.getMinPairsForRelativeThreshold();
        double relativeCutoff = meanCombined + props.getRelativeThresholdZScore() * stddevCombined;

        int compared = 0;
        int flagged = 0;
        List<SubmissionSimilarity> toSave = new ArrayList<>();

        for (PairResult pr : pairResults) {
            boolean isFlagged =
                pr.combinedScore() >= props.getFlagThreshold()
                    || (relativeThresholdTrusted && pr.combinedScore() >= relativeCutoff);

            compared++;
            if (isFlagged) {
                flagged++;
            }

            Long lower = Math.min(pr.a().getId(), pr.b().getId());
            Long higher = Math.max(pr.a().getId(), pr.b().getId());
            UUID teamLower = pr.a().getId().equals(lower) ? pr.a().getTeamId() : pr.b().getTeamId();
            UUID teamHigher = pr.a().getId().equals(lower) ? pr.b().getTeamId() : pr.a().getTeamId();

            BigDecimal structuralScore =
                BigDecimal.valueOf(pr.structuralScore()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal embeddingScore =
                pr.embeddingScore() == null
                    ? null
                    : BigDecimal.valueOf(pr.embeddingScore()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal combinedScoreValue =
                BigDecimal.valueOf(pr.combinedScore()).setScale(4, RoundingMode.HALF_UP);

            toSave.add(
                new SubmissionSimilarity(
                    eventId,
                    levelId,
                    lower,
                    higher,
                    teamLower,
                    teamHigher,
                    structuralScore,
                    embeddingScore,
                    combinedScoreValue,
                    pr.matchedKgramCount(),
                    isFlagged
                )
            );
        
        }
        similarityRepo.saveAll(toSave);
        return new RunOutcome(compared, flagged);
    }

    /**
     * Weighted combination of the structural and semantic signals. Falls to just structual if no embedding signal.
     */
    private double combinedScore(double structural, Double embedding) {
        if (embedding == null){
            return structural;
        }

        double structuralWeight = props.getStructuralWeight();
        double embeddingWeight = props.getEmbeddingWeight();
        double totalWeight = structuralWeight + embeddingWeight;
        if (totalWeight <= 0) {

            return structural;
        }
        return (structuralWeight * structural + embeddingWeight * embedding) / totalWeight;
    }

    private double mean(List<PairResult> pairs) {
        if(pairs.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for(PairResult p : pairs) {
            sum += p.combinedScore();
        }
        return sum / pairs.size();
    }

    /**Population standard deviation */
    private double stddev(List<PairResult> pairs, double mean) {
        
        if (pairs.size() < 2) {
            return 0.0;
        }

        double sumSquaredDiff = 0.0;
        for (PairResult p : pairs) {
            double diff = p.combinedScore() - mean;
            sumSquaredDiff += diff * diff;

        }
        return Math.sqrt(sumSquaredDiff / pairs.size());

    }



    private int countSharedHashes(FingerprintResult a, FingerprintResult b) {
        var bHashes = b.hashes();
        return (int) a.hashes().stream().filter(bHashes::contains).count();

    }

    private SubmissionSource fetchAndNormalize(Submission sub) {
        byte[] bytes = downloadSourceBytes(sub);
        if(isZip(bytes)) {
            return normalizeZipArchive(bytes);

        }

        //Defensive fallback in case non-zip
        String fileName = sub.getSourceFileName() == null ? "submission" : sub.getSourceFileName();
        String content = new String(bytes, StandardCharsets.UTF_8);
        StructuralNormalizationResult normalized = structuralNormalizer.normalize(fileName, content);
        return new SubmissionSource(
            Map.of(fileName, content),
            normalized.tokens(),
            Map.of(fileName, normalized.functions())
        );

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
    private SubmissionSource normalizeZipArchive(byte[] zipBytes) {

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
            return new SubmissionSource(Map.of(), List.of(), Map.of());
        }

        Map<String, String> filesByName = new LinkedHashMap<>();
        Map<String, List<AstFunctionSpan>> functionsByFile = new LinkedHashMap<>();
        List<NormalizedToken> tokens =  new ArrayList<>();
        
        for(var e : entries.entrySet()) {

          String fileName = e.getKey();
          String content = new String(e.getValue(), StandardCharsets.UTF_8);
          filesByName.put(fileName, content);

          StructuralNormalizationResult normalized = structuralNormalizer.normalize(fileName, content);
          tokens.addAll(normalized.tokens());
          functionsByFile.put(fileName, normalized.functions());

        }

        return new SubmissionSource(filesByName, tokens, functionsByFile);
        

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

        SubmissionSource srcA = fetchAndNormalize(a);
        SubmissionSource srcB = fetchAndNormalize(b);

       FingerprintResult fpA =
            winnowing.fingerprint(srcA.tokenTexts(), props.getKgramSize(), props.getWindowSize());
        FingerprintResult fpB =
            winnowing.fingerprint(srcB.tokenTexts(), props.getKgramSize(), props.getWindowSize());


        double structural = winnowing.jaccard(fpA.hashes(), fpB.hashes());

        var sharedHashes = fpA.hashes();
        sharedHashes.retainAll(fpB.hashes());

        List<MatchedRangeResponse> rangesA =
            matchedRanges(fpA, sharedHashes, props.getKgramSize(), srcA.tokens(), srcA.filesByName());
        List<MatchedRangeResponse> rangesB =
            matchedRanges(fpB, sharedHashes, props.getKgramSize(), srcB.tokens(), srcB.filesByName());

        List<SourceFileResponse> filesA = toFileResponses(srcA.filesByName());
        List<SourceFileResponse> filesB = toFileResponses(srcB.filesByName());

        FunctionMatchOutcome functionOutcome = computeFunctionMatches(a, b, srcA, srcB);

        return new PlagiarismDiffResponse(
            submissionIdA,
            submissionIdB,
            filesA,
            filesB,
            rangesA,
            rangesB,
            structural,
            functionOutcome.matches(),
            functionOutcome.status());
    }

    /** Bundles the function-match list together with WHY it looks the way it does */
    private record FunctionMatchOutcome(
        List<FunctionMatchResponse> matches, PlagiarismDiffResponse.SemanticStatus status) {}

    /**
     * Function-level semantic matches for the diff view
     */
    private FunctionMatchOutcome computeFunctionMatches(
        Submission a, Submission b, SubmissionSource srcA, SubmissionSource srcB) {

        List<FunctionEmbeddingStore.StoredEmbedding> rawA =
            functionEmbeddingStore.findBySubmissionId(a.getId());
        List<FunctionEmbeddingStore.StoredEmbedding> rawB =
            functionEmbeddingStore.findBySubmissionId(b.getId());

        if (rawA.isEmpty() && rawB.isEmpty()) {
            return new FunctionMatchOutcome(List.of(), PlagiarismDiffResponse.SemanticStatus.NO_DATA_FOR_EITHER);
        }
        if (rawA.isEmpty()) {
            return new FunctionMatchOutcome(List.of(), PlagiarismDiffResponse.SemanticStatus.NO_DATA_FOR_A);
        }
        if (rawB.isEmpty()) {
            return new FunctionMatchOutcome(List.of(), PlagiarismDiffResponse.SemanticStatus.NO_DATA_FOR_B);
        }

        Set<Long> corpusIds = new java.util.HashSet<>();
        corpusIds.add(a.getId());
        corpusIds.add(b.getId());

        for (SubmissionSimilarity row :
            similarityRepo.findByEventIdAndLevelIdOrderByCombinedScoreDesc(a.getEventId(), a.getLevelId())) {
            corpusIds.add(row.getSubmissionIdA());
            corpusIds.add(row.getSubmissionIdB());


        }

        List<FunctionEmbeddingStore.StoredEmbedding> corpus =
            functionEmbeddingStore.findBySubmissionIds(new ArrayList<>(corpusIds));
        float[] meanVector =
            embeddingSimilarityCalculator.meanVector(corpus.stream().map(e -> e.vector()).toList());

        List<FunctionEmbeddingStore.StoredEmbedding> centeredA =
            embeddingSimilarityCalculator.centerAll(rawA, meanVector);
        List<FunctionEmbeddingStore.StoredEmbedding> centeredB =
            embeddingSimilarityCalculator.centerAll(rawB, meanVector);

        List<EmbeddingSimilarityCalculator.FunctionMatch> matches =
            embeddingSimilarityCalculator.topFunctionMatches(
                centeredA, centeredB, props.getFunctionMatchThreshold(), props.getMaxFunctionMatches());

        if (matches.isEmpty()) {
            return new FunctionMatchOutcome(
                List.of(), PlagiarismDiffResponse.SemanticStatus.NO_MATCHES_ABOVE_THRESHOLD);
        }

        Map<String, SpanLocation> locationsA = flattenSpans(srcA.functionsByFile());
        Map<String, SpanLocation> locationsB = flattenSpans(srcB.functionsByFile());

        List<FunctionMatchResponse> result = new ArrayList<>(matches.size());
        for (EmbeddingSimilarityCalculator.FunctionMatch m : matches) {
            SpanLocation locA = locationsA.get(m.qualifiedNameA());
            SpanLocation locB = locationsB.get(m.qualifiedNameB());
            if (locA == null || locB == null) {
                continue; // shouldn't happen - embedding exists but its span disappeared on re-parse
            }
            result.add(
                new FunctionMatchResponse(
                    locA.fileName(),
                    m.qualifiedNameA(),
                    locA.span().startByte(),
                    locA.span().endByte(),
                    locB.fileName(),
                    m.qualifiedNameB(),
                    locB.span().startByte(),
                    locB.span().endByte(),
                    m.similarity()));
        }

        // Every match's span lookup failing is defensive-only and shouldn't happen in practice.
        return result.isEmpty()
            ? new FunctionMatchOutcome(List.of(), PlagiarismDiffResponse.SemanticStatus.NO_MATCHES_ABOVE_THRESHOLD)
            : new FunctionMatchOutcome(result, PlagiarismDiffResponse.SemanticStatus.MATCHED);
    }

    /** Where one qualified function name lives: which file, and its byte span within it. */
    private record SpanLocation(String fileName, AstFunctionSpan span) {}

    private Map<String, SpanLocation> flattenSpans(Map<String, List<AstFunctionSpan>> functionsByFile) {
        Map<String, SpanLocation> out = new HashMap<>();
        for (var entry : functionsByFile.entrySet()) {
            for (AstFunctionSpan span : entry.getValue()) {
                out.put(span.qualifiedName(), new SpanLocation(entry.getKey(), span));
            }
        }
        return out;
    }

    private List<SourceFileResponse> toFileResponses(Map<String, String> filesByName) {
        List<SourceFileResponse> out = new ArrayList<>(filesByName.size());
        for (var e : filesByName.entrySet()) {
            out.add(new SourceFileResponse(e.getKey(), e.getValue()));
        }
        return out;
    }

    /**
     * Maps each matched fingerprint's token-index position to one or more real character ranges
     * in the file it came from.
     */
    private List<MatchedRangeResponse> matchedRanges(
        FingerprintResult fp,
        Set<Long> sharedHashes,
        int kgramSize,
        List<NormalizedToken> tokens,
        Map<String, String> filesByName
    ) {
        List<MatchedRangeResponse> ranges = new ArrayList<>();
        
        for (var f : fp.fingerprints()) {
            if (!sharedHashes.contains(f.hash())) {

                continue;
            }
            int startIdx = f.position();
            if (startIdx < 0 || startIdx >= tokens.size()) {

                continue;
            }
            int endIdx = Math.min(startIdx + kgramSize, tokens.size());

            int rangeStartIdx = startIdx;
            NormalizedToken prev = tokens.get(startIdx);

            boolean stoppedAtFileBoundary = false;

            for (int idx = startIdx + 1; idx < endIdx; idx++) {
                NormalizedToken cur = tokens.get(idx);
                if (!cur.fileName().equals(prev.fileName())) {

                    NormalizedToken rangeStart = tokens.get(rangeStartIdx);
                    ranges.add(new MatchedRangeResponse(rangeStart.fileName(), rangeStart.start(), prev.end()));
                    stoppedAtFileBoundary = true;
                    break;


                }
                if (!isBlankGap(filesByName.get(prev.fileName()), prev.end(), cur.start())) {
                    NormalizedToken rangeStart = tokens.get(rangeStartIdx);
                    ranges.add(new MatchedRangeResponse(rangeStart.fileName(), rangeStart.start(), prev.end()));
                    rangeStartIdx = idx;
                }

                prev = cur;
            }
            if (!stoppedAtFileBoundary) {
                NormalizedToken rangeStart = tokens.get(rangeStartIdx);
                ranges.add(new MatchedRangeResponse(rangeStart.fileName(), rangeStart.start(), prev.end()));

            }
        }

        ranges.sort((x, y) -> Integer.compare(x.start(), y.start()));
        return ranges;
    }

    /** True if given file's "start, end" slice is empty or whitespace-only. */
    private boolean isBlankGap(String source, int start, int end) {
        if (source == null || start < 0 || end > source.length() || start > end) {
            return true;

        }
        return source.substring(start, end).isBlank();
    }

    private String truncate(String s, int max) {
        if(s == null) {

            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }


}