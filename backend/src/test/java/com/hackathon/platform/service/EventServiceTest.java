package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.EventRequest;
import com.hackathon.platform.dto.EventStatusResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.EventRepository;
import com.hackathon.platform.repository.HackathonRepository;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
  @Mock private EventRepository eventRepository;
  @Mock private EventRequest eventRequest;
  @Mock private HackathonRepository hackathonRepository;
  @InjectMocks EventService eventService;

  private UUID eventId;
  private UUID creatorUserId;
  private Event event;

  @BeforeEach
  void setUp() {
    eventId = UUID.randomUUID();
    creatorUserId = UUID.randomUUID();
    User admin = new User();
    admin.setUserId(creatorUserId);
    admin.setEmail("admin@test.com");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(admin, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);

    event = new Event();
    event.setEventId(eventId);
    event.setCreatedByUserId(creatorUserId);
    event.setName("Test Hackathon");
    event.setVisibility("PUBLIC");
    event.setStatus("ACTIVE");
    event.setRegistrationKey(null);
    event.setStartDateTime(OffsetDateTime.now().minusHours(1));
    event.setDuration(48 * 3600);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getEventByCreator_withValidCreatorId_returnsMatchingEvents() {
    when(eventRepository.fetchAllByAdmin(creatorUserId))
        .thenReturn(Collections.singletonList(event));
    List<Event> results = eventService.getEventByCreator(creatorUserId);

    assertThat(results).isNotEmpty().hasSize(1);
    assertThat(results.get(0).getName()).isEqualTo("Test Hackathon");
    assertThat(results.get(0).getCreatedByUserId()).isEqualTo(creatorUserId);
    verify(eventRepository).fetchAllByAdmin(creatorUserId);
  }

  @Test
  void getEventByCreator_withInvalidCreatorId_returnsEmptyList() {
    UUID invalid = UUID.randomUUID();
    when(eventRepository.fetchAllByAdmin(invalid)).thenReturn(Collections.emptyList());
    List<Event> results = eventService.getEventByCreator(invalid);

    assertThat(results).isNotNull().isEmpty();
    verify(eventRepository).fetchAllByAdmin(invalid);
  }

  @Test
  void createEvent_withValidPayload_returnsSavedEvent() {
    EventRequest req = new EventRequest();
    req.setName("My new name");
    req.setVisibility("PUBLIC");
    req.setStatus("ACTIVE");
    req.setTagline("This is a test");
    req.setTeamSizeLimit((short) 4);
    req.setStartDateTime(OffsetDateTime.now().minusHours(1));
    req.setDuration(48 * 3600);
    req.setHackathonId(UUID.randomUUID());
    when(eventRepository.save(any(Event.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(hackathonRepository.existsById(any())).thenReturn(true);
    Event result = eventService.createEvent(req);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("My new name");
    assertThat(result.getVisibility()).isEqualTo("PUBLIC");
    assertThat(result.getStatus()).isEqualTo("ACTIVE");
    assertThat(result.getTagline()).isEqualTo("This is a test");
    assertThat(result.getCreatedByUserId()).isEqualTo(creatorUserId);

    verify(eventRepository).save(any(Event.class));
  }

  @Test
  void createEvent_withNullRequest_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> eventService.createEvent(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Event request body cant be null");

    verify(eventRepository, never()).save(any(Event.class));
  }

  @Test
  void createEvent_withInvalidName_throwsIllegalArgumentException() {
    EventRequest req = new EventRequest();
    req.setHackathonId(UUID.randomUUID());
    req.setName(null);
    req.setVisibility("PUBLIC");
    req.setStatus("ACTIVE");
    req.setDescription("This is a test");
    req.setTeamSizeLimit((short) 4);
    req.setStartDateTime(OffsetDateTime.parse("2026-06-01T09:00:00+02:00"));
    req.setDuration(48);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              eventService.createEvent(req);
            });

    assertThat(ex.getMessage()).isEqualTo("Name is required");
  }

  @Test
  void createEvent_withInvalidTeamSize_throwsIllegalArgumentException() {
    EventRequest req = new EventRequest();
    req.setHackathonId(UUID.randomUUID());
    req.setName("null");
    req.setVisibility("PUBLIC");
    req.setStatus("ACTIVE");
    req.setDescription("This is a test");
    req.setTeamSizeLimit((short) 0);
    req.setStartDateTime(OffsetDateTime.parse("2026-06-01T09:00:00+02:00"));
    req.setDuration(48);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              eventService.createEvent(req);
            });

    assertThat(ex.getMessage()).isEqualTo("Team size limit must be greater than 0");
  }

  @Test
  void createEvent_withInvalidStartDateTime_throwsIllegalArgumentException() {
    EventRequest req = new EventRequest();
    req.setHackathonId(UUID.randomUUID());
    req.setName("null");
    req.setVisibility("PUBLIC");
    req.setStatus("ACTIVE");
    req.setDescription("This is a test");
    req.setTeamSizeLimit((short) 10);
    req.setStartDateTime(null);
    req.setDuration(48);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              eventService.createEvent(req);
            });

    assertThat(ex.getMessage()).isEqualTo("Start date is required");
  }

  @Test
  void createEvent_withInvalidDuration_throwsIllegalArgumentException() {
    EventRequest req = new EventRequest();
    req.setHackathonId(UUID.randomUUID());
    req.setName("null");
    req.setVisibility("PUBLIC");
    req.setStatus("ACTIVE");
    req.setDescription("This is a test");
    req.setTeamSizeLimit((short) 10);
    req.setStartDateTime(OffsetDateTime.parse("2026-06-01T09:00:00+02:00"));
    req.setDuration(-50);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              eventService.createEvent(req);
            });

    assertThat(ex.getMessage()).isEqualTo("Duration is less than 0");
  }

  @Test
  void createEvent_withInvalidVisiblity_throwsIllegalArgumentException() {
    EventRequest req = new EventRequest();
    req.setHackathonId(UUID.randomUUID());
    req.setName("null");
    req.setVisibility(null);
    req.setStatus("ACTIVE");
    req.setDescription("This is a test");
    req.setTeamSizeLimit((short) 10);
    req.setStartDateTime(OffsetDateTime.parse("2026-06-01T09:00:00+02:00"));
    req.setDuration(50);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              eventService.createEvent(req);
            });

    assertThat(ex.getMessage()).isEqualTo("Visibility is required");
  }

  @Test
  void createEvent_withInvalidStatus_throwsIllegalArgumentException() {
    EventRequest req = new EventRequest();
    req.setHackathonId(UUID.randomUUID());
    req.setName("null");
    req.setVisibility("PUBLIC");
    req.setStatus("INVALID");
    req.setDescription("This is a test");
    req.setTeamSizeLimit((short) 10);
    req.setStartDateTime(OffsetDateTime.parse("2026-06-01T09:00:00+02:00"));
    req.setDuration(50);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              eventService.createEvent(req);
            });

    assertThat(ex.getMessage()).contains("Status must be one of these:");
  }

  @Test
  void createEvent_withInvalidRegistrationKey_throwsIllegalArgumentException() {
    EventRequest req = new EventRequest();
    req.setHackathonId(UUID.randomUUID());
    req.setName("null");
    req.setVisibility("PRIVATE");
    req.setStatus("ACTIVE");
    req.setDescription("This is a test");
    req.setTeamSizeLimit((short) 10);
    req.setStartDateTime(OffsetDateTime.parse("2026-06-01T09:00:00+02:00"));
    req.setDuration(50);
    req.setRegistrationKey(null);
    req.setHackathonId(UUID.randomUUID());

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              eventService.createEvent(req);
            });

    assertThat(ex.getMessage()).isEqualTo("Registration key is required for private events");
  }

  @Test
  void putUpdateEvent_withValidId_returnsUpdatedEvent() {
    EventRequest req = new EventRequest();
    req.setName("My new name");
    req.setVisibility("PRIVATE");
    req.setStatus("ACTIVE");
    req.setTagline("This is a test");
    req.setRegistrationKey("THISISAKEY");

    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRepository.save(any(Event.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Event result = eventService.putUpdateEvent(eventId, req);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("My new name");
    assertThat(result.getVisibility()).isEqualTo("PRIVATE");
    assertThat(result.getStatus()).isEqualTo("ACTIVE");
    assertThat(result.getTagline()).isEqualTo("This is a test");
    assertThat(result.getRegistrationKey()).isEqualTo("THISISAKEY");
    assertThat(result.getCreatedByUserId()).isEqualTo(creatorUserId);

    verify(eventRepository).findById(eventId);
    verify(eventRepository).save(any(Event.class));
  }

  @Test
  void putUpdateEvent_withInvalidId_throwsRuntimeException() {
    UUID invalidId = UUID.randomUUID();
    EventRequest req = new EventRequest();
    req.setName("Invalid");

    when(eventRepository.findById(invalidId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.putUpdateEvent(invalidId, req))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Event not found");

    verify(eventRepository).findById(invalidId);
    verify(eventRepository, never()).save(any(Event.class));
  }

  @Test
  void patchEventStatus_toPrivateWithKey_updatesAndSavesEvent() {
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRepository.save(any(Event.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Event updateEvent = eventService.patchEventStatus(eventId, "PRIVATE", "ACTIVE", "KEY");

    assertThat(updateEvent.getVisibility()).isEqualTo("PRIVATE");
    assertThat(updateEvent.getStatus()).isEqualTo("ACTIVE");
    assertThat(updateEvent.getRegistrationKey()).isEqualTo("KEY");
    verify(eventRepository).save(event);
  }

  @Test
  void patchEventStatus_toPublic_updatesAndSavesEvent() {
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRepository.save(any(Event.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Event updateEvent = eventService.patchEventStatus(eventId, "PUBLIC", "ACTIVE", null);

    assertThat(updateEvent.getVisibility()).isEqualTo("PUBLIC");
    assertThat(updateEvent.getStatus()).isEqualTo("ACTIVE");
    verify(eventRepository).save(event);
  }

  @Test
  void patchEventStatus_toPrivateWithoutKey_throwsRuntimeException() {
    event.setRegistrationKey("OLD_KEY");
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

    assertThatThrownBy(() -> eventService.patchEventStatus(eventId, "PRIVATE", "ACTIVE", null))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Registration key is required");

    verify(eventRepository, never()).save(any(Event.class));
  }

  @Test
  void patchEventStatus_withInvalidId_throwsNotFoundException() {
    UUID randomEventId = UUID.randomUUID();
    when(eventRepository.findById(randomEventId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.patchEventStatus(randomEventId, "PUBLIC", "ACTIVE", null))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Event not found");

    verify(eventRepository, never()).save(any(Event.class));
  }

  @Test
  void getEventStatus_withValidEventId_returnsStatusSummaryDto() {
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
    EventStatusResponse response = eventService.getEventStatus(eventId);

    assertThat(response).isNotNull();
    assertThat(response.getEventId()).isEqualTo(eventId);
    assertThat(response.getStatus()).isEqualTo("ACTIVE");
    assertThat(response.getVisibility()).isEqualTo("PUBLIC");

    verify(eventRepository).findById(eventId);
  }

  @Test
  void getEventStatus_withInvalidEventId_throwsRuntimeException() {
    UUID randomEventId = UUID.randomUUID();
    when(eventRepository.findById(randomEventId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.getEventStatus(randomEventId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Event not found");

    verify(eventRepository).findById(randomEventId);
  }

  @Test
  void getEventById_withValidId_returnsEvent() {
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

    Event result = eventService.getEventById(eventId);

    assertThat(result).isNotNull();
    assertThat(result.getEventId()).isEqualTo(eventId);
    assertThat(result.getName()).isEqualTo("Test Hackathon");
    verify(eventRepository).findById(eventId);
  }

  @Test
  void getEventById_withInvalidId_throwsRuntimeException() {
    UUID randomEventId = UUID.randomUUID();
    when(eventRepository.findById(randomEventId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.getEventById(randomEventId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Event not found");

    verify(eventRepository).findById(randomEventId);
  }

  @Test
  void updateEventBanner_withValidId_setsStorageKeyAndSaves() {
    String storageKey = "events/" + eventId + "/branding/banner/banner.png";
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRepository.save(any(Event.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Event result = eventService.updateEventBanner(eventId, storageKey);

    assertThat(result).isNotNull();
    assertThat(result.getBannerStorageKey()).isEqualTo(storageKey);
    verify(eventRepository).findById(eventId);
    verify(eventRepository).save(event);
  }

  @Test
  void updateEventBanner_withNullStorageKey_clearsBanner() {
    event.setBannerStorageKey("events/" + eventId + "/branding/banner/old.png");
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRepository.save(any(Event.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Event result = eventService.updateEventBanner(eventId, null);

    assertThat(result.getBannerStorageKey()).isNull();
    verify(eventRepository).save(event);
  }

  @Test
  void updateEventBanner_withInvalidId_throwsRuntimeException() {
    UUID randomEventId = UUID.randomUUID();
    when(eventRepository.findById(randomEventId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.updateEventBanner(randomEventId, "some-key"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("The event could not be found");

    verify(eventRepository, never()).save(any(Event.class));
  }

  @Test
  void updateEventLogo_withValidId_setsStorageKeyAndSaves() {
    String storageKey = "events/" + eventId + "/branding/logo/logo.png";
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRepository.save(any(Event.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Event result = eventService.updateEventLogo(eventId, storageKey);

    assertThat(result).isNotNull();
    assertThat(result.getLogoStorageKey()).isEqualTo(storageKey);
    verify(eventRepository).findById(eventId);
    verify(eventRepository).save(event);
  }

  @Test
  void updateEventLogo_withNullStorageKey_clearsLogo() {
    event.setLogoStorageKey("events/" + eventId + "/branding/logo/old.png");
    when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRepository.save(any(Event.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Event result = eventService.updateEventLogo(eventId, null);

    assertThat(result.getLogoStorageKey()).isNull();
    verify(eventRepository).save(event);
  }

  @Test
  void updateEventLogo_withInvalidId_throwsRuntimeException() {
    UUID randomEventId = UUID.randomUUID();
    when(eventRepository.findById(randomEventId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.updateEventLogo(randomEventId, "some-key"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("The event could not be found");

    verify(eventRepository, never()).save(any(Event.class));
  }
}
