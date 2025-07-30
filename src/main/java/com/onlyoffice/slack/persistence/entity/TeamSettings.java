package com.onlyoffice.slack.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "team_settings")
public class TeamSettings {
  @Id
  @Column(name = "team_id", nullable = false)
  private String teamId;

  @Column(name = "address")
  private String address;

  @Column(name = "header")
  private String header;

  @Column(name = "secret", length = 1000)
  private String secret;

  @Builder.Default
  @Column(name = "demo_enabled", nullable = false)
  private Boolean demoEnabled = false;

  @Column(name = "demo_started_date")
  private LocalDateTime demoStartedDate;
}
