package com.hackathon.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class AnnouncementUpdateServiceTest {
  private AnnouncementUpdateService upSer;
  private UUID eventId;
  private UUID messageId;
  private Map<UUID, CopyOnWriteArrayList<SseEmitter>> subs;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    upSer = new AnnouncementUpdateService();
    eventId = UUID.randomUUID();
    messageId = UUID.randomUUID();

    subs =
        (Map<UUID, CopyOnWriteArrayList<SseEmitter>>)
            ReflectionTestUtils.getField(upSer, "eventSubscribers");
  }

  @Test
  void subscribe_returnsEmitter() {
    SseEmitter em = upSer.subscribe(eventId);
    assertThat(em).isNotNull();
    assertThat(em.getTimeout()).isEqualTo(30L * 60L * 1000L);
  }

  @Test
  void pushAnnouncementUpdate_sendsUpdate() throws Exception {
    SseEmitter em = mock(SseEmitter.class);
    subs.put(eventId, new CopyOnWriteArrayList<>(List.of(em)));
    upSer.pushAnnouncementUpdate(eventId, messageId);

    verify(em).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  void pushAnnouncementUpdate_noSubscribers_doesNothing() {
    upSer.pushAnnouncementUpdate(eventId, messageId);
    assertThat(subs).isEmpty();
  }

  @Test
  void pushAnnouncementUpdate_sendFails_removesEmitter() throws Exception {
    SseEmitter em = mock(SseEmitter.class);
    subs.put(eventId, new CopyOnWriteArrayList<>(List.of(em)));

    doThrow(new IOException()).when(em).send(any(SseEmitter.SseEventBuilder.class));
    upSer.pushAnnouncementUpdate(eventId, messageId);

    assertThat(subs).doesNotContainKey(eventId);
  }
}
