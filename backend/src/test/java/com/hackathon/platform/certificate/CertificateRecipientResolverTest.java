package com.hackathon.platform.certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.LeaderboardEntryResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.TeamMember;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.repository.UserRepository;
import com.hackathon.platform.scoring.LeaderboardService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificateRecipientResolverTest {
  @Mock private TeamRepository teamRepo;
  @Mock private TeamMemberRepository teamMemRepo;
  @Mock private UserRepository userRepo;
  @Mock private LeaderboardService leaderboardService;

  private CertificateRecipientResolver resolver;
  private Event event;

  @BeforeEach
  void setUp() {
    resolver =
        new CertificateRecipientResolver(teamRepo, teamMemRepo, userRepo, leaderboardService);
    event = new Event();
    event.setEventId(UUID.randomUUID());
    event.setName("Hack 2026");
  }

  private Team team(String name) {
    Team t = new Team(name, UUID.randomUUID(), event.getEventId());
    t.setTeamId(UUID.randomUUID());
    return t;
  }

  private User user(String first, String last) {
    return User.builder().userId(UUID.randomUUID()).firstName(first).lastName(last).build();
  }

  private LeaderboardEntryResponse entry(Team t, int rank) {
    return new LeaderboardEntryResponse(
        rank, t.getTeamId(), t.getTeamName(), BigDecimal.TEN, Instant.now());
  }

  private void stubMember(Team t, User u) {
    when(teamMemRepo.findByTeamIdAndStatus(t.getTeamId(), "APPROVED"))
        .thenReturn(List.of(new TeamMember(t.getTeamId(), u.getUserId())));
    when(userRepo.findById(u.getUserId())).thenReturn(Optional.of(u));
  }

  private void stubEvent(List<Team> teams, List<LeaderboardEntryResponse> board) {
    when(leaderboardService.getEventLeaderboard(event.getEventId())).thenReturn(board);
    when(teamRepo.findByEventId(event.getEventId())).thenReturn(teams);
  }

  @ParameterizedTest
  @CsvSource({
    "1,WINNER,1st",
    "2,RUNNER_UP,2nd",
    "3,THIRD_PLACE,3rd",
    "4,PARTICIPATION,4th",
    "10,PARTICIPATION,10th",
    "11,PARTICIPATION,11th",
    "12,PARTICIPATION,12th",
    "13,PARTICIPATION,13th",
    "21,PARTICIPATION,21st",
    "22,PARTICIPATION,22nd",
    "23,PARTICIPATION,23rd",
    "111,PARTICIPATION,111th",
    "112,PARTICIPATION,112th"
  })
  void resolve_mapsRankToTypeAndOrdinal(int rank, String type, String ordinal) {
    Team t = team("Rockets");
    User u = user("Lewis", "Hamilton");
    stubEvent(List.of(t), List.of(entry(t, rank)));
    stubMember(t, u);

    List<CertificateRecipient> result = resolver.resolve(event, "ALL_PARTICIPANTS", null);

    assertThat(result).hasSize(1);
    CertificateRecipient r = result.get(0);
    assertThat(r.getRank()).isEqualTo(rank);
    assertThat(r.getCertificateType()).isEqualTo(type);
    assertThat(r.getFieldValues()).containsEntry("rank", ordinal);
    assertThat(r.getFieldValues()).containsEntry("certificateType", type);
  }

  @Test
  void resolve_populatesRecipientAndFieldValues() {
    Team t = team("Rockets");
    User u = user("Lewis", "Hamilton");
    stubEvent(List.of(t), List.of(entry(t, 1)));
    stubMember(t, u);

    CertificateRecipient r = resolver.resolve(event, "ALL_PARTICIPANTS", null).get(0);

    assertThat(r.getUserId()).isEqualTo(u.getUserId());
    assertThat(r.getTeamId()).isEqualTo(t.getTeamId());
    assertThat(r.getRecipientName()).isEqualTo("Lewis Hamilton");
    assertThat(r.getFieldValues())
        .containsEntry("participantName", "Lewis Hamilton")
        .containsEntry("teamName", "Rockets")
        .containsEntry("eventName", "Hack 2026");
    String expectedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy"));
    assertThat(r.getFieldValues().get("date").trim()).isEqualTo(expectedDate);
  }

  @Test
  void resolve_unrankedTeam_getsParticipation() {
    Team t = team("Unranked");
    User u = user("Sam", "Smith");
    stubEvent(List.of(t), List.of());
    stubMember(t, u);

    CertificateRecipient r = resolver.resolve(event, "ALL_PARTICIPANTS", null).get(0);

    assertThat(r.getRank()).isNull();
    assertThat(r.getCertificateType()).isEqualTo("PARTICIPATION");
    assertThat(r.getFieldValues()).containsEntry("rank", "Unranked");
  }

  @Test
  void resolve_nullScope_treatedAsAllParticipants() {
    Team t = team("Unranked");
    User u = user("Sam", "Smith");
    stubEvent(List.of(t), List.of());
    stubMember(t, u);

    assertThat(resolver.resolve(event, null, null)).hasSize(1);
  }

  @Test
  void resolve_trimsFullNameWhenLastNameEmpty() {
    Team t = team("Solo");
    User u = user("Cher", "");
    stubEvent(List.of(t), List.of());
    stubMember(t, u);

    assertThat(resolver.resolve(event, "ALL_PARTICIPANTS", null).get(0).getRecipientName())
        .isEqualTo("Cher");
  }

  @Test
  void resolve_createsOneRecipientPerApprovedMember() {
    Team t = team("Duo");
    User a = user("Ann", "A");
    User b = user("Bob", "B");
    stubEvent(List.of(t), List.of(entry(t, 2)));
    when(teamMemRepo.findByTeamIdAndStatus(t.getTeamId(), "APPROVED"))
        .thenReturn(
            List.of(
                new TeamMember(t.getTeamId(), a.getUserId()),
                new TeamMember(t.getTeamId(), b.getUserId())));
    when(userRepo.findById(a.getUserId())).thenReturn(Optional.of(a));
    when(userRepo.findById(b.getUserId())).thenReturn(Optional.of(b));

    assertThat(resolver.resolve(event, "ALL_PARTICIPANTS", null))
        .extracting(CertificateRecipient::getRecipientName)
        .containsExactly("Ann A", "Bob B");
  }

  @Test
  void resolve_skipsMembersWhoseUserNoLongerExists() {
    Team t = team("Ghosts");
    User real = user("Real", "Person");
    UUID missingId = UUID.randomUUID();
    stubEvent(List.of(t), List.of());
    when(teamMemRepo.findByTeamIdAndStatus(t.getTeamId(), "APPROVED"))
        .thenReturn(
            List.of(
                new TeamMember(t.getTeamId(), missingId),
                new TeamMember(t.getTeamId(), real.getUserId())));
    when(userRepo.findById(missingId)).thenReturn(Optional.empty());
    when(userRepo.findById(real.getUserId())).thenReturn(Optional.of(real));

    assertThat(resolver.resolve(event, "ALL_PARTICIPANTS", null))
        .extracting(CertificateRecipient::getRecipientName)
        .containsExactly("Real Person");
  }

  @Test
  void resolve_noTeams_returnsEmpty() {
    stubEvent(List.of(), List.of());
    assertThat(resolver.resolve(event, "ALL_PARTICIPANTS", null)).isEmpty();
  }

  @Test
  void resolve_topN_onlyIncludesTeamsWithinRank() {
    Team first = team("First");
    Team fifth = team("Fifth");
    Team unranked = team("Unranked");
    User u = user("Top", "Player");
    stubEvent(List.of(first, fifth, unranked), List.of(entry(first, 1), entry(fifth, 5)));
    stubMember(first, u);

    List<CertificateRecipient> result = resolver.resolve(event, "TOP_N", 3);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTeamId()).isEqualTo(first.getTeamId());
    verify(teamMemRepo, never()).findByTeamIdAndStatus(fifth.getTeamId(), "APPROVED");
    verify(teamMemRepo, never()).findByTeamIdAndStatus(unranked.getTeamId(), "APPROVED");
  }

  @Test
  void resolve_topN_isCaseInsensitive_andIncludesBoundaryRank() {
    Team third = team("Third");
    User u = user("Bronze", "Medal");
    stubEvent(List.of(third), List.of(entry(third, 3)));
    stubMember(third, u);

    assertThat(resolver.resolve(event, "top_n", 3)).hasSize(1);
  }

  @Test
  void resolve_topN_withNullN_returnsNothing() {
    Team first = team("First");
    stubEvent(List.of(first), List.of(entry(first, 1)));

    assertThat(resolver.resolve(event, "TOP_N", null)).isEmpty();
    verify(userRepo, never()).findById(any());
  }

  @Test
  void recipient_exposesConstructorValues() {
    UUID userId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();
    CertificateRecipient r =
        new CertificateRecipient(
            userId, teamId, "Lewis", 2, "RUNNER_UP", java.util.Map.of("a", "b"));
    assertThat(r.getUserId()).isEqualTo(userId);
    assertThat(r.getTeamId()).isEqualTo(teamId);
    assertThat(r.getRecipientName()).isEqualTo("Lewis");
    assertThat(r.getRank()).isEqualTo(2);
    assertThat(r.getCertificateType()).isEqualTo("RUNNER_UP");
    assertThat(r.getFieldValues()).containsEntry("a", "b");
  }
}
