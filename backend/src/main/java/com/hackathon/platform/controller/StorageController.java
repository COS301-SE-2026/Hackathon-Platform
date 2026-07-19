package com.hackathon.platform.controller;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.model.SolverVersion;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.service.FileMetadataService;
import com.hackathon.platform.service.HackathonService;
import com.hackathon.platform.service.StorageService;
import com.hackathon.platform.storage.BlobPath;
import com.hackathon.platform.storage.StorageException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.hackathon.platform.scoring.queue.ScoringJobProducer;


/**
 * REST controller for all file upload and presigned download URL operations. All logic is delegated
 * to {@link StorageService}. Storage keys returned match the column names in the database schema.
 * storage_key, output_storage_key, source_code_storage_key.
 */
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

  private final StorageService storageService;
  private final AzureBlobConfig config;
  private final FileMetadataService fileMetadataService;
  private final SolverVersionRepository solverVersionRepository;
  private final ScoringJobProducer producer;
  private final HackathonService hackathonService;


  // Event Resources

  /**
   * Uploads a level input file for a specific event and level. The returned storageKey maps to
   * levelfiles.storage_key in the database.
   *
   * @param hackathonId the event UUID
   * @param levelId the level ID
   * @param file the uploaded file
   * @param fileType the file type (ZIP, TAR, PDF, TXT, CSV, JSON, IMAGE, OTHER)
   * @return storageKey, blobUrl, and database record id
   */
  @PostMapping("/hackathons/{hackathonId}/levels/{levelId}/files")
  // @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> uploadLevelFile(
      @PathVariable String hackathonId,
      @PathVariable Long levelId,
      @RequestParam("file") MultipartFile file,
      @RequestParam("fileType") String fileType) {

    String storageKey =
        BlobPath.levelFile(hackathonId, String.valueOf(levelId), file.getOriginalFilename());
    String blobUrl = storageService.upload(config.getEventResourcesContainer(), storageKey, file);

    LevelFile saved =
        fileMetadataService.saveLevelFile(
            levelId,
            file.getOriginalFilename(),
            storageKey,
            fileType.toUpperCase(),
            file.getSize(),
            file.getContentType());

    return ResponseEntity.ok(
        Map.of(
            "id", String.valueOf(saved.getId()),
            "storageKey", storageKey,
            "blobUrl", blobUrl));
  }

  /**
   * Returns a presigned SAS URL for downloading a level file.
   *
   * @param hackathonId the event UUID
   * @param levelId the level ID
   * @param filename the blob filename
   * @return presigned download URL
   */
  @GetMapping("/hackathons/{hackathonId}/levels/{levelId}/files/{filename}")
  // @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<Map<String, String>> getLevelFileUrl(
      @PathVariable String hackathonId,
      @PathVariable String levelId,
      @PathVariable String filename) {

    String storageKey = BlobPath.levelFile(hackathonId, levelId, filename);
    String url =
        storageService.generatePresignedUrl(
            config.getEventResourcesContainer(), storageKey, config.getSasExpiryMinutes());
    return ResponseEntity.ok(Map.of("url", url));
  }

  /**
   * Lists all files uploaded for a specific level (input files, resource bundles, supplementary
   * documents, etc). Used by the admin Levels page to render each level's file list.
   *
   * @param hackathonId the hackathon UUID
   * @param levelId the level ID
   * @return the level's file metadata records
   */
  @GetMapping("/hackathons/{hackathonId}/levels/{levelId}/files")
  // @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<List<LevelFile>> listLevelFiles(
      @PathVariable String hackathonId, @PathVariable Long levelId) {
    return ResponseEntity.ok(fileMetadataService.listLevelFiles(levelId));
  }

  /**
   * Deletes a level file, removing both the blob and its metadata record.
   *
   * @param hackathonId the hackathon UUID
   * @param levelId the level ID
   * @param fileId the level file's database id
   */
  @DeleteMapping("/hackathons/{hackathonId}/levels/{levelId}/files/{fileId}")
  // @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteLevelFile(
      @PathVariable String hackathonId, @PathVariable Long levelId, @PathVariable Long fileId) {
    LevelFile file = fileMetadataService.getLevelFile(fileId);
    storageService.delete(config.getEventResourcesContainer(), file.getStorageKey());
    fileMetadataService.deleteLevelFile(fileId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Uploads a solver file for a specific event and version. The returned storageKey maps to
   * solverversion.storage_key in the database. Automatically deactivates all previous solver
   * versions for this event before saving the new active one.
   *
   * @param hackathonId the event UUID
   * @param version the solver version number
   * @param file the uploaded solver file
   * @param uploadedBy UUID of the admin uploading the solver
   * @param notes optional release notes for this solver version
   * @return storageKey, blobUrl, version, and database record id
   */
  @PostMapping("/hackathons/{hackathonId}/solver")
  // @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> uploadSolver(
      @PathVariable String hackathonId,
      @RequestParam("version") int version,
      @RequestParam("file") MultipartFile file,
      @RequestParam("uploadedBy") UUID uploadedBy,
      @RequestParam(value = "notes", required = false) String notes) {

    String storageKey = BlobPath.solverFile(hackathonId, version, file.getOriginalFilename());
    String blobUrl = storageService.upload(config.getEventResourcesContainer(), storageKey, file);

    // Deactivate all previous solver versions for this event
    solverVersionRepository
        .findByHackathonId(UUID.fromString(hackathonId))
        .forEach(
            sv -> {
              sv.setIsActive(false);
              solverVersionRepository.save(sv);
            });

    SolverVersion saved =
        fileMetadataService.saveSolverVersion(
            UUID.fromString(hackathonId),
            uploadedBy,
            storageKey,
            version,
            file.getOriginalFilename(),
            file.getSize());

    // Set notes separately since saveSolverVersion doesn't take it
    if (notes != null) {
      saved.setNotes(notes);
      solverVersionRepository.save(saved);
    }

    return ResponseEntity.ok(
        Map.of(
            "solverVersionId",
            String.valueOf(saved.getId()),
            "storageKey",
            storageKey,
            "blobUrl",
            blobUrl,
            "version",
            String.valueOf(version)));
  }

  /**
   * Uploads a branding asset (logo, banner) for a specific event.
   *
   * @param hackathonId the event UUID
   * @param file the uploaded image file
   * @return storageKey and blobUrl
   */
  @PostMapping("/hackathons/{hackathonId}/branding")
  // @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> uploadBrandingAsset(
      @PathVariable String hackathonId, @RequestParam("file") MultipartFile file) {
    String storageKey = BlobPath.brandingAsset(hackathonId, file.getOriginalFilename());
    String blobUrl = storageService.upload(config.getEventResourcesContainer(), storageKey, file);
    return ResponseEntity.ok(Map.of("storageKey", storageKey, "blobUrl", blobUrl));
  }

  /**
   * Uploads the problem statement PDF for a hackathon. The returned storageKey is saved to
   * hackathon.problem_statement_storage_key, replacing any previous problem statement for this
   * hackathon.
   *
   * @param hackathonId the hackathon UUID
   * @param file the uploaded PDF file
   * @return storageKey and blobUrl
   */
  @PostMapping("/hackathons/{hackathonId}/problem-statement")
  // @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> uploadProblemStatement(
      @PathVariable String hackathonId, @RequestParam("file") MultipartFile file) {

    if (file.isEmpty()) {
      throw new StorageException("No file provided");
    }
    if (!"application/pdf".equals(file.getContentType())) {
      throw new StorageException("Problem statement must be a PDF file");
    }

    String storageKey = BlobPath.problemStatement(hackathonId, file.getOriginalFilename());
    String blobUrl = storageService.upload(config.getEventResourcesContainer(), storageKey, file);

    fileMetadataService.updateProblemStatementStorageKey(UUID.fromString(hackathonId), storageKey);

    return ResponseEntity.ok(Map.of("storageKey", storageKey, "blobUrl", blobUrl));
  }

  /**
   * Returns a presigned SAS URL for downloading a hackathon's problem statement PDF.
   *
   * @param hackathonId the hackathon UUID
   * @return presigned download URL and the storage key
   */
  @GetMapping("/hackathons/{hackathonId}/problem-statement")
  // @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<Map<String, String>> getProblemStatementUrl(
      @PathVariable String hackathonId) {
    String storageKey =
        hackathonService.getHackathonById(UUID.fromString(hackathonId)).getProblemStatementStorageKey();

    if (storageKey == null) {
      return ResponseEntity.notFound().build();
    }

    String url =
        storageService.generatePresignedUrl(
            config.getEventResourcesContainer(), storageKey, config.getSasExpiryMinutes());
    return ResponseEntity.ok(Map.of("url", url, "storageKey", storageKey));
  }

  // Submissions (Participant)

  /**
   * Uploads both the submission output file and source code archive in a single request. Creates
   * one submission record with both storage keys set, then uploads both files to their canonical
   * blob paths.
   *
   * @param hackathonId the event UUID
   * @param teamId the team UUID
   * @param outputFile the solution output file
   * @param sourceFile the zipped source code archive
   * @param levelId the level this submission is for
   * @param solverVersionId the active solver version to use for scoring
   * @return submissionId, both storage keys, and status
   */
  @PostMapping("/hackathons/{hackathonId}/teams/{teamId}/submissions")
  // @PreAuthorize("hasRole('PARTICIPANT')")
  public ResponseEntity<Map<String, String>> uploadSubmission(
      @PathVariable String hackathonId,
      @PathVariable String teamId,
      @RequestParam("outputFile") MultipartFile outputFile,
      @RequestParam("sourceFile") MultipartFile sourceFile,
      @RequestParam("levelId") Long levelId,
      @RequestParam("solverVersionId") Long solverVersionId) {

    Submission saved =
        fileMetadataService.saveSubmission(
            hackathonId,
            UUID.fromString(teamId),
            levelId,
            solverVersionId,
            outputFile.getOriginalFilename(),
            outputFile.getSize(),
            outputFile.getContentType(),
            sourceFile.getOriginalFilename(),
            sourceFile.getSize(),
            sourceFile.getContentType());

    storageService.upload(
        config.getSubmissionsContainer(), saved.getOutputStorageKey(), outputFile);
    storageService.upload(
        config.getSubmissionsContainer(), saved.getSourceCodeStorageKey(), sourceFile);

    String record = producer.enqueue(saved.getId());

    return ResponseEntity.ok(
        Map.of(
            "submissionId", String.valueOf(saved.getId()),
            "outputStorageKey", saved.getOutputStorageKey(),
            "sourceStorageKey", saved.getSourceCodeStorageKey(),
            "status", "QUEUED",
                "scoringRecordId", record != null ? record : ""));
  }

  /**
   * Returns a presigned SAS URL for downloading a submission output file.
   *
   * @param hackathonId the event UUID
   * @param teamId the team UUID
   * @param submissionId the submission ID
   * @param filename the blob filename
   * @return presigned download URL
   */
  @GetMapping(
      "/hackathons/{hackathonId}/teams/{teamId}/submissions/{submissionId}/output/{filename}")
  // @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<Map<String, String>> getSubmissionOutputUrl(
      @PathVariable String hackathonId,
      @PathVariable String teamId,
      @PathVariable Long submissionId,
      @PathVariable String filename) {

    String storageKey = fileMetadataService.getSubmissionOutputStorageKey(submissionId);
    String url =
        storageService.generatePresignedUrl(
            config.getSubmissionsContainer(), storageKey, config.getSasExpiryMinutes());
    return ResponseEntity.ok(Map.of("url", url));
  }

  /**
   * Returns a presigned SAS URL for downloading a source code archive (Admin only for auditing).
   *
   * @param hackathonId the event UUID
   * @param teamId the team UUID
   * @param submissionId the submission ID
   * @param filename the blob filename
   * @return presigned download URL
   */
  @GetMapping(
      "/hackathons/{hackathonId}/teams/{teamId}/submissions/{submissionId}/source/{filename}")
  // @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> getSourceArchiveUrl(
      @PathVariable String hackathonId,
      @PathVariable String teamId,
      @PathVariable Long submissionId,
      @PathVariable String filename) {

    String storageKey = fileMetadataService.getSubmissionSourceStorageKey(submissionId);
    String url =
        storageService.generatePresignedUrl(
            config.getSubmissionsContainer(), storageKey, config.getSasExpiryMinutes());
    return ResponseEntity.ok(Map.of("url", url));
  }

  // Scoring Logs

  /**
   * Returns a presigned SAS URL for downloading a scoring log.
   *
   * @param hackathonId the event UUID
   * @param teamId the team ID
   * @param levelId the level ID
   * @return presigned download URL
   */
  @GetMapping("/hackathons/{hackathonId}/teams/{teamId}/levels/{levelId}")
  // @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<Map<String, String>> getScoringLogUrl(
      @PathVariable String hackathonId, @PathVariable String teamId, @PathVariable String levelId) {
    String storageKey = BlobPath.scoringLog(hackathonId, teamId, levelId);
    String url =
        storageService.generatePresignedUrl(
            config.getScoringLogsContainer(), storageKey, config.getSasExpiryMinutes());
    return ResponseEntity.ok(Map.of("url", url));
  }
}
