package com.hackathon.platform.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface LeaderboardEntry {
  UUID getTeamId();

  String getTeamName();

  BigDecimal getBestScore();

  Instant getLastScoredAt();
}
