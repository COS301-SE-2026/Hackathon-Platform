package com.hackathon.platform.scoring;

import com.hackathon.platform.dto.LeaderboardEntryResponse;
import com.hackathon.platform.repository.LeaderboardEntry;
import com.hackathon.platform.repository.SubmissionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hackathon.platform.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.hackathon.platform.model.Event;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class LeaderboardService {
  private final SubmissionRepository subRepo;
  private final EventRepository eventRepo;

  @Autowired
  public LeaderboardService(SubmissionRepository subRepo){
    this.subRepo = subRepo;
    this.eventRepo = null;
  }

  @Transactional(readOnly = true)
  public List<LeaderboardEntryResponse> getLeaderboard(UUID eventId, short levelId) {
    List<LeaderboardEntry> entries;
    if(eventRepo == null){
      entries = subRepo.findLeaderboardByEventId(eventId);
    } else {
      Event event = eventRepo.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Event not found"));
      entries = entriesForLevel(event, eventId, levelId);
    }
    List<LeaderboardEntryResponse> leaderboard = new ArrayList<>(entries.size());

    for (int i = 0; i < entries.size(); i++) {
      LeaderboardEntry entry = entries.get(i);
      leaderboard.add(
          new LeaderboardEntryResponse(
              i + 1,
              entry.getTeamId(),
              entry.getTeamName(),
              entry.getBestScore(),
              entry.getLastScoredAt()));
    }

    return leaderboard;
  }

  @Transactional(readOnly = true)
  public List<LeaderboardEntryResponse> getEventLeaderboard(UUID eventId) {
    List<LeaderboardEntry> entries;
    if(eventRepo == null){
      entries = subRepo.findLeaderboardByEventId(eventId);
    } else {
      Event event = eventRepo.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Event not found"));
      entries = entriesForEvent(event, eventId);
    }
    List<LeaderboardEntryResponse> leaderboard = new ArrayList<>(entries.size());

    for (int i = 0; i < entries.size(); i++) {
      LeaderboardEntry entry = entries.get(i);
      leaderboard.add(
          new LeaderboardEntryResponse(
              i + 1,
              entry.getTeamId(),
              entry.getTeamName(),
              entry.getBestScore(),
              entry.getLastScoredAt()));
    }

    return leaderboard;
  }

  private List<LeaderboardEntry> entriesForLevel(Event event, UUID eventId, short levelId){
    OffsetDateTime freeze = event.getLeaderboardFreezeDuration();
    if(freeze != null && !OffsetDateTime.now().isBefore(freeze)){
      return subRepo.findFrozenLeaderboardByEventIdAndLevelId(eventId, levelId, freeze);
    }
    return subRepo.findLeaderboardByEventIdAndLevelId(eventId, levelId);
  }

  private List<LeaderboardEntry> entriesForEvent(Event event, UUID eventId){
    OffsetDateTime freeze = event.getLeaderboardFreezeDuration();
    if(freeze != null && !OffsetDateTime.now().isBefore(freeze)){
      return subRepo.findFrozenLeaderboardByEventId(eventId, freeze);
    }
    return subRepo.findLeaderboardByEventId(eventId);
  }
}
