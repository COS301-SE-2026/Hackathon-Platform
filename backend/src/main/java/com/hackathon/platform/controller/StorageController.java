package com.hackathon.platform.controller;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.model.LevelFile;
import com.hackathon.platform.model.SolverVersion;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.service.FileMetadataService;
import com.hackathon.platform.service.StorageService;
import com.hackathon.platform.storage.BlobPath;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

  // Event Resources

  /**
   * Uploads a level input file for a specific event and level. The returned storageKey maps to
   * levelfiles.storage_key in the database.
   *
   * @param eventId the event UUID
   * @param levelId the level ID
   * @param file the uploaded file
   * @param fileType the file type (ZIP, TAR, PDF, TXT, CSV, JSON, IMAGE, OTHER)
   * @return storageKey, blobUrl, and database record id
   */
  @PostMapping("/events/{eventId}/levels/{levelId}/files")
  // @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> uploadLevelFile(
      @PathVariable String eventId,
      @PathVariable Long levelId,
      @RequestParam("file") MultipartFile file,
      @RequestParam("fileType") String fileType) {

    String storageKey = BlobPath.levelFile(eventId, String.valueOf(levelId), file.getOriginalFilename());
    String blobUrl = storageService.upload(config.getEventResourcesContainer(), storageKey, file);

    LevelFile saved = fileMetadataService.saveLevelFile(
        levelId,
        file.getOriginalFilename(),
        storageKey,
        fileType.toUpperCase(),
        file.getSize(),
        file.getContentType());

    return ResponseEntity.ok(Map.of(
        "id", String.valueOf(saved.getId()),
        "storageKey", storageKey,
        "blobUrl", blobUrl));
  }


  /**
   * Returns a presigned SAS URL for downloading a level file.
   *
   * @param eventId the event UUID
   * @param levelId the level ID
   * @param filename the blob filename
   * @return presigned download URL
   */
  @GetMapping("/events/{eventId}/levels/{levelId}/files/{filename}")
  // @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<Map<String, String>> getLevelFileUrl(
      @PathVariable String eventId, @PathVariable String levelId, @PathVariable String filename) {

    String storageKey = BlobPath.levelFile(eventId, levelId, filename);
    String url =
        storageService.generatePresignedUrl(
            config.getEventResourcesContainer(), storageKey, config.getSasExpiryMinutes());
    return ResponseEntity.ok(Map.of("url", url));
  }

  /**
   * Uploads a solver file for a specific event and version. The returned storageKey maps to
   * solverversion.storage_key in the database. Automatically deactivates all previous solver
   * versions for this event before saving the new active one.
   *
   * @param eventId the event UUID
   * @param version the solver version number
   * @param file the uploaded solver file
   * @param uploadedBy UUID of the admin uploading the solver
   * @param notes optional release notes for this solver version
   * @return storageKey, blobUrl, version, and database record id
   */
  @PostMapping("/events/{eventId}/solver")
  // @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> uploadSolver(
      @PathVariable String eventId,
      @RequestParam("version") int version,
      @RequestParam("file") MultipartFile file,
      @RequestParam("uploadedBy") UUID uploadedBy,
      @RequestParam(value = "notes", required = false) String notes) {

    String storageKey = BlobPath.solverFile(eventId, version, file.getOriginalFilename());
    String blobUrl = storageService.upload(config.getEventResourcesContainer(), storageKey, file);

    // Deactivate all previous solver versions for this event
    solverVersionRepository.findByEventId(UUID.fromString(eventId))
        .forEach(sv -> { sv.setIsActive(false); solverVersionRepository.save(sv); });

    SolverVersion saved = fileMetadataService.saveSolverVersion(
        UUID.fromString(eventId),
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

    return ResponseEntity.ok(Map.of(
        "solverVersionId", String.valueOf(saved.getId()),
        "storageKey", storageKey,
        "blobUrl", blobUrl,
        "version", String.valueOf(version)));
  }

  /**
   * Uploads a branding asset (logo, banner) for a specific event.
   *
   * @param eventId the event UUID
   * @param file the uploaded image file
   * @return storageKey and blobUrl
   */
  @PostMapping("/events/{eventId}/branding")
  // @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> uploadBrandingAsset(
      @PathVariable String eventId, @RequestParam("file") MultipartFile file) {
    String storageKey = BlobPath.brandingAsset(eventId, file.getOriginalFilename());
    String blobUrl = storageService.upload(config.getEventResourcesContainer(), storageKey, file);
    return ResponseEntity.ok(Map.of("storageKey", storageKey, "blobUrl", blobUrl));
  }

  // Submissions (Participant)

  /**
   * Uploads a submission output file for a specific team and level. The returned storageKey maps to
   * submissions.output_storage_key in the database. Creates a new submission record with QUEUED
   * status.
   *
   * @param eventId the event UUID
   * @param teamId the team UUID
   * @param submissionId the submission ID (used for blob path only)
   * @param file the solution output file
   * @param levelId the level this submission is for
   * @param solverVersionId the active solver version to use for scoring
   * @return storageKey, blobUrl, and database submission id
   */
  @PostMapping("/events/{eventId}/teams/{teamId}/submissions/{submissionId}/output")
  // @PreAuthorize("hasRole('PARTICIPANT')")
  public ResponseEntity<Map<String, String>> uploadSubmissionOutput(
      @PathVariable String eventId,
      @PathVariable String teamId,
      @PathVariable String submissionId,
      @RequestParam("file") MultipartFile file,
      @RequestParam("levelId") Long levelId,
      @RequestParam("solverVersionId") Long solverVersionId) {

    String storageKey =
        BlobPath.submissionOutput(eventId, teamId, submissionId, file.getOriginalFilename());
    String blobUrl = storageService.upload(config.getSubmissionsContainer(), storageKey, file);

    Submission saved = fileMetadataService.saveSubmissionOutput(
        UUID.fromString(teamId),
        levelId,
        solverVersionId,
        storageKey,
        file.getOriginalFilename(),
        file.getSize(),
        file.getContentType());

    return ResponseEntity.ok(Map.of(
        "submissionId", String.valueOf(saved.getId()),
        "storageKey", storageKey,
        "blobUrl", blobUrl,
        "status", saved.getStatus()));
  }

  /**
   * Uploads a source code ZIP archive alongside a submission. The returned storageKey maps to
   * submissions.source_code_storage_key in the database.
   *
   * @param eventId the event UUID
   * @param teamId the team UUID
   * @param submissionId the submission ID
   * @param file the zipped source code archive
   * @return storageKey and blobUrl
   */
  @PostMapping("/events/{eventId}/teams/{teamId}/submissions/{submissionId}/source")
  // @PreAuthorize("hasRole('PARTICIPANT')")
  public ResponseEntity<Map<String, String>> uploadSourceArchive(
      @PathVariable String eventId,
      @PathVariable String teamId,
      @PathVariable String submissionId,
      @RequestParam("file") MultipartFile file) {
    String storageKey =
        BlobPath.submissionSourceArchive(eventId, teamId, submissionId, file.getOriginalFilename());
    String blobUrl = storageService.upload(config.getSubmissionsContainer(), storageKey, file);
    return ResponseEntity.ok(Map.of("storageKey", storageKey, "blobUrl", blobUrl));
  }

  /**
   * Returns a presigned SAS URL for downloading a submission output file.
   *
   * @param eventId the event UUID
   * @param teamId the team UUID
   * @param submissionId the submission ID
   * @param filename the blob filename
   * @return presigned download URL
   */
  @GetMapping("/events/{eventId}/teams/{teamId}/submissions/{submissionId}/output/{filename}")
  // @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<Map<String, String>> getSubmissionOutputUrl(
      @PathVariable String eventId,
      @PathVariable String teamId,
      @PathVariable String submissionId,
      @PathVariable String filename) {
    String storageKey = BlobPath.submissionOutput(eventId, teamId, submissionId, filename);
    String url =
        storageService.generatePresignedUrl(
            config.getSubmissionsContainer(), storageKey, config.getSasExpiryMinutes());
    return ResponseEntity.ok(Map.of("url", url));
  }

  /**
   * Returns a presigned SAS URL for downloading a source code archive (Admin only for auditing).
   *
   * @param eventId the event UUID
   * @param teamId the team UUID
   * @param submissionId the submission ID
   * @param filename the blob filename
   * @return presigned download URL
   */
  @GetMapping("/events/{eventId}/teams/{teamId}/submissions/{submissionId}/source/{filename}")
  // @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> getSourceArchiveUrl(
      @PathVariable String eventId,
      @PathVariable String teamId,
      @PathVariable String submissionId,
      @PathVariable String filename) {
    String storageKey = BlobPath.submissionSourceArchive(eventId, teamId, submissionId, filename);
    String url =
        storageService.generatePresignedUrl(
            config.getSubmissionsContainer(), storageKey, config.getSasExpiryMinutes());
    return ResponseEntity.ok(Map.of("url", url));
  }

  // Scoring Logs(might remove)

  /**
   * Returns a presigned SAS URL for downloading a scoring log. Note: scoringlogs table stores
   * log_text directly in the DB unlike the other endpoints for now. This endpoint is for any
   * supplementary log files stored in blob storage.
   *
   * @param eventId the event UUID
   * @param submissionId the submission ID
   * @param filename the log filename
   * @return presigned download URL
   */
  @GetMapping("/events/{eventId}/submissions/{submissionId}/logs/{filename}")
  // @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<Map<String, String>> getScoringLogUrl(
      @PathVariable String eventId,
      @PathVariable String submissionId,
      @PathVariable String filename) {
    String storageKey = BlobPath.scoringLog(eventId, submissionId, filename);
    String url =
        storageService.generatePresignedUrl(
            config.getScoringLogsContainer(), storageKey, config.getSasExpiryMinutes());
    return ResponseEntity.ok(Map.of("url", url));
  }
}
