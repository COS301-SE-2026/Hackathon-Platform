package com.hackathon.platform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnnouncementEmailService {
    private final JavaMailSender mail;
    
    @Value("${app.mail.from}")
    private String from;

    public void sendAnnouncementEmail(String recipient, String reply, String title, String body, String severity) {
        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setFrom(from);
        msg.setTo(recipient);
        msg.setReplyTo(reply);
        msg.setSubject(severity + ": " + title);
        msg.setText(body);

        mail.send(msg);
    }
}