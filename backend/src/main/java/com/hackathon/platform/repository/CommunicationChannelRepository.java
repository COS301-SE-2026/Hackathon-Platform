package com.hackathon.platform.repository;

import com.hackathon.platform.model.CommunicationChannel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunicationChannelRepository extends JpaRepository<CommunicationChannel, UUID> {
    Optional<CommunicationChannel> findByChannelTypeAndEventId(String channelType, UUID eventId);
}