package com.onlyoffice.slack.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "active_file_sessions")
public class ActiveFileSession {
  @Id
  @Column(name = "file_id", nullable = false)
  private String fileId;

  @Column(nullable = false)
  private String key;

  @Column(name = "channel_id", nullable = false)
  private String channelId;

  @Column(name = "message_ts", nullable = false)
  private String messageTs;

  @CreatedDate private LocalDateTime createdAt;
}
