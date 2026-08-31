package com.hackathon.platform.service;

import com.hackathon.platform.model.Event;
import com.hackathon.platform.repository.EventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventLifecycleService {
  private final EventRepository eventRepo;
  private final EventService eventService;

  @Scheduled(fixedDelayString = "${events.lifecycle-refresh-ms:30000}")
  @Transactional
  public void refreshStatuses() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    for (Event event : eventRepo.findAll()) {
      try {
        eventService.refreshLifecycleStatus(event, now);
      } catch (RuntimeException e) {
        log.error("Failed to refresh event lifecycle {}", event.getEventId(), e);
      }
    }
  }
}
