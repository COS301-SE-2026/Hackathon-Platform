package com.hackathon.platform.service;

import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.RoleRepository;
import com.hackathon.platform.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class GoogleOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler{
    private final UserRepository userRepo;
    private RoleRepository roleRepo;
    private final JwtService jwtService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontend;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse resp, Authentication auth) throws IOException, ServletException {
        OAuth2User oauth = (OAuth2User) auth.getPrincipal();
        String email = oauth.getAttribute("email");
        Boolean verified = oauth.getAttribute("email_verified");
        if(email == null || !Boolean.TRUE.equals(verified)){
            resp.sendRedirect(frontend+"/login?error=google_email_not_verified");
            return;
        }

        String normalisedEmail = email.toLowerCase(Locale.ROOT);
        User user = userRepo.findByEmail(normalisedEmail).orElseGet(() -> createUser(oauth, normalisedEmail));
        if(!user.isEmailVerified()){
            user.setEmailVerified(true);
            userRepo.save(user);
        }
        String token = jwtService.generateToken(user);
        resp.sendRedirect(frontend+"/auth/oauth-success#token="+token);
    }

    private User createUser(OAuth2User oauth, String email){
        Role role = roleRepo.findByName("PARTICIPANT").orElseThrow(() -> new IllegalStateException("PARTICIPANT role not found"));
        String firstN = valueOr(oauth.getAttribute("given_name"), "Participant");
        String surN = valueOr(oauth.getAttribute("family_name"), "User");
        return userRepo.save(User.builder().firstName(firstN).lastName(surN).email(email).passwordHash(null).role(role).status("ACTIVE").emailVerified(true).build());
    }

    private String valueOr(String value, String fallback){
        return value == null || value.isBlank() ? fallback : value;
    }
}