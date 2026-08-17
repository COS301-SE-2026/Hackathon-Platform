package com.hackathon.platform.repository;

import com.hackathon.platform.model.CommunicationMessage;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunicationMessageRepository extends JpaRepository<CommunicationMessage, UUID> {
    List<CommunicationMessage> findTop100ByChannelIdOrderByCreatedAtDesc(UUID channelId);
}