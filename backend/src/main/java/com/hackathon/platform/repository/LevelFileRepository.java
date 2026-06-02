package com.hackathon.platform.repository;

import com.hackathon.platform.model.LevelFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LevelFileRepository extends JpaRepository<LevelFile, Long> {

  Optional<LevelFile> findByLevelIdAndFileName(Long levelId, String fileName);

  List<LevelFile> findByLevelId(Long levelId);

  @Query("SELECT lf FROM LevelFile lf WHERE lf.levelId = :levelId AND lf.fileType = :fileType")
  List<LevelFile> findByLevelIdAndFileType(@Param("levelId") Long levelId, @Param("fileType") String fileType);

  boolean existsByStorageKey(String storageKey);

  void deleteByLevelId(Long levelId);
}