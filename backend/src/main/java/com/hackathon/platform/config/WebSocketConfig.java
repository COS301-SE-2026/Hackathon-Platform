package com.hackathon.platform.config;

import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.UserRepository;
import com.hackathon.platform.service.CodeWorkspaceService;
import com.hackathon.platform.service.JwtService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final JwtService jwtService;
  private final UserRepository userRepo;
  private final CodeWorkspaceService codeService;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.setApplicationDestinationPrefixes("/app");
    config.enableSimpleBroker("/topic");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry reg) {
    reg.addEndpoint("/ws")
        .setAllowedOriginPatterns("http://localhost:4200", "https://hackathonplatform.co.za");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration reg) {
    reg.interceptors(
        new ChannelInterceptor() {
          @Override
          public Message<?> preSend(Message<?> msg, MessageChannel channel) {
            StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(msg, StompHeaderAccessor.class);
            if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
              String authHeader = accessor.getFirstNativeHeader("Authorization");

              if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new AccessDeniedException("WebSocket authentication is missing");
              }

              String token = authHeader.substring(7);

              if (!jwtService.isTokenValid(token)) {
                throw new AccessDeniedException("Invalid WebSocket");
              }

              UUID userId = jwtService.extractUserId(token);
              User user =
                  userRepo
                      .findById(userId)
                      .orElseThrow(() -> new AccessDeniedException("User could not be found"));

              if (!user.isEnabled()) {
                throw new AccessDeniedException("User is disabled");
              }

              UsernamePasswordAuthenticationToken auth =
                  new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
              accessor.setUser(auth);
            }
            if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
              String dest = accessor.getDestination();
              if (dest != null && dest.startsWith("/topic/workspaces/")) {
                Authentication auth = (Authentication) accessor.getUser();
                if (auth == null || !(auth.getPrincipal() instanceof User user)) {
                  throw new AccessDeniedException("WebSocket user not authenticated");
                }
                String workspaceIdText = dest.substring("/topic/workspaces/".length());
                UUID workspaceId;
                try {
                  workspaceId = UUID.fromString(workspaceIdText);
                } catch (IllegalArgumentException e) {
                  throw new AccessDeniedException("Invalid workspace ID");
                }

                codeService.getWorkspaceForUser(workspaceId, user);
              }
            }
            return msg;
          }
        });
  }
}
