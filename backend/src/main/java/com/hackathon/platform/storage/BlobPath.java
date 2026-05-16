package com.hackathon.platform.storage;

/**
 * Centralises storage key construction for all Azure Blob Storage operations.
 * This class ensures consistent naming conventions and prevents
 * path traversal attacks via uploaded filenames.
 */
public final class BlobPath {

  private BlobPath() {}

  /**
   * Storage key for a level input file.
   * Stored in: levelfiles.storage_key
   *
   * @param eventId  the event UUID
   * @param levelId  the level ID
   * @param filename the original filename
   * @return storage key string
   */
  public static String levelFile(String eventId, String levelId, String filename) {
    return String.format("events/%s/levels/%s/%s", eventId, levelId, sanitise(filename));
  }

  /**
   * Storage key for an event branding asset (logo, banner).
   * Stored in: events.branding_storage_key (might be added later).
   *
   * @param eventId  the event UUID
   * @param filename the original filename
   * @return storage key string
   */
  public static String brandingAsset(String eventId, String filename) {
    return String.format("events/%s/branding/%s", eventId, sanitise(filename));
  }

  /**
   * Storage key for a solver version file.
   * Stored in: solverversion.storage_key
   *
   * @param eventId  the event UUID
   * @param version  the solver version number
   * @param filename the original filename
   * @return storage key string
   */
  public static String solverFile(String eventId, int version, String filename) {
    return String.format("events/%s/solver/v%d/%s", eventId, version, sanitise(filename));
  }

  /**
   * Storage key for an event problem statement.
   *
   * @param eventId  the event UUID
   * @param filename the original filename
   * @return storage key string
   */
  public static String problemStatement(String eventId, String filename) {
    return String.format("events/%s/problem/%s", eventId, sanitise(filename));
  }

  /**
   * Storage key for a submission output file (the artifact graded by the solver).
   * Stored in: submissions.output_storage_key
   *
   * @param eventId      the event UUID
   * @param teamId       the team UUID
   * @param submissionId the submission ID
   * @param filename     the original filename
   * @return storage key string
   */
  public static String submissionOutput(
      String eventId, String teamId, String submissionId, String filename) {
    return String.format(
        "submissions/%s/%s/%s/output/%s", eventId, teamId, submissionId, sanitise(filename));
  }

  /**
   * Storage key for a submission source code ZIP archive.
   * Stored in: submissions.source_code_storage_key
   *
   * @param eventId      the event UUID
   * @param teamId       the team UUID
   * @param submissionId the submission ID
   * @param filename     the original filename
   * @return storage key string
   */
  public static String submissionSourceArchive(
      String eventId, String teamId, String submissionId, String filename) {
    return String.format(
        "submissions/%s/%s/%s/source/%s", eventId, teamId, submissionId, sanitise(filename));
  }

  /**
   * Storage key for a scoring log file.
   *
   * @param eventId      the event UUID
   * @param submissionId the submission ID
   * @param filename     the log filename
   * @return storage key string
   */
  public static String scoringLog(String eventId, String submissionId, String filename) {
    return String.format("logs/%s/%s/%s", eventId, submissionId, sanitise(filename));
  }

  /**
   * Strips path traversal characters from a filename to prevent directory traversal attacks.
   *
   * @param filename the raw filename from the upload
   * @return sanitised filename safe for use as a storage key segment
   */
  private static String sanitise(String filename) {
    return filename
        .replaceAll("[/\\\\]", "_")
        .replaceAll("\\.\\.", "_")
        .trim();
  }

  
}