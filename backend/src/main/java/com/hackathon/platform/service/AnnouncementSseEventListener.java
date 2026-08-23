package com.hackathon.platform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AnnouncementSseEventListener {
  private final AnnouncementUpdateService announcementUpdateService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleAnnouncementCreated(AnnouncementCreatedEvent event) {
    announcementUpdateService.pushAnnouncementUpdate(event.eventId(), event.messageId());
  }
}
