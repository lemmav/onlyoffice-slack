package com.onlyoffice.slack.persistence.entity;

import com.onlyoffice.slack.persistence.converter.EncryptionAttributeConverter;
import com.onlyoffice.slack.persistence.entity.id.InstallerUserId;
import jakarta.persistence.*;
import java.io.Serializable;
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
    name = "installer_users",
    indexes = {@Index(name = "idx_team_user", columnList = "team_id,installer_user_id")})
@IdClass(InstallerUserId.class)
public class InstallerUser implements Serializable {
  @Id
  @Column(name = "team_id", nullable = false)
  private String teamId;

  @Id
  @Column(name = "installer_user_id", nullable = false)
  private String installerUserId;

  @Column(name = "app_id", nullable = false)
  private String appId;

  @Column(name = "token_type", nullable = false)
  private String tokenType;

  @Column(name = "installer_user_scope")
  private String installerUserScope;

  @Column(name = "installer_user_access_token", length = 1000)
  @Convert(converter = EncryptionAttributeConverter.class)
  private String installerUserAccessToken;

  @Column(name = "installer_user_refresh_token", length = 1000)
  @Convert(converter = EncryptionAttributeConverter.class)
  private String installerUserRefreshToken;

  @Column(name = "installer_user_token_expires_at")
  private Long installerUserTokenExpiresAt;

  @Column(name = "installed_at", nullable = false)
  private Long installedAt;

  @Column(name = "bot_id")
  private String botId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "team_id",
        referencedColumnName = "team_id",
        insertable = false,
        updatable = false),
    @JoinColumn(
        name = "bot_id",
        referencedColumnName = "bot_id",
        insertable = false,
        updatable = false)
  })
  private BotUser bot;

  public void setBot(BotUser bot) {
    if (this.bot != null) this.bot.getUsers().remove(this);
    this.bot = bot;
    if (bot != null) {
      this.botId = bot.getBotId();
      bot.getUsers().add(this);
    } else {
      this.botId = null;
    }
  }
}
