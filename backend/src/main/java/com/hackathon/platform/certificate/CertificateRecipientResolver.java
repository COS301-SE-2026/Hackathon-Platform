package com.hackathon.platform.certificate;

import com.hackathon.platform.dto.LeaderboardEntryResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.TeamMember;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.repository.UserRepository;
import com.hackathon.platform.scoring.LeaderboardService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CertificateRecipientResolver {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(" d MMMM yyyy");
  private final TeamRepository teamRepo;
  private final TeamMemberRepository teamMemRepo;
  private final UserRepository userRepo;
  private final LeaderboardService leaderboard;

  public CertificateRecipientResolver(
      TeamRepository teamRepo,
      TeamMemberRepository teamMemRepo,
      UserRepository userRepo,
      LeaderboardService leaderboard) {
    this.teamRepo = teamRepo;
    this.teamMemRepo = teamMemRepo;
    this.userRepo = userRepo;
    this.leaderboard = leaderboard;
  }

  public List<CertificateRecipient> resolve(Event event, String scope, Integer topN) {
    List<LeaderboardEntryResponse> leaderboard =
        leaderboard.getEventLeaderBoard(event.getEventId());
    Map<UUID, Integer> rankByTeam = new HashMap<>();
    for (LeaderboardEntryResponse entry : leaderboard) {
      rankByTeam.put(entry.getTeamId(), entry.getRank());
    }

    List<Team> teams = teamRepo.findByEventId(event.getEventId());
    String today = LocalDate.now().format(DATE_FORMAT);

    List<CertificateRecipient> recipients = new ArrayList();
    for (Team team : teams) {
      Integer rank = rankByTeam.get(team.getTeamId());
      if ("TOP_N".equalsIgnoreCase(scope)) {
        if (rank == null || topN == null || rank > topN) {
          continue;
        }
      }

      String certificateType = certificateTypeForRank(rank);
      List<TeamMember> members = teamMemRepo.findByTeamIdAndStatus(team.getTeamId(), "APPROVED");

      for (TeamMember member : members) {
        User user = userRepo.findById(member.getUserId()).orElse(null);
        if (user == null) {
          continue;
        }
        String fullName = (user.getFirstName() + " " + user.getLastName()).trim();

        Map<String, String> fields = new HashMap<>();
        fields.put("participantName", fullName);
        fields.put("teamName", team.getTeamName());
        fields.put("eventName", event.getName());
        fields.put("rank", rank == null ? "Unranked" : ordinal(rank));
        fields.put("certificateType", certificateType);
        fields.put("date", today);

        recipients.add(
            new CertificateRecipient(
                user.getUserId(), team.getTeamId(), fullName, rank, certificateType, fields));
      }
    }
    return recipients;
  }

  private String certificateTypeForRank(Integer rank) {
    if (rank == null) {
      return "PARTICIPATION";
    }
    return switch (rank) {
      case 1 -> "WINNER";
      case 2 -> "RUNNER_UP";
      case 3 -> "THIRD_PLACE";
      default -> "PARTICIPATION";
    };
  }

  private String ordinal(int n) {
    if (n % 100 >= 11 && n % 100 <= 13) {
      return n + "th";
    }
    return switch (n % 10) {
      case 1 -> n + "st";
      case 2 -> n + "nd";
      case 3 -> n + "rd";
      default -> n + "th";
    };
  }
}
