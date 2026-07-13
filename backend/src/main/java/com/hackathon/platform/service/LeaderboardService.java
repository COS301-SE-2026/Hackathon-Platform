package com.hackathon.platform.scoring;

import com.hackathon.platform.dto.LeaderboardEntryResponse;
import com.hackathon.platform.repository.LeaderboardEntry;
import com.hackathon.platform.repository.SubmissionRepository;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaderboardService {
    private final SubmissionRepository subRepo;

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard(UUID eventId, Long levelId) {
        List<LeaderboardEntry> entries = subRepo.findLeaderboardByEventIdAndLevelId(eventId, levelId);
        List<LeaderboardEntryResponse> leaderboard = new ArrayList<>(entries.size());

        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            leaderboard.add(new LeaderboardEntryResponse(
                i + 1,
                entry.getTeamId(),
                entry.getTeamName(),
                entry.getBestScore(),
                entry.getLastScoredAt()
            ));
        }

        return leaderboard;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getEventLeaderboard(UUID eventId) {
        List<LeaderboardEntry> entries = subRepo.findLeaderboardByEventId(eventId);
        List<LeaderboardEntryResponse> leaderboard = new ArrayList<>(entries.size());

        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            leaderboard.add(new LeaderboardEntryResponse(
                i + 1,
                entry.getTeamId(),
                entry.getTeamName(),
                entry.getBestScore(),
                entry.getLastScoredAt()
            ));
        }

        return leaderboard;
    }
}