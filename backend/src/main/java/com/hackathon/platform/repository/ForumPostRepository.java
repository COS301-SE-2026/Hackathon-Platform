package com.hackathon.platform.repository;

import com.hackathon.platform.model.ForumPost;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, UUID> {
  List<ForumPost> findByEventIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID eventId);

  Optional<ForumPost> findByPostIdAndEventIdAndIsDeletedFalse(UUID postId, UUID eventId);
}
