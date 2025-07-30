package com.onlyoffice.slack.service.data;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.configuration.slack.SlackConfigurationProperties;
import com.onlyoffice.slack.persistence.entity.BotUser;
import com.onlyoffice.slack.persistence.entity.InstallerUser;
import com.onlyoffice.slack.persistence.entity.id.BotUserId;
import com.onlyoffice.slack.persistence.entity.id.InstallerUserId;
import com.onlyoffice.slack.persistence.repository.BotUserRepository;
import com.onlyoffice.slack.persistence.repository.InstallerUserRepository;
import com.onlyoffice.slack.util.SafeOptional;
import com.slack.api.Slack;
import com.slack.api.bolt.model.Bot;
import com.slack.api.bolt.model.Installer;
import com.slack.api.bolt.model.builtin.DefaultBot;
import com.slack.api.bolt.model.builtin.DefaultInstaller;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstallationService implements RotatingInstallationService {
  private final Slack slack = Slack.getInstance();

  private final IMap<InstallerUserId, DefaultInstaller> userCache;
  private final SlackConfigurationProperties slackProperties;
  private final PlatformTransactionManager transactionManager;
  private final InstallerUserRepository userRepository;
  private final IMap<String, DefaultBot> botCache;
  private final BotUserRepository botRepository;

  private boolean historicalDataEnabled = true;

  @Override
  public boolean isHistoricalDataEnabled() {
    return historicalDataEnabled;
  }

  @Override
  public void setHistoricalDataEnabled(boolean enabled) {
    this.historicalDataEnabled = enabled;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void saveInstallerAndBot(Installer installer) {
    var slackBot = installer.toBot();
    try {
      MDC.put("team_id", installer.getTeamId());
      MDC.put("installer_user_id", installer.getInstallerUserId());

      if (slackBot != null) {
        MDC.put("bot_id", slackBot.getBotId());
        log.info("Saving installer and bot for current team");

        var bot =
            BotUser.builder()
                .teamId(installer.getTeamId())
                .botId(slackBot.getBotId())
                .botUserId(slackBot.getBotUserId())
                .botAccessToken(slackBot.getBotAccessToken())
                .botRefreshToken(slackBot.getBotRefreshToken())
                .botTokenExpiresAt(slackBot.getBotTokenExpiresAt())
                .botScope(slackBot.getBotScope())
                .build();

        botRepository.save(bot);

        var user =
            InstallerUser.builder()
                .teamId(installer.getTeamId())
                .installerUserId(installer.getInstallerUserId())
                .appId(installer.getAppId())
                .tokenType(installer.getTokenType())
                .installerUserScope(installer.getInstallerUserScope())
                .installerUserAccessToken(installer.getInstallerUserAccessToken())
                .installerUserRefreshToken(installer.getInstallerUserRefreshToken())
                .installerUserTokenExpiresAt(installer.getInstallerUserTokenExpiresAt())
                .installedAt(installer.getInstalledAt())
                .botId(slackBot.getBotId())
                .build();

        user.setBot(bot);

        userRepository.save(user);
        log.info("Installer and bot saved successfully");
      }
    } finally {
      MDC.clear();
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteBot(Bot bot) {
    try {
      MDC.put("team_id", bot.getTeamId());
      MDC.put("bot_id", bot.getBotId());
      MDC.put("bot_user_id", bot.getBotUserId());

      var botId = bot.getBotId();
      var botUserId = bot.getBotUserId();
      var users = Optional.<java.util.List<InstallerUser>>empty();

      if (botId != null && !botId.isBlank()) {
        users = Optional.of(userRepository.findByBotId(botId));
      } else if (botUserId != null && !botUserId.isBlank()) {
        var user = userRepository.findByBotUserId(botUserId);
        users = user.map(java.util.List::of);
      }

      if (users.isPresent() && !users.get().isEmpty()) {
        log.info("Deleting installer users for bot");

        botCache.delete(bot.getTeamId());
        userCache.clear();
        botRepository.deleteById(new BotUserId(bot.getTeamId(), botId));

        log.info("Bot and associated installer users deleted successfully");
      }
    } finally {
      MDC.clear();
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteInstaller(Installer installer) {
    try {
      MDC.put("team_id", installer.getTeamId());
      MDC.put("installer_user_id", installer.getInstallerUserId());

      userRepository
          .findByTeamIdAndInstallerUserId(installer.getTeamId(), installer.getInstallerUserId())
          .ifPresentOrElse(
              user -> {
                log.info("Deleting installer user for team");

                var id = new InstallerUserId(installer.getTeamId(), installer.getInstallerUserId());
                userCache.remove(id);
                userRepository.delete(user);

                log.info("Installer user deleted successfully");
              },
              () -> log.warn("Installer user not found for team and user"));
    } finally {
      MDC.clear();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Bot findBot(String enterpriseId, String teamId) {
    try {
      MDC.put("team_id", teamId);
      log.info("Finding bot for team");

      return botCache.getOrDefault(
          teamId,
          userRepository.findFirstByTeamId(teamId).map(this::convertUserToBot).orElse(null));
    } finally {
      MDC.clear();
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void saveBot(Bot bot) throws Exception {
    try {
      MDC.put("team_id", bot.getTeamId());
      MDC.put("bot_id", bot.getBotId());

      log.info("Saving bot for team");

      var botEntity =
          BotUser.builder()
              .teamId(bot.getTeamId())
              .botId(bot.getBotId())
              .botUserId(bot.getBotUserId())
              .botAccessToken(bot.getBotAccessToken())
              .botRefreshToken(bot.getBotRefreshToken())
              .botTokenExpiresAt(bot.getBotTokenExpiresAt())
              .botScope(bot.getBotScope())
              .build();

      botRepository.save(botEntity);

      log.info("Bot saved successfully");
    } finally {
      MDC.clear();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Installer findInstaller(String enterpriseId, String teamId, String userId) {
    try {
      MDC.put("team_id", teamId);
      MDC.put("installer_user_id", userId);
      log.info("Finding installer for team");

      var id = new InstallerUserId(teamId, userId);
      return userCache.getOrDefault(
          id,
          userRepository
              .findByTeamIdAndInstallerUserId(teamId, userId)
              .map(this::convertUserToInstaller)
              .orElse(null));
    } finally {
      MDC.clear();
    }
  }

  @Override
  public Installer findInstallerWithRotation(String enterpriseId, String teamId, String userId) {
    try {
      MDC.put("team_id", teamId);
      MDC.put("installer_user_id", userId);
      log.info("Finding installer with rotation for team");

      var maybeUser = userRepository.findByTeamIdAndInstallerUserId(teamId, userId);

      if (maybeUser.isPresent()
          && maybeUser.get().getInstallerUserTokenExpiresAt()
              < Instant.now().plus(10, ChronoUnit.MINUTES).toEpochMilli()) {
        var user = maybeUser.get();
        var maybeToken =
            SafeOptional.of(
                () ->
                    slack
                        .methods()
                        .oauthV2Access(
                            OAuthV2AccessRequest.builder()
                                .grantType("refresh_token")
                                .clientId(slackProperties.getClientId())
                                .clientSecret(slackProperties.getClientSecret())
                                .refreshToken(user.getInstallerUserRefreshToken())
                                .build()));
        if (maybeToken.isPresent()) {
          userCache.remove(new InstallerUserId(teamId, userId));
          var token = maybeToken.get();
          var transactionTemplate = new TransactionTemplate(transactionManager);
          transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
          transactionTemplate.setTimeout(2);
          transactionTemplate.execute(
              status -> {
                user.setInstallerUserAccessToken(token.getAccessToken());
                user.setInstallerUserRefreshToken(token.getRefreshToken());
                user.setInstallerUserTokenExpiresAt(
                    Instant.now().plus(token.getExpiresIn(), ChronoUnit.SECONDS).toEpochMilli());
                userRepository.save(user);

                log.info("Installer user token refreshed and saved for team");
                return status;
              });
        }
      }

      return maybeUser.map(this::convertUserToInstaller).orElse(null);
    } finally {
      MDC.clear();
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteAll(String enterpriseId, String teamId) {
    try {
      MDC.put("team_id", teamId);
      log.info("Deleting all bots for team");

      botCache.clear();
      userCache.clear();
      botRepository.deleteByTeamId(teamId);

      log.info("All bots deleted for team");
    } finally {
      MDC.clear();
    }
  }

  private DefaultBot convertUserToBot(InstallerUser user) {
    if (user.getBot() == null || user.getBot().getBotAccessToken() == null) return null;

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

  private DefaultInstaller convertUserToInstaller(InstallerUser user) {
    if (user.getInstallerUserAccessToken() == null
        && (user.getBot() == null || user.getBot().getBotAccessToken() == null)) return null;

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
