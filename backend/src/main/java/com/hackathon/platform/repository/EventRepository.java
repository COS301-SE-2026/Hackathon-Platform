package com.hackathon.platform.repository;

import com.hackathon.platform.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

}