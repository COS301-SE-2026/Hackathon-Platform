package com.hackathon.platform.scoring;

import com.hackathon.platform.dto.LeaderboardEntryResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.LeaderboardEntry;
import com.hackathon.platform.repository.SubmissionRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

  private final SubmissionRepository subRepo;
  private final EventRepository eventRepo;

  @Transactional(readOnly = true)
  public List<LeaderboardEntryResponse> getLeaderboard(UUID eventId, short levelId) {

    List<LeaderboardEntry> entries = subRepo.findLeaderboardByEventIdAndLevelId(eventId, levelId);

    return toResponses(entries);
  }

  @Transactional(readOnly = true)
  public List<LeaderboardEntryResponse> getEventLeaderboard(UUID eventId) {

    Event event =
        eventRepo
            .findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found"));

    List<LeaderboardEntry> entries = entriesForEvent(event, eventId);

    return toResponses(entries);
  }

  private List<LeaderboardEntry> entriesForEvent(Event event, UUID eventId) {

    OffsetDateTime freeze = event.getLeaderboardFreezeDateTime();

    if (freeze != null && !OffsetDateTime.now().isBefore(freeze)) {

      return subRepo.findFrozenLeaderboardByEventId(eventId, freeze);
    }

    return subRepo.findLeaderboardByEventId(eventId);
  }

  private List<LeaderboardEntryResponse> toResponses(List<LeaderboardEntry> entries) {

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
}
