package com.hackathon.platform.dto;

/** Coordinator overview across all events */
public class AdminDashboardResponse {

  private long activeEvents;
  private long totalEvents;
  private long totalParticipants;
  private long submissionsToday;

  public AdminDashboardResponse() {}

  public AdminDashboardResponse(
      long activeEvents, long totalEvents, long totalParticipants, long submissionsToday) {
    this.activeEvents = activeEvents;
    this.totalEvents = totalEvents;
    this.totalParticipants = totalParticipants;
    this.submissionsToday = submissionsToday;
  }

  public long getActiveEvents() {
    return activeEvents;
  }

  public void setActiveEvents(long activeEvents) {
    this.activeEvents = activeEvents;
  }

  public long getTotalEvents() {
    return totalEvents;
  }

  public void setTotalEvents(long totalEvents) {
    this.totalEvents = totalEvents;
  }

  public long getTotalParticipants() {
    return totalParticipants;
  }

  public void setTotalParticipants(long totalParticipants) {
    this.totalParticipants = totalParticipants;
  }

  public long getSubmissionsToday() {
    return submissionsToday;
  }

  public void setSubmissionsToday(long submissionsToday) {
    this.submissionsToday = submissionsToday;
  }
}
