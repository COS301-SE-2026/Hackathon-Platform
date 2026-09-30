package com.hackathon.platform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.UserRepository;
import com.hackathon.platform.service.CodeWorkspaceService;
import com.hackathon.platform.service.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {
  @Mock private JwtService jwtService;
  @Mock private UserRepository userRepo;
  @Mock private CodeWorkspaceService codeService;
  @Mock private MessageChannel channel;
  private WebSocketConfig config;
  private ChannelInterceptor interceptor;

  private final UUID userId = UUID.randomUUID();
  private final UUID workspaceId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    config = new WebSocketConfig(jwtService, userRepo, codeService);

    // grab the interceptor the config registers so we can call preSend directly
    ChannelRegistration registration = mock(ChannelRegistration.class);
    config.configureClientInboundChannel(registration);
    ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
    verify(registration).interceptors(captor.capture());
    interceptor = captor.getValue();
  }

  private User userWithStatus(String status) {
    Role role = Role.builder().name("PARTICIPANT").build();
    return User.builder().userId(userId).status(status).role(role).build();
  }

  private Message<byte[]> toMessage(StompHeaderAccessor accessor) {
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<byte[]> connectMessage(String authHeader) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    if (authHeader != null) {
      accessor.addNativeHeader("Authorization", authHeader);
    }
    return toMessage(accessor);
  }

  private Message<byte[]> subscribeMessage(String destination, Object principal) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(destination);
    if (principal != null) {
      accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null));
    }
    return toMessage(accessor);
  }

  @Test
  void configureMessageBroker_setsAppPrefixAndTopicBroker() {
    MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

    config.configureMessageBroker(registry);

    verify(registry).setApplicationDestinationPrefixes("/app");
    verify(registry).enableSimpleBroker("/topic");
  }

  @Test
  void registerStompEndpoints_exposesWsEndpointForAllowedOrigins() {
    StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
    StompWebSocketEndpointRegistration endpoint = mock(StompWebSocketEndpointRegistration.class);
    when(registry.addEndpoint("/ws")).thenReturn(endpoint);

    config.registerStompEndpoints(registry);

    verify(endpoint)
        .setAllowedOriginPatterns("http://localhost:4200", "https://hackathonplatform.co.za");
  }

  @Test
  void connect_throws_whenAuthorizationHeaderMissing() {
    assertThatThrownBy(() -> interceptor.preSend(connectMessage(null), channel))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void connect_throws_whenHeaderIsNotBearer() {
    assertThatThrownBy(() -> interceptor.preSend(connectMessage("Basic abc"), channel))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void connect_throws_whenTokenInvalid() {
    when(jwtService.isTokenValid("bad")).thenReturn(false);

    assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer bad"), channel))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void connect_throws_whenUserNotFound() {
    when(jwtService.isTokenValid("tok")).thenReturn(true);
    when(jwtService.extractUserId("tok")).thenReturn(userId);
    when(userRepo.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer tok"), channel))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("could not be found");
  }

  @Test
  void connect_throws_whenUserDisabled() {
    when(jwtService.isTokenValid("tok")).thenReturn(true);
    when(jwtService.extractUserId("tok")).thenReturn(userId);
    when(userRepo.findById(userId)).thenReturn(Optional.of(userWithStatus("SUSPENDED")));

    assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer tok"), channel))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("disabled");
  }

  @Test
  void connect_setsAuthenticatedUser_whenTokenIsValid() {
    User user = userWithStatus("ACTIVE");
    when(jwtService.isTokenValid("tok")).thenReturn(true);
    when(jwtService.extractUserId("tok")).thenReturn(userId);
    when(userRepo.findById(userId)).thenReturn(Optional.of(user));
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.addNativeHeader("Authorization", "Bearer tok");
    Message<byte[]> message = toMessage(accessor);

    Message<?> result = interceptor.preSend(message, channel);

    assertThat(result).isSameAs(message);
    Authentication auth = (Authentication) accessor.getUser();
    assertThat(auth.getPrincipal()).isSameAs(user);
  }

  @Test
  void subscribe_throws_whenWorkspaceTopicHasNoUser() {
    Message<byte[]> message = subscribeMessage("/topic/workspaces/" + workspaceId, null);

    assertThatThrownBy(() -> interceptor.preSend(message, channel))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void subscribe_throws_whenPrincipalIsNotAUser() {
    Message<byte[]> message = subscribeMessage("/topic/workspaces/" + workspaceId, "not-a-user");

    assertThatThrownBy(() -> interceptor.preSend(message, channel))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void subscribe_throws_whenWorkspaceIdIsNotAUuid() {
    Message<byte[]> message = subscribeMessage("/topic/workspaces/nope", userWithStatus("ACTIVE"));

    assertThatThrownBy(() -> interceptor.preSend(message, channel))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Invalid workspace ID");
  }

  @Test
  void subscribe_checksWorkspaceAccess_forValidWorkspaceTopic() {
    User user = userWithStatus("ACTIVE");
    Message<byte[]> message = subscribeMessage("/topic/workspaces/" + workspaceId, user);

    Message<?> result = interceptor.preSend(message, channel);

    assertThat(result).isSameAs(message);
    verify(codeService).getWorkspaceForUser(workspaceId, user);
  }

  @Test
  void subscribe_letsOtherTopicsThrough_withoutAccessCheck() {
    Message<byte[]> message = subscribeMessage("/topic/other", null);

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    verifyNoInteractions(codeService);
  }

  @Test
  void preSend_ignoresOtherStompCommands() {
    Message<byte[]> message = toMessage(StompHeaderAccessor.create(StompCommand.SEND));

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
  }

  @Test
  void preSend_ignoresMessagesWithoutStompHeaders() {
    Message<String> message = MessageBuilder.withPayload("plain").build();

    assertThat(interceptor.preSend(message, channel)).isSameAs(message);
  }
}
