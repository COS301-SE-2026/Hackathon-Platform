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
}