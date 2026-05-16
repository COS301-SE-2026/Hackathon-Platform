package com.hackathon.platform.controller;
 
import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.service.StorageService;
import com.hackathon.platform.storage.BlobPath;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
 
/**
 * REST controller for all file upload and presigned download URL operations.
 * Controllers only handle HTTP — all logic is delegated to {@link StorageService}.
 * Storage keys returned match the column names in the database schema:
 * storage_key, output_storage_key, source_code_storage_key.
 */
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {
 
  private final StorageService storageService;
  private final AzureBlobConfig config;

  //Event Resources (Admin only)

  /**
   * Uploads a level input file for a specific event and level.
   * The returned storageKey maps to levelfiles.storage_key in the database.
   *
   * @param eventId the event UUID
   * @param levelId the level ID
   * @param file    the uploaded file
   * @return storageKey and blobUrl
   */
  @PostMapping("/events/{eventId}/levels/{levelId}/files")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> uploadLevelFile(
      @PathVariable String eventId,
      @PathVariable String levelId,
      @RequestParam("file") MultipartFile file) {
    String storageKey = BlobPath.levelFile(eventId, levelId, file.getOriginalFilename());
    String blobUrl = storageService.upload(config.getEventResourcesContainer(), storageKey, file);
    return ResponseEntity.ok(Map.of("storageKey", storageKey, "blobUrl", blobUrl));
  }

  /**
   * Returns a presigned SAS URL for downloading a level file.
   *
   * @param eventId  the event UUID
   * @param levelId  the level ID
   * @param filename the blob filename
   * @return presigned download URL
   */
  @GetMapping("/events/{eventId}/levels/{levelId}/files/{filename}")
  @PreAuthorize("hasAnyRole('ADMIN', 'PARTICIPANT')")
  public ResponseEntity<Map<String, String>> getLevelFileUrl(
      @PathVariable String eventId,
      @PathVariable String levelId,
      @PathVariable String filename) {

    String storageKey = BlobPath.levelFile(eventId, levelId, filename);
    String url = storageService.generatePresignedUrl(
        config.getEventResourcesContainer(), storageKey, config.getSasExpiryMinutes());
    return ResponseEntity.ok(Map.of("url", url));

  }

  /**
   * Uploads a solver file for a specific event and version.
   * The returned storageKey maps to solverversion.storage_key in the database.
   *
   * @param eventId the event UUID
   * @param version the solver version number
   * @param file    the uploaded solver file
   * @return storageKey, blobUrl, and version
   */
  @PostMapping("/events/{eventId}/solver")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> uploadSolver(
      @PathVariable String eventId,
      @RequestParam("version") int version,
      @RequestParam("file") MultipartFile file) {
    String storageKey = BlobPath.solverFile(eventId, version, file.getOriginalFilename());
    String blobUrl = storageService.upload(config.getEventResourcesContainer(), storageKey, file);
    return ResponseEntity.ok(Map.of(
        "storageKey", storageKey,
        "blobUrl", blobUrl,
        "version", String.valueOf(version)));
  }



}