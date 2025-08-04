package com.onlyoffice.slack.domain.slack.installation;

import com.onlyoffice.slack.shared.persistence.entity.InstallerUser;
import com.onlyoffice.slack.shared.utils.Mapper;
import com.slack.api.bolt.model.builtin.DefaultInstaller;
import org.springframework.stereotype.Component;

@Component
class InstallerMapper implements Mapper<InstallerUser, DefaultInstaller> {

  @Override
  public DefaultInstaller map(InstallerUser user) {
    if (user == null
        || (user.getInstallerUserAccessToken() == null
            && (user.getBot() == null || user.getBot().getBotAccessToken() == null))) {
      return null;
    }

    var builder =
        DefaultInstaller.builder()
            .appId(user.getAppId())
            .teamId(user.getTeamId())
            .installerUserId(user.getInstallerUserId())
            .installerUserScope(user.getInstallerUserScope())
            .installerUserAccessToken(user.getInstallerUserAccessToken())
            .installerUserRefreshToken(user.getInstallerUserRefreshToken())
            .installerUserTokenExpiresAt(user.getInstallerUserTokenExpiresAt())
            .tokenType(user.getTokenType())
            .installedAt(user.getInstalledAt());

    if (user.getBot() != null) {
      builder
          .botId(user.getBot().getBotId())
          .botUserId(user.getBot().getBotUserId())
          .botAccessToken(user.getBot().getBotAccessToken())
          .botRefreshToken(user.getBot().getBotRefreshToken())
          .botTokenExpiresAt(user.getBot().getBotTokenExpiresAt())
          .botScope(user.getBot().getBotScope());
    }

    return builder.build();
  }
}
