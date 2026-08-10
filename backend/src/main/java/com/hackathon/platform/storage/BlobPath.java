package com.hackathon.platform.storage;

/**
 * Centralises storage key construction for all Azure Blob Storage operations. This class ensures
 * consistent naming conventions and prevents path traversal attacks via uploaded filenames.
 */
public final class BlobPath {

  private BlobPath() {}

  /**
   * Storage key for a level input file. Stored in: levelfiles.storage_key
   *
   * @param hackathonId the hackathon UUID
   * @param levelId the level ID
   * @param filename the original filename
   * @return storage key string
   */
  public static String levelFile(String hackathonId, String levelId, String filename) {
    return String.format("hackathons/%s/levels/%s/%s", hackathonId, levelId, sanitise(filename));
  }

  /**
   * Storage key for an event's banner image. Stored in: events.banner_storage_key
   *
   * @param eventId the event UUID
   * @param filename the original filename
   * @return storage key string
   */
  public static String eventBanner(String eventId, String filename) {
    return String.format("events/%s/branding/banner/%s", eventId, sanitise(filename));
  }

  /**
   * Storage key for an event's logo image. Stored in: events.logo_storage_key
   *
   * @param eventId the event UUID
   * @param filename the original filename
   * @return storage key string
   */
  public static String eventLogo(String eventId, String filename) {
    return String.format("events/%s/branding/logo/%s", eventId, sanitise(filename));
  }

  /**
   * Storage key for a solver version file. Stored in: solverversion.storage_key
   *
   * @param hackathonId the hackathon UUID
   * @param version the solver version number
   * @param filename the original filename
   * @return storage key string
   */
  public static String solverFile(String hackathonId, int version, String filename) {
    return String.format("hackathons/%s/solver/v%d/%s", hackathonId, version, sanitise(filename));
  }

  /**
   * Storage key for an hackathon problem statement.
   *
   * @param hackathonId the hackathon UUID
   * @param filename the original filename
   * @return storage key string
   */
  public static String problemStatement(String hackathonId, String filename) {
    return String.format("hackathons/%s/problem/%s", hackathonId, sanitise(filename));
  }

  /**
   * Storage key for a submission output file (the artifact graded by the solver). Stored in:
   * submissions.output_storage_key
   *
   * @param eventId the event UUID
   * @param teamId the team UUID
   * @param submissionId the submission ID
   * @param filename the original filename
   * @return storage key string
   */
  public static String submissionOutput(
      String eventId, String teamId, String levelId, String submissionId, String filename) {
    return String.format(
        "submissions/%s/%s/levels/%s/%s/output/%s",
        eventId, teamId, levelId, submissionId, sanitise(filename));
  }

  /**
   * Storage key for a submission source code ZIP archive. Stored in:
   * submissions.source_code_storage_key
   *
   * @param eventId the event UUID
   * @param teamId the team UUID
   * @param submissionId the submission ID
   * @param filename the original filename
   * @return storage key string
   */
  public static String submissionSourceArchive(
      String eventId, String teamId, String levelId, String submissionId, String filename) {
    return String.format(
        "submissions/%s/%s/levels/%s/%s/source/%s",
        eventId, teamId, levelId, submissionId, sanitise(filename));
  }

  /**
   * Storage key for a scoring log file. One file per submission.
   *
   * @param eventId the event UUID
   * @param teamId the team ID
   * @param levelId the level ID
   * @param submissionId the submission ID
   * @return storage key string
   */
  public static String scoringLog(
      String eventId, String teamId, String levelId, String submissionId) {
    return String.format(
        "logs/%s/%s/%s/%s/scoring_log.txt", eventId, teamId, levelId, submissionId);
  }

  /**
   * Strips path traversal characters from a filename to prevent directory traversal attacks.
   *
   * @param filename the raw filename from the upload
   * @return sanitised filename safe for use as a storage key segment
   */
  private static String sanitise(String filename) {
    return filename.replaceAll("[/\\\\]", "_").replaceAll("\\.\\.", "_").trim();
  }
}