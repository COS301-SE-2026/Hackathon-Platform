package com.hackathon.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnnouncementEmailServiceTest {
    @Mock private JavaMailSender mail;
    private AnnouncementEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new AnnouncementEmailService(mail);

        ReflectionTestUtils.setField(emailService, "from", "keyboardgremlins@gmail.com");
    }

    @Test
    void sendAnnouncementEmailCreatesCorrectEmail() {
        emailService.sendAnnouncementEmail("test@test.com", "jane@test.com", "Schedule changed", "This event start time is changing", "IMPORTANT");

        ArgumentCaptor<SimpleMailMessage> cap = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(cap.capture());

        SimpleMailMessage msg = cap.getValue();

        assertEquals("keyboardgremlins@gmail.com", msg.getFrom());
        assertArrayEquals(new String[] {"test@test.com"}, msg.getTo());
        assertEquals("jane@test.com", msg.getReplyTo());
        assertEquals("IMPORTANT: Schedule changed", msg.getSubject());
        assertEquals("This event start time is changing", msg.getText());
    }
}

