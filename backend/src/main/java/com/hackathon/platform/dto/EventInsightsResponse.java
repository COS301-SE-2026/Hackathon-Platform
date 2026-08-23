package com.hackathon.platform.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Coordinator dashboard for single event. */
public class EventInsightsResponse {

  private UUID eventId;

  private long activeTeams;
  private long approvedParticipants;

  private long totalSubmissions;
  private long submissionsLastHour;

  /** Count of submissions grouped by status */
  private Map<String, Long> submissionsByStatus;

  private Double errorRate;
  private List<SubmissionRateBucket> submissionRate;

  private List<LevelScoreStats> scoreDistributionByLevel;

  public EventInsightsResponse() {}

  public EventInsightsResponse(
      UUID eventId,
      long activeTeams,
      long approvedParticipants,
      long totalSubmissions,
      long submissionsLastHour,
      Map<String, Long> submissionsByStatus,
      Double errorRate,
      List<SubmissionRateBucket> submissionRate,
      List<LevelScoreStats> scoreDistributionByLevel) {
    this.eventId = eventId;
    this.activeTeams = activeTeams;
    this.approvedParticipants = approvedParticipants;
    this.totalSubmissions = totalSubmissions;
    this.submissionsLastHour = submissionsLastHour;
    this.submissionsByStatus = submissionsByStatus;
    this.errorRate = errorRate;
    this.submissionRate = submissionRate;
    this.scoreDistributionByLevel = scoreDistributionByLevel;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public long getActiveTeams() {
    return activeTeams;
  }

  public void setActiveTeams(long activeTeams) {
    this.activeTeams = activeTeams;
  }

  public long getApprovedParticipants() {
    return approvedParticipants;
  }

  public void setApprovedParticipants(long approvedParticipants) {
    this.approvedParticipants = approvedParticipants;
  }

  public long getTotalSubmissions() {
    return totalSubmissions;
  }

  public void setTotalSubmissions(long totalSubmissions) {
    this.totalSubmissions = totalSubmissions;
  }

  public long getSubmissionsLastHour() {
    return submissionsLastHour;
  }

  public void setSubmissionsLastHour(long submissionsLastHour) {
    this.submissionsLastHour = submissionsLastHour;
  }

  public Map<String, Long> getSubmissionsByStatus() {
    return submissionsByStatus;
  }

  public void setSubmissionsByStatus(Map<String, Long> submissionsByStatus) {
    this.submissionsByStatus = submissionsByStatus;
  }

  public Double getErrorRate() {
    return errorRate;
  }

  public void setErrorRate(Double errorRate) {
    this.errorRate = errorRate;
  }

  public List<SubmissionRateBucket> getSubmissionRate() {
    return submissionRate;
  }

  public void setSubmissionRate(List<SubmissionRateBucket> submissionRate) {
    this.submissionRate = submissionRate;
  }

  public List<LevelScoreStats> getScoreDistributionByLevel() {
    return scoreDistributionByLevel;
  }

  public void setScoreDistributionByLevel(List<LevelScoreStats> scoreDistributionByLevel) {
    this.scoreDistributionByLevel = scoreDistributionByLevel;
  }
}
