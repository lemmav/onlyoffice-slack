package com.onlyoffice.slack.domain.document.session;

import com.onlyoffice.slack.domain.document.session.entity.ActiveFileSession;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentFileSessionRepository extends JpaRepository<ActiveFileSession, String> {
  @Query(
      value =
          "SELECT s.file_id FROM active_file_sessions s WHERE s.created_at < :cutoffDate ORDER BY s.created_at ASC LIMIT 50",
      nativeQuery = true)
  Set<String> findLastStaleRecords(@Param("cutoffDate") LocalDateTime cutoffDate);
}
