package com.hackathon.platform.service;

import com.hackathon.platform.config.AzureBlobConfig;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Level;
import com.hackathon.platform.model.SolverVersion;
import com.hackathon.platform.model.Submission;
import com.hackathon.platform.model.Team;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.LevelRepository;
import com.hackathon.platform.repository.SolverVersionRepository;
import com.hackathon.platform.repository.TeamMemberRepository;
import com.hackathon.platform.repository.TeamRepository;
import com.hackathon.platform.scoring.queue.ScoringJobProducer;
import com.hackathon.platform.storage.StorageException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SubmissionCreationService {
  private final StorageService storeService;
  private final AzureBlobConfig config;
  private final FileMetadataService metaService;
  private final SolverVersionRepository solverVersionRepository;
  private final EventRepository eventRepo;
  private final ScoringJobProducer producer;
  private final EventService eventService;
  private final TeamRepository teamRepo;
  private final TeamMemberRepository teamMemberRepo;
  private final LevelRepository levelRepo;

  public Map<String, String> createSubmission(
      String eventId,
      String teamId,
      MultipartFile outputFile,
      MultipartFile sourceFile,
      short levelId,
      User currUser) {
    if (outputFile == null || outputFile.isEmpty()) {
      throw new StorageException("Output file is missing");
    }

    if (sourceFile == null || sourceFile.isEmpty()) {
      throw new StorageException("Source file is missing");
    }

    String fileName =
        sourceFile.getOriginalFilename() == null
            ? ""
            : sourceFile.getOriginalFilename().toLowerCase();

    if (!fileName.endsWith(".zip")) {
      throw new StorageException("Source code must be zipped");
    }

    UUID eventUUID = UUID.fromString(eventId);
    UUID teamUUID = UUID.fromString(teamId);

    Event event = eventService.getEventById(eventUUID);
    eventService.refreshLifecycleStatus(event, OffsetDateTime.now(ZoneOffset.UTC));

    if (!"ACTIVE".equals(event.getStatus())) {
      throw new StorageException("you cannot submit before the event starts");
    }

    Team team =
        teamRepo.findById(teamUUID).orElseThrow(() -> new StorageException("Team not found"));

    if (!eventUUID.equals(team.getEventId())) {
      throw new StorageException("Team does not exist");
    }

    boolean teamMember =
        teamMemberRepo
            .findByUserIdAndStatusAndEventId(currUser.getUserId(), "APPROVED", eventUUID)
            .stream()
            .anyMatch(member -> teamUUID.equals(member.getTeamId()));

    if (!teamMember) {
      throw new StorageException("You are not part of this team");
    }

    Level level =
        levelRepo.findById(levelId).orElseThrow(() -> new StorageException("Level not found"));

    if (!hackathonIdMatchesEvent(level.getHackathonId(), event.getHackathon())) {
      throw new StorageException("Level doesnt exist");
    }

    UUID hackathonId =
        eventRepo
            .findHackathonIdByEventId(eventUUID)
            .orElseThrow(
                () -> new StorageException("Hackathon could not be found for event: " + eventId));

    SolverVersion latestSolver =
        solverVersionRepository
            .findByHackathonIdAndIsActiveTrue(hackathonId)
            .orElseThrow(() -> new StorageException("No active solver has been uploaded yet"));

    Submission saved =
        metaService.saveSubmission(
            eventId,
            teamUUID,
            levelId,
            latestSolver.getId(),
            outputFile.getOriginalFilename(),
            outputFile.getSize(),
            outputFile.getContentType(),
            sourceFile.getOriginalFilename(),
            sourceFile.getSize(),
            sourceFile.getContentType());

    storeService.upload(config.getSubmissionsContainer(), saved.getOutputStorageKey(), outputFile);
    storeService.upload(
        config.getSubmissionsContainer(), saved.getSourceCodeStorageKey(), sourceFile);

    String record = producer.enqueue(saved.getId());

    return Map.of(
        "submissionId",
        String.valueOf(saved.getId()),
        "outputStorageKey",
        saved.getOutputStorageKey(),
        "sourceStorageKey",
        saved.getSourceCodeStorageKey(),
        "status",
        "QUEUED",
        "scoringRecordId",
        record != null ? record : "");
  }

  private boolean hackathonIdMatchesEvent(UUID levelHackathonId, UUID eventHackathonId) {
    return levelHackathonId != null && levelHackathonId.equals(eventHackathonId);
  }
}
