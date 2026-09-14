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
    private final CodeNormalizer normalizer;
    private final Winnowing winnowing;
    private final PlagiarismProperties props;

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
            List<String> tokens = fetchAndNormalize(sub);
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
        }

        similarityRepo.deleteByEventIdAndLevelId(eventId, levelId);

        int compared = 0;
        int flagged = 0;
        List<SubmissionSimilarity> toSave = new ArrayList<>();

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
                Long highter = Math.max(a.getId(), b.getId());
                UUID teamLower = a.getId().equals(lower) ? a.getTeamId() : b.getTeamId();
                UUID teamHighter = a.getId().equals(lower) ? b.getTeamId() : a.getTeamId();

                BigDecimal score = BigDecimal.valueOf(structural).setScale(4, RoundingMode.HALF_UP);
                toSave.add(
                    new SubmissionSimilarity(
                        eventId,
                        levelId,
                        lower,
                        higher,
                        teamLower,
                        teamHighter,
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

    
}