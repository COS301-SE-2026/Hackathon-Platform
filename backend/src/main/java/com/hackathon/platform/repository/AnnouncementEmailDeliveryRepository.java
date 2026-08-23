package com.hackathon.platform.repository;

import com.hackathon.platform.model.AnnouncementEmailDelivery;
import com.hackathon.platform.model.AnnouncementEmailDeliveryId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementEmailDeliveryRepository
    extends JpaRepository<AnnouncementEmailDelivery, AnnouncementEmailDeliveryId> {
  List<AnnouncementEmailDelivery> findByMessageId(UUID messageId);

  List<AnnouncementEmailDelivery> findByMessageIdAndDeliveryStatus(
      UUID messageId, String deliveryStatus);

  List<AnnouncementEmailDelivery> findByDeliveryStatusAndAttemptCountLessThan(
      String deliveryStatus, int attemptCount);
}
