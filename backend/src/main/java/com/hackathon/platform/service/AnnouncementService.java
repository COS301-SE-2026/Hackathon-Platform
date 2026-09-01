package com.hackathon.platform.service;

import com.hackathon.platform.dto.AnnouncementResponse;
import com.hackathon.platform.dto.CreateAnnouncementRequest;
import com.hackathon.platform.dto.CreateAnnouncementResponse;
import com.hackathon.platform.model.AnnouncementEmailDelivery;
import com.hackathon.platform.model.CommunicationChannel;
import com.hackathon.platform.model.CommunicationMessage;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.AnnouncementEmailDeliveryRepository;
import com.hackathon.platform.repository.CommunicationChannelRepository;
import com.hackathon.platform.repository.CommunicationMessageRepository;
import com.hackathon.platform.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
  private static final String EVENT_ANNOUNCEMENT = "EVENT_ANNOUNCEMENT";
  private final CommunicationChannelRepository channelRepo;
  private final CommunicationMessageRepository messageRepo;
  private final AnnouncementEmailDeliveryRepository emailDeliveryRepo;
  private final UserRepository userRepo;
  private final AnnouncementAccessService announcementAccessService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public CreateAnnouncementResponse createAnnouncement(
      UUID eventId, UUID adminId, CreateAnnouncementRequest req) {
    announcementAccessService.requireEventOwner(eventId, adminId);
    CommunicationChannel channel = getOrCreateEventAnnouncementChannel(eventId);
    CommunicationMessage msg = new CommunicationMessage();

    msg.setChannelId(channel.getChannelId());
    msg.setCreatedByUserId(adminId);
    msg.setTitle(req.getTitle().trim());
    msg.setBody(req.getBody().trim());
    msg.setSeverity(req.getSeverity());
    CommunicationMessage savedMessage = messageRepo.save(msg);

    List<User> users = userRepo.findActiveParticipantsByEventId(eventId);
    List<AnnouncementEmailDelivery> deliveries =
        users.stream().map(user -> createEmailDelivery(savedMessage.getMessageId(), user)).toList();

    if (!deliveries.isEmpty()) {
      emailDeliveryRepo.saveAll(deliveries);
    }

    eventPublisher.publishEvent(new AnnouncementCreatedEvent(eventId, savedMessage.getMessageId()));

    AnnouncementResponse announcementResponse = toResponse(savedMessage, eventId);

    return new CreateAnnouncementResponse(announcementResponse, users.size(), "PENDING");
  }

  @Transactional(readOnly = true)
  public List<AnnouncementResponse> getAnnouncements(UUID eventId, UUID userId) {
    announcementAccessService.requireParticipantAccess(eventId, userId);
    return getAnnouncementsForEvent(eventId);
  }

  @Transactional(readOnly = true)
  public List<AnnouncementResponse> getAnnouncementsForAdmin(UUID eventId, UUID adminId) {
    announcementAccessService.requireEventOwner(eventId, adminId);
    return getAnnouncementsForEvent(eventId);
  }

  private CommunicationChannel getOrCreateEventAnnouncementChannel(UUID eventId) {
    return channelRepo
        .findByChannelTypeAndEventId(EVENT_ANNOUNCEMENT, eventId)
        .orElseGet(
            () -> {
              CommunicationChannel channel = new CommunicationChannel();
              channel.setChannelType(EVENT_ANNOUNCEMENT);
              channel.setEventId(eventId);
              return channelRepo.save(channel);
            });
  }

  private List<AnnouncementResponse> getAnnouncementsForEvent(UUID eventId) {
    return channelRepo
        .findByChannelTypeAndEventId(EVENT_ANNOUNCEMENT, eventId)
        .map(
            channel ->
                messageRepo
                    .findTop100ByChannelIdOrderByCreatedAtDesc(channel.getChannelId())
                    .stream()
                    .map(message -> toResponse(message, eventId))
                    .toList())
        .orElseGet(Collections::emptyList);
  }

  private AnnouncementEmailDelivery createEmailDelivery(UUID messageId, User user) {
    AnnouncementEmailDelivery delivery = new AnnouncementEmailDelivery();
    delivery.setMessageId(messageId);
    delivery.setUserId(user.getUserId());
    delivery.setRecipientEmail(user.getEmail());
    delivery.setDeliveryStatus("PENDING");
    delivery.setAttemptCount(0);
    return delivery;
  }

  private AnnouncementResponse toResponse(CommunicationMessage message, UUID eventId) {
    return new AnnouncementResponse(
        message.getMessageId(),
        eventId,
        message.getTitle(),
        message.getBody(),
        message.getSeverity(),
        message.getCreatedAt());
  }
}
