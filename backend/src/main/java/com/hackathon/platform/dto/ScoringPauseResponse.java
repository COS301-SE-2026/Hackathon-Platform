package com.hackathon.platform.dto;

import java.util.UUID;

public class ScoringPauseResponse {
    private UUID eventId;
    private boolean scoringPaused;

    public ScoringPauseResponse() {}

     public ScoringPauseResponse(UUID eventId, boolean scoringPaused) {
        this.eventId = eventId;
        this.scoringPaused = scoringPaused;
     }

     public UUID getEventId() {
        return eventId;
     }

     public void setEventId(UUID eventId) {
        this.eventId = eventId;
     }

     public boolean isScoringPaused() {
        return scoringPaused;
     }

     public void setScoringPaused(boolean scoringPaused) {
        this.scoringPaused = scoringPaused;
     }
}