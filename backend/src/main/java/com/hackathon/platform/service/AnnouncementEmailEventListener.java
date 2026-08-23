package com.hackathon.platform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AnnouncementEmailEventListener {
    private final AnnouncementEmailDeliveryService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAnnouncement(AnnouncementCreatedEvent event) {
        emailService.processMessage(event.messageId());
    }
}