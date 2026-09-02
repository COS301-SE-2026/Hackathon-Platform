package com.hackathon.platform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ForumSseEventListener {
  private final ForumUpdateService forumUpdateService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleForumUpdated(ForumUpdatedEvent event) {
    forumUpdateService.pushForumUpdate(event.eventId(), event.action(), event.resourceId());
  }
}
