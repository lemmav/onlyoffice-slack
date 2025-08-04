package com.onlyoffice.slack.shared.persistence.entity;

import com.onlyoffice.slack.shared.persistence.converter.EncryptionAttributeConverter;
import com.onlyoffice.slack.shared.persistence.entity.id.BotUserId;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "bot_users",
    indexes = {
      @Index(name = "idx_team_bot", columnList = "team_id,bot_id"),
      @Index(name = "idx_team_bot_user", columnList = "team_id,bot_user_id")
    })
@IdClass(BotUserId.class)
public class BotUser {
  @Id
  @Column(name = "team_id", nullable = false)
  private String teamId;

  @Id
  @Column(name = "bot_id", nullable = false)
  private String botId;

  @Column(name = "bot_user_id")
  private String botUserId;

  @Column(name = "bot_access_token", length = 1000)
  @Convert(converter = EncryptionAttributeConverter.class)
  private String botAccessToken;

  @Column(name = "bot_refresh_token", length = 1000)
  @Convert(converter = EncryptionAttributeConverter.class)
  private String botRefreshToken;

  @Column(name = "bot_token_expires_at")
  private Long botTokenExpiresAt;

  @Column(name = "bot_scope")
  private String botScope;

  @Builder.Default
  @OneToMany(mappedBy = "bot", fetch = FetchType.LAZY)
  private Set<InstallerUser> users = new HashSet<>();

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "team_id",
      referencedColumnName = "team_id",
      insertable = false,
      updatable = false)
  private TeamSettings teamSettings;
}
