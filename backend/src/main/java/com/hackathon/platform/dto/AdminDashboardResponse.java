package com.hackathon.platform.dto;

/** Coordinator overview across all events */
public class AdminDashboardResponse {

  private long activeEvents;
  private long totalEvents;
  private long totalParticipants;
  private long submissionsToday;
  private long totalSubmissions;

  public AdminDashboardResponse() {}

  public AdminDashboardResponse(
      long activeEvents, long totalEvents, long totalParticipants, long submissionsToday, long totalSubmissions) {
    this.activeEvents = activeEvents;
    this.totalEvents = totalEvents;
    this.totalParticipants = totalParticipants;
    this.submissionsToday = submissionsToday;
    this.totalSubmissions = totalSubmissions;
  }

  public long getActiveEvents() {
    return activeEvents;
  }

  public long getTotalEvents() {
    return totalEvents;
  }

  public long getTotalParticipants() {
    return totalParticipants;
  }

  public long getSubmissionsToday() {
    return submissionsToday;
  }

  public long getTotalSubmissions() {
    return totalSubmissions;
  }
}