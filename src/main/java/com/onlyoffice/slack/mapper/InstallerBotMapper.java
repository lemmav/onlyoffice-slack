package com.onlyoffice.slack.mapper;

import com.onlyoffice.slack.persistence.entity.InstallerUser;
import com.slack.api.bolt.model.builtin.DefaultBot;
import org.springframework.stereotype.Component;

@Component
class InstallerBotMapper implements Mapper<InstallerUser, DefaultBot> {

  @Override
  public DefaultBot map(InstallerUser user) {
    if (user == null || user.getBot() == null || user.getBot().getBotAccessToken() == null) {
      return null;
    }

    var bot = new DefaultBot();
    bot.setAppId(user.getAppId());
    bot.setTeamId(user.getTeamId());
    bot.setBotId(user.getBot().getBotId());
    bot.setBotUserId(user.getBot().getBotUserId());
    bot.setBotAccessToken(user.getBot().getBotAccessToken());
    bot.setBotRefreshToken(user.getBot().getBotRefreshToken());
    bot.setBotTokenExpiresAt(user.getBot().getBotTokenExpiresAt());
    bot.setBotScope(user.getBot().getBotScope());
    bot.setTokenType(user.getTokenType());
    bot.setScope(user.getBot().getBotScope());
    bot.setInstalledAt(user.getInstalledAt());

    return bot;
  }
}
