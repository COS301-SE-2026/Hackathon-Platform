package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.platform.dto.EventRegistrationResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.EventRegistration;
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.repository.EventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceTest {
  @Mock private EventRepository eventRepo;
  @Mock private EventRegistrationRepository eventRegistrationRepo;
  @InjectMocks private EventRegistrationService eventRegistrationService;

  private UUID eventId;
  private UUID userId;
  private Event event;

  @BeforeEach
  void setUp() {
    eventId = UUID.randomUUID();
    userId = UUID.randomUUID();

    event = new Event();
    event.setEventId(eventId);
    event.setName("Test Hackathon");
    event.setStatus("ACTIVE");
    event.setVisibility("PUBLIC");
    event.setRegistrationKey(null);
    event.setInPerson(false);

  }

  private EventRegistration buildSavedRegistration() {
    
    EventRegistration reg = new EventRegistration();
    reg.setRegistrationId(UUID.randomUUID());
    reg.setEventId(eventId);
    reg.setUserId(userId);
    reg.setRegisteredAt(Instant.now());
    return reg;

  }

  @Test
  void registerForEvent_withValidPublicEvent_returnsRegistrationResponse() {

    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegistrationRepo.existsByEventIdAndUserId(eventId, userId)).thenReturn(false);
    when(eventRegistrationRepo.save(any(EventRegistration.class)))
        .thenReturn(buildSavedRegistration());
    
    EventRegistrationResponse response =
        eventRegistrationService.registerForEvent(eventId, userId, null);
    
    assertThat(response).isNotNull();
    assertThat(response.getEventId()).isEqualTo(eventId);
    verify(eventRegistrationRepo).save(any(EventRegistration.class));

  }

  @Test
  void registerForEvent_withNotInPersonEvent_ignoresDietaryAndAllergies() {

    event.setInPerson(false);
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegistrationRepo.existsByEventIdAndUserId(eventId, userId)).thenReturn(false);
    when(eventRegistrationRepo.save(any(EventRegistration.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    
    ArgumentCaptor<EventRegistration> captor = ArgumentCaptor.forClass(EventRegistration.class);

    eventRegistrationService.registerForEvent(
      eventId, userId, null, "Vegetarian", "Peanuts"
    );
    
    verify(eventRegistrationRepo).save(captor.capture());
    assertThat(captor.getValue().getDietaryReq()).isNull();
    assertThat(captor.getValue().getAllergies()).isNull();

  }

  @Test
  void registerForEvent_withInPersonEvent_savesDietaryAndAllergies() {

    event.setInPerson(true);
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegistrationRepo.existsByEventIdAndUserId(eventId, userId)).thenReturn(false);
    when(eventRegistrationRepo.save(any(EventRegistration.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ArgumentCaptor<EventRegistration> captor = ArgumentCaptor.forClass(EventRegistration.class);

    eventRegistrationService.registerForEvent(
      eventId, userId, null, " Vegetarian ", "Peanuts"
    );

    verify(eventRegistrationRepo).save(captor.capture());
    assertThat(captor.getValue().getDietaryReq()).isEqualTo("Vegetarian");
    assertThat(captor.getValue().getAllergies()).isEqualTo("Peanuts");


  }

  @Test
  void registerForEvent_withInPersonEventAndBlankDietaryReq_normalizesToNull() {

    event.setInPerson(true);
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegistrationRepo.existsByEventIdAndUserId(eventId, userId)).thenReturn(false);
    when(eventRegistrationRepo.save(any(EventRegistration.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    
    ArgumentCaptor<EventRegistration> captor = ArgumentCaptor.forClass(EventRegistration.class);

    eventRegistrationService.registerForEvent(eventId, userId, null, "  ", null);

    verify(eventRegistrationRepo).save(captor.capture());
    assertThat(captor.getValue().getDietaryReq()).isNull();

  }

  @Test
  void registerForEvent_withInvalidEventId_throwRuntimeException() {
    UUID randomEventId = UUID.randomUUID();
    when(eventRepo.findById(randomEventId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventRegistrationService.registerForEvent(randomEventId, userId, null))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Event not found");

    verify(eventRegistrationRepo, never()).save(any(EventRegistration.class));

  }

  @Test
  void registerForEvent_withCompletedEvent_throwsRuntimeException() {
    event.setStatus("COMPLETED");
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));

    assertThatThrownBy(() -> eventRegistrationService.registerForEvent(eventId, userId, null))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("This event is not accepting registrations");

    verify(eventRegistrationRepo, never()).save(any(EventRegistration.class));

  }

  @Test
  void registerForEvent_withCanceledEvent_throwsRuntimeException() {
    event.setStatus("CANCELED");
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));

    assertThatThrownBy(() -> eventRegistrationService.registerForEvent(eventId, userId, null))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("This event is not accepting registrations");

    verify(eventRegistrationRepo, never()).save(any(EventRegistration.class));

  }

  @Test
  void registerForEvent_withPrivateEventAndCorrectKey_registersSuccessfully() {

    event.setVisibility("PRIVATE");
    event.setRegistrationKey("SECRETKEY");

    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegistrationRepo.existsByEventIdAndUserId(eventId, userId)).thenReturn(false);
    when(eventRegistrationRepo.save(any(EventRegistration.class)))
        .thenReturn(buildSavedRegistration());

    EventRegistrationResponse response =
        eventRegistrationService.registerForEvent(eventId, userId, "SECRETKEY");

    assertThat(response).isNotNull();
    verify(eventRegistrationRepo).save(any(EventRegistration.class));

  }

  @Test
  void registerForEvent_withPrivateEventAndWrongKey_throwsRuntimeException() {
    event.setVisibility("PRIVATE");
    event.setRegistrationKey("SECRETKEY");
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));

    assertThatThrownBy(
      () -> eventRegistrationService.registerForEvent(eventId, userId, "WRONGKEY")

    )
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Registration key is not correct");

    verify(eventRegistrationRepo, never()).save(any(EventRegistration.class));

  }

  @Test
  void registerForEvent_withPrivateEventAndNullKey_throwsRuntimeException() {
    event.setVisibility("PRIVATE");
    event.setRegistrationKey("SECRETKEY");
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));

    assertThatThrownBy(
      () -> eventRegistrationService.registerForEvent(eventId, userId, null)

    )
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Registration key is not correct");

    verify(eventRegistrationRepo, never()).save(any(EventRegistration.class));

  }

  @Test
  void registerForEvent_withPrivateEventAndBlankKey_throwsRuntimeException() {
    event.setVisibility("PRIVATE");
    event.setRegistrationKey("SECRETKEY");
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));

    assertThatThrownBy(
      () -> eventRegistrationService.registerForEvent(eventId, userId, "  ")

    )
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Registration key is not correct");

    verify(eventRegistrationRepo, never()).save(any(EventRegistration.class));

  }

  @Test
  void registerForEvent_whenAlreadyRegistered_throwsRuntimeException() {
    
    when(eventRepo.findById(eventId)).thenReturn(Optional.of(event));
    when(eventRegistrationRepo.existsByEventIdAndUserId(eventId, userId)).thenReturn(true);

    assertThatThrownBy(() -> eventRegistrationService.registerForEvent(eventId, userId, null))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("You're already registered for this event");

    verify(eventRegistrationRepo, never()).save(any(EventRegistration.class));

  }

  @Test
  void isRegistered_whenRegistrationExists_returnsTrue() {
    when(eventRegistrationRepo.existsByEventIdAndUserId(eventId, userId)).thenReturn(true);

    boolean result = eventRegistrationService.isRegistered(eventId, userId);

    assertThat(result).isTrue();
    verify(eventRegistrationRepo).existsByEventIdAndUserId(eventId, userId);

  }

  @Test
  void isRegistered_whenRegistrationExists_returnsFalse() {
    when(eventRegistrationRepo.existsByEventIdAndUserId(eventId, userId)).thenReturn(false);

    boolean result = eventRegistrationService.isRegistered(eventId, userId);

    assertThat(result).isFalse();
    verify(eventRegistrationRepo).existsByEventIdAndUserId(eventId, userId);

  }

  @Test
  void getMyRegistrations_returnsMappedResponses() {
    EventRegistration reg = buildSavedRegistration();
    when(eventRegistrationRepo.findByUserId(userId)).thenReturn(List.of(reg));

    List<EventRegistrationResponse> results = eventRegistrationService.getMyRegistrations(userId);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getRegId()).isEqualTo(reg.getRegistrationId());
    assertThat(results.get(0).getEventId()).isEqualTo(eventId);
    verify(eventRegistrationRepo).findByUserId(userId);

  }

  @Test
  void getMyRegistrations_withNoRegistrations_returnsEmptyList() {
    when(eventRegistrationRepo.findByUserId(userId)).thenReturn(List.of());
 
    List<EventRegistrationResponse> results = eventRegistrationService.getMyRegistrations(userId);
 
    assertThat(results).isEmpty();

    verify(eventRegistrationRepo).findByUserId(userId);

  }





}