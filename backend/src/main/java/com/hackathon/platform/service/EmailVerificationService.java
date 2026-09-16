package com.hackathon.platform.service;

import com.hackathon.platform.model.EmailVerificationToken;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.EmailVerificationTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService{
    private final EmailVerificationTokenRepository tokenRepo;
    private final JavaMailSender email;

    @Value("${app.mail.from}") private String from;
    @Value("${app.frontend-url:http://localhost:4200}") private String frontendUrl;
    @Value("${app.auth.verification-expiry-hours:24}") private long expiryHours;

    @Transactional
    public void sendVerificationEmail(User user){
        tokenRepo.deleteByUserUserId(user.getUserId());
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String rawToken = HexFormat.of().formatHex(bytes);
        tokenRepo.save(EmailVerificationToken.builder().user(user).tokenHash(hash(rawToken)).expiresAt(LocalDateTime.now().plusHours(expiryHours)).build());
        String link = frontendUrl+"/verify-email?token="+rawToken;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(user.getEmail());
        msg.setSubject("Verify your Hackathon Platform email");
        msg.setText("Hi "+user.getFirstName()+",\n\nVerify your email by clicking this link please"+link+"\n\nThis link expires in "+expiryHours+" hours.");
        email.send(msg);
    }

    @Transactional
    public User verify(String rawToken){
        EmailVerificationService token = tokenRepo.findByTokenHash(hash(rawToken)).orElseThrow(() -> new IllegalArgumentException("Invalid or expired link"));
        if(token.getUsed() != null || token.getExpiredAt().isBefore(LocalDateTime.now())){
            throw new IllegalArgumentException("Invalid or expired verification link");
        }
        User user = token.getUser();
        user.setEmailVerified(true);
        token.setUsed(LocalDateTime.now());
        return user;
    }

    private String hash(String value){
        try{
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e){
            throw new IllegalArgumentException("Couldnt hash");
        }
    }
}