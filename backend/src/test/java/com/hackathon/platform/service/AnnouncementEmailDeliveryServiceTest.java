package com.hackathon.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.model.AnnouncementEmailDelivery;
import com.hackathon.platform.model.CommunicationMessage;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.AnnouncementEmailDeliveryRepository;
import com.hackathon.platform.repository.CommunicationMessageRepository;
import com.hackathon.platform.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnouncementEmailDeliveryServiceTest {
    @Mock
    private AnnouncementEmailDeliveryRepository emailDeliveryRepo;
    
    @Mock
    private CommunicationMessageRepository communicationRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private AnnouncementEmailService announcementEmailService;

    private AnnouncementEmailDeliveryService deliveryService;
    private UUID msgId;
    private UUID adminId;
    private CommunicationMessage msg;
    private User admin;
    private AnnouncementEmailDelivery delivery;

    @BeforeEach
    void setUp() {
        deliveryService = new AnnouncementEmailDeliveryService(emailDeliveryRepo, communicationRepo, userRepo, announcementEmailService);
        msgId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        msg = new CommunicationMessage();
        msg.setCreatedByUserId(adminId);
        msg.setTitle("TEST");
        msg.setBody("BODY");
        msg.setSeverity("IMPORTANT");

        admin = new User();
        admin.setEmail("test@email.com");

        delivery = new AnnouncementEmailDelivery();
        delivery.setRecipientEmail("send@here.com");
        delivery.setDeliveryStatus("PENDING");
        delivery.setAttemptCount(0);
    }

    @Test
    void successfulEmailIsMarkedAsSent() {
        when(communicationRepo.findById(msgId)).thenReturn(Optional.of(msg));
        when(userRepo.findById(adminId)).thenReturn(Optional.of(admin));
        when(emailDeliveryRepo.findByMessageId(msgId)).thenReturn(List.of(delivery));

        deliveryService.processMessage(msgId);
        verify(announcementEmailService).sendAnnouncementEmail("send@here.com", "test@email.com", "TEST", "BODY", "IMPORTANT");

        assertEquals("SENT", delivery.getDeliveryStatus());
        assertEquals(1, delivery.getAttemptCount());
        assertNotNull(delivery.getSentAt());
        assertNull(delivery.getLastError());

        verify(emailDeliveryRepo, times(2)).save(delivery);
    }

    @Test
    void failedEmailIsMarkedAsFailed() {
        when(communicationRepo.findById(msgId)).thenReturn(Optional.of(msg));
        when(userRepo.findById(adminId)).thenReturn(Optional.of(admin));
        when(emailDeliveryRepo.findByMessageId(msgId)).thenReturn(List.of(delivery));

        doThrow(new RuntimeException("SMTP failed")).when(announcementEmailService).sendAnnouncementEmail("send@here.com", "test@email.com", "TEST", "BODY", "IMPORTANT");
        deliveryService.processMessage(msgId);

        assertEquals("FAILED", delivery.getDeliveryStatus());
        assertEquals(1, delivery.getAttemptCount());
        assertEquals("SMTP failed", delivery.getLastError());
        assertNull(delivery.getSentAt());

        verify(emailDeliveryRepo, times(2)).save(delivery);
    }
    
    @Test
    void sentEmailIsNotSentAgain() {
        delivery.setDeliveryStatus("SENT");
        when(communicationRepo.findById(msgId)).thenReturn(Optional.of(msg));
        when(userRepo.findById(adminId)).thenReturn(Optional.of(admin));
        when(emailDeliveryRepo.findByMessageId(msgId)).thenReturn(List.of(delivery));
        deliveryService.processMessage(msgId);
        verify(announcementEmailService, never()).sendAnnouncementEmail("send@here.com", "test@email.com", "TEST", "BODY", "IMPORTANT");
        verify(emailDeliveryRepo, never()).save(delivery);
    }

}