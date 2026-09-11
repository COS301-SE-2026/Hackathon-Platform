package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.CreateAnnouncementRequest;
import com.hackathon.platform.model.CommunicationChannel;
import com.hackathon.platform.model.CommunicationMessage;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.AnnouncementEmailDeliveryRepository;
import com.hackathon.platform.repository.CommunicationChannelRepository;
import com.hackathon.platform.repository.CommunicationMessageRepository;
import com.hackathon.platform.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {
  @Mock private CommunicationChannelRepository channelRepo;
  @Mock private CommunicationMessageRepository messageRepo;
  @Mock private AnnouncementEmailDeliveryRepository deliveryRepo;
  @Mock private UserRepository userRepo;
  @Mock private AnnouncementAccessService accSer;
  @Mock private ApplicationEventPublisher eventPublish;
  @Mock private CommunicationChannel channel;
  @Mock private CommunicationMessage message;
  @Mock private User user;
  @Mock private CreateAnnouncementRequest req;
  @InjectMocks private AnnouncementService annSer;

  private UUID eventId;
  private UUID userId;
  private UUID channelId;
  private UUID messageId;

  @BeforeEach
  void setUp() {
    eventId = UUID.randomUUID();
    userId = UUID.randomUUID();
    channelId = UUID.randomUUID();
    messageId = UUID.randomUUID();
  }

  @Test
  void getAnnouncements_noChannel_returnsEmpty() {
    when(channelRepo.findByChannelTypeAndEventId("EVENT_ANNOUNCEMENT", eventId))
        .thenReturn(Optional.empty());
    assertThat(annSer.getAnnouncements(eventId, userId)).isEmpty();
    verify(accSer).requireParticipantAccess(eventId, userId);
  }

  @Test
  void getAnnouncements_existingChannel_returnsMessages() {
    when(channel.getChannelId()).thenReturn(channelId);
    when(channelRepo.findByChannelTypeAndEventId("EVENT_ANNOUNCEMENT", eventId))
        .thenReturn(Optional.of(channel));
    when(messageRepo.findTop100ByChannelIdOrderByCreatedAtDesc(channelId))
        .thenReturn(List.of(message));

    assertThat(annSer.getAnnouncements(eventId, userId)).hasSize(1);
  }

  @Test
  void getAnnouncementsForAdmin_returnsMessages() {
    when(channel.getChannelId()).thenReturn(channelId);
    when(channelRepo.findByChannelTypeAndEventId("EVENT_ANNOUNCEMENT", eventId))
        .thenReturn(Optional.of(channel));
    when(messageRepo.findTop100ByChannelIdOrderByCreatedAtDesc(channelId)).thenReturn(List.of());

    annSer.getAnnouncementsForAdmin(eventId, userId);
    verify(accSer).requireEventOwner(eventId, userId);
  }

  @Test
  void createAnnouncement_existingChannel_success() {
    when(channel.getChannelId()).thenReturn(channelId);
    when(message.getMessageId()).thenReturn(messageId);
    when(req.getTitle()).thenReturn("Title");
    when(req.getBody()).thenReturn("Body");
    when(channelRepo.findByChannelTypeAndEventId("EVENT_ANNOUNCEMENT", eventId))
        .thenReturn(Optional.of(channel));
    when(messageRepo.save(any(CommunicationMessage.class))).thenReturn(message);
    when(user.getUserId()).thenReturn(userId);
    when(user.getEmail()).thenReturn("test@test.com");
    when(userRepo.findActiveParticipantsByEventId(eventId)).thenReturn(List.of(user));

    assertThat(annSer.createAnnouncement(eventId, userId, req)).isNotNull();
    verify(deliveryRepo).saveAll(any());
    verify(eventPublish).publishEvent(any(AnnouncementCreatedEvent.class));
  }

  @Test
  void createAnnouncement_noParticipants_success() {
    when(channel.getChannelId()).thenReturn(channelId);
    when(message.getMessageId()).thenReturn(messageId);
    when(req.getTitle()).thenReturn("Title");
    when(req.getBody()).thenReturn("Body");
    when(channelRepo.findByChannelTypeAndEventId("EVENT_ANNOUNCEMENT", eventId))
        .thenReturn(Optional.empty());
    when(channelRepo.save(any(CommunicationChannel.class))).thenReturn(channel);
    when(messageRepo.save(any(CommunicationMessage.class))).thenReturn(message);
    when(userRepo.findActiveParticipantsByEventId(eventId)).thenReturn(List.of());

    assertThat(annSer.createAnnouncement(eventId, userId, req)).isNotNull();
    verify(deliveryRepo, never()).saveAll(any());
    verify(eventPublish).publishEvent(any(AnnouncementCreatedEvent.class));
  }
}
