package com.hackathon.platform.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.LeaderboardEntryResponse;
import com.hackathon.platform.repository.LeaderboardEntry;
import com.hackathon.platform.repository.SubmissionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {
    @Mock private SubmissionRepository submissionRepo;
    private LeaderboardService leaderService;
    private static final UUID EVENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID FIRST_TEAM =UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private static final UUID SECOND_TEAM = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");
    private static final Long LVL_ID = 1L;
    private static final Instant SCORED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        leaderService = new LeaderboardService(submissionRepo);
    }

    @Test
    void getLeaderboard_assignRanksInRepositoryOrder() {
        when(submissionRepo.findLeaderboardByEventIdAndLevelId(EVENT_ID, LVL_ID))
            .thenReturn(
                List.of(
                    entry(FIRST_TEAM, "Debug Thugs", "97.00", SCORED_AT),
                    entry(SECOND_TEAM, "Byte Busters", "85.00", SCORED_AT)
                )
            );

        List<LeaderboardEntryResponse> res = leaderService.getLeaderboard(EVENT_ID, LVL_ID);

        assertThat(res).hasSize(2);

        assertThat(res.get(0).getRank()).isEqualTo(1);
        assertThat(res.get(0).getTeamId()).isEqualTo(FIRST_TEAM);
        assertThat(res.get(0).getTeamName()).isEqualTo("Debug Thugs");
        assertThat(res.get(0).getBestScore()).isEqualByComparingTo("97.00");
        assertThat(res.get(0).getLastScoredAt()).isEqualTo(SCORED_AT);
        
        assertThat(res.get(1).getRank()).isEqualTo(2);
        assertThat(res.get(1).getTeamId()).isEqualTo(SECOND_TEAM);
        assertThat(res.get(1).getTeamName()).isEqualTo("Byte Busters");
        assertThat(res.get(1).getBestScore()).isEqualByComparingTo("85.00");
        assertThat(res.get(1).getLastScoredAt()).isEqualTo(SCORED_AT);
    }

    @Test
    void getEventLeaderboard_assignRanksInRepositoryOrder() {
        when(submissionRepo.findLeaderboardByEventId(EVENT_ID))
            .thenReturn(
                    List.of(
                        entry(FIRST_TEAM, "Debug Thugs", "97.00", SCORED_AT),
                        entry(SECOND_TEAM, "Byte Busters", "85.00", SCORED_AT)
                    )
            );
        
        List<LeaderboardEntryResponse> res = leaderService.getEventLeaderboard(EVENT_ID);

        assertThat(res).hasSize(2);

        assertThat(res.get(0).getRank()).isEqualTo(1);
        assertThat(res.get(0).getTeamId()).isEqualTo(FIRST_TEAM);
        assertThat(res.get(0).getTeamName()).isEqualTo("Debug Thugs");
        assertThat(res.get(0).getBestScore()).isEqualByComparingTo("97.00");
        assertThat(res.get(0).getLastScoredAt()).isEqualTo(SCORED_AT);
        
        assertThat(res.get(1).getRank()).isEqualTo(2);
        assertThat(res.get(1).getTeamId()).isEqualTo(SECOND_TEAM);
        assertThat(res.get(1).getTeamName()).isEqualTo("Byte Busters");
        assertThat(res.get(1).getBestScore()).isEqualByComparingTo("85.00");
        assertThat(res.get(1).getLastScoredAt()).isEqualTo(SCORED_AT);
    }

    private static LeaderboardEntry entry(UUID teamId, String teamName, String score, Instant lastScoredAt) {
        return new TestLeaderboardEntry(teamId, teamName, new BigDecimal(score), lastScoredAt);
    }

    private static class TestLeaderboardEntry implements LeaderboardEntry {
        private final UUID teamId;
        private final String teamName;
        private final BigDecimal bestScore;
        private final Instant lastScoredAt;

        private TestLeaderboardEntry(UUID teamId, String teamName, BigDecimal bestScore, Instant lastScoredAt) {
            this.teamId = teamId;
            this.teamName = teamName;
            this.bestScore = bestScore;
            this.lastScoredAt = lastScoredAt;
        }

        @Override
        public UUID getTeamId() {
            return teamId;
        }

        @Override
        public String getTeamName() {
            return teamName;
        }

        @Override
        public BigDecimal getBestScore() {
            return bestScore;
        }

        @Override
        public Instant getLastScoredAt() {
            return lastScoredAt;
        }
    }
}