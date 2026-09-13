package com.hackathon.platform.repository;

import com.hackathon.platform.model.PlagiarismRun;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlagiarismRunRepository extends JpaRepository<PlagiarismRun, Long> {

  List<PlagiarismRun> findByEventIdOrderByRequestedAtDesc(UUID eventId);
  
}
