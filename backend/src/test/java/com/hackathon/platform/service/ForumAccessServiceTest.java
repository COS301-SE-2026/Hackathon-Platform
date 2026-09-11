package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.ForumPermissionResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Role;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.repository.EventRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ForumAccessServiceTest {
  @Mock private EventRepository eventRepo;
  @Mock private EventRegistrationRepository eventRegRepo;

  @InjectMocks private ForumAccessService forumAccSer;

  private UUID eventId;
  private Event event;
  private User part;
  private User ownerAdmin;
  private User otherAdmin;
  private User superAdmin;

  @BeforeEach
  void setUp() {
    eventId = UUID.randomUUID();
    Role partRole = Role.builder().roleId(1).name("PARTICIPANT").build();
    Role adminRole = Role.builder().roleId(2).name("ADMIN").build();
    Role superAdminRole = Role.builder().roleId(3).name("SUPERADMIN").build();

    part = User.builder().userId(UUID.randomUUID()).role(partRole).build();
    ownerAdmin = User.builder().userId(UUID.randomUUID()).role(adminRole).build();
    otherAdmin = User.builder().userId(UUID.randomUUID()).role(adminRole).build();
    superAdmin = User.builder().userId(UUID.randomUUID()).role(superAdminRole).build();

    event = new Event();
    event.setEventId(eventId);
    event.setCreatedByUserId(ownerAdmin.getUserId());
  }

  @Test
  void requireForumAccess_registeredParticipant_returnEvent() {
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegRepo.existsByEventIdAndUserId(eventId, part.getUserId())).thenReturn(true);
    Event res = forumAccSer.requireForumAccess(eventId, part);
    assertThat(res).isSameAs(event);

    verify(eventRegRepo).existsByEventIdAndUserId(eventId, part.getUserId());
  }

  @Test
  void requireForumAccess_unregisteredParticipant_throwsAccessDeniedException() {
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegRepo.existsByEventIdAndUserId(eventId, part.getUserId())).thenReturn(false);
    assertThatThrownBy(() -> forumAccSer.requireForumAccess(eventId, part))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("not a participant");
  }

  @Test
  void requireForumAccess_eventOwner_returnEvent() {
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    Event res = forumAccSer.requireForumAccess(eventId, ownerAdmin);
    assertThat(res).isSameAs(event);

    verify(eventRegRepo, never()).existsByEventIdAndUserId(eventId, ownerAdmin.getUserId());
  }

  @Test
  void requireForumAccess_superAdmin_returnEvent() {
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    Event res = forumAccSer.requireForumAccess(eventId, superAdmin);
    assertThat(res).isSameAs(event);

    verify(eventRegRepo, never()).existsByEventIdAndUserId(eventId, superAdmin.getUserId());
  }

  @Test
  void requireForumAccess_nullUser_throwsAccessDeniedException() {
    assertThatThrownBy(() -> forumAccSer.requireForumAccess(eventId, null))
        .isInstanceOf(AccessDeniedException.class);
    verify(eventRepo, never()).findById(eventId);
  }

  @Test
  void requireModeratorAccess_nonOwnerAdmin_throwsAccessDeniedException() {
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    assertThatThrownBy(() -> forumAccSer.requireModeratorAccess(eventId, otherAdmin))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("cannot moderate");
  }

  @Test
  void getPermissions_registeredParticipant_canPostAndCommentButCannotModerate() {
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegRepo.existsByEventIdAndUserId(eventId, part.getUserId())).thenReturn(true);

    ForumPermissionResponse perms = forumAccSer.getPermissions(eventId, part);
    assertThat(perms.isCanCreatePost()).isTrue();
    assertThat(perms.isCanComment()).isTrue();
    assertThat(perms.isCanModerate()).isFalse();
  }

  @Test
  void getPermissions_eventOwnerAdmin_canPostAndCommentAndModerate() {
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegRepo.existsByEventIdAndUserId(eventId, ownerAdmin.getUserId())).thenReturn(true);

    ForumPermissionResponse perms = forumAccSer.getPermissions(eventId, ownerAdmin);
    assertThat(perms.isCanCreatePost()).isTrue();
    assertThat(perms.isCanComment()).isTrue();
    assertThat(perms.isCanModerate()).isTrue();
  }
}
