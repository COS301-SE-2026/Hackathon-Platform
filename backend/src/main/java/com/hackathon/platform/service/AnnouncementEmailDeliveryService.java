package com.hackathon.platform.service;

import com.hackathon.platform.model.AnnouncementEmailDelivery;
import com.hackathon.platform.model.CommunicationMessage;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.AnnouncementEmailDeliveryRepository;
import com.hackathon.platform.repository.CommunicationMessageRepository;
import com.hackathon.platform.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnnouncementEmailDeliveryService {
  private static final int MAX_ATTEMPT = 3;
  private final AnnouncementEmailDeliveryRepository emailDeliveryRepo;
  private final CommunicationMessageRepository communicationRepo;
  private final UserRepository userRepo;
  private final AnnouncementEmailService announcementEmailService;

  public void processMessage(UUID msgId) {
    CommunicationMessage msg =
        communicationRepo
            .findById(msgId)
            .orElseThrow(
                () -> new IllegalArgumentException("Announcement could not be found: " + msgId));
    User admin =
        userRepo
            .findById(msg.getCreatedByUserId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Admin of the announcement could not be found: "
                            + msg.getCreatedByUserId()));
    List<AnnouncementEmailDelivery> deliveries = emailDeliveryRepo.findByMessageId(msgId);

    for (AnnouncementEmailDelivery delivery : deliveries) {
      process(delivery, msg, admin);
    }
  }

  private void process(AnnouncementEmailDelivery delivery, CommunicationMessage msg, User admin) {
    if ("SENT".equals(delivery.getDeliveryStatus())) {
      return;
    }

    if (delivery.getAttemptCount() >= MAX_ATTEMPT) {
      return;
    }

    delivery.setDeliveryStatus("PROCESSING");
    delivery.setAttemptCount(delivery.getAttemptCount() + 1);
    delivery.setLastError(null);
    emailDeliveryRepo.save(delivery);

    try {
      announcementEmailService.sendAnnouncementEmail(
          delivery.getRecipientEmail(),
          admin.getEmail(),
          msg.getTitle(),
          msg.getBody(),
          msg.getSeverity());

      delivery.setDeliveryStatus("SENT");
      delivery.setSentAt(Instant.now());
      delivery.setLastError(null);
    } catch (Exception exception) {
      delivery.setDeliveryStatus("FAILED");
      delivery.setLastError(exception.getMessage());
    }

    emailDeliveryRepo.save(delivery);
  }
}
