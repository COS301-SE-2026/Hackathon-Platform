package com.hackathon.platform.repository;

import com.hackathon.platform.model.ForumComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, UUID> {
    List<ForumComment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID postId);
    long countByPostIdAndIsDeletedFalse(UUID postId);
}