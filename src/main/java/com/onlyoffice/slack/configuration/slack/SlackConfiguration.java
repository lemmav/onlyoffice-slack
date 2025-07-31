package com.onlyoffice.slack.configuration.slack;

import com.onlyoffice.slack.registry.SlackBlockActionRegistry;
import com.onlyoffice.slack.registry.SlackSlashCommandHandlerRegistry;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.handler.builtin.MessageShortcutHandler;
import com.slack.api.bolt.service.InstallationService;
import com.slack.api.bolt.service.OAuthStateService;
import com.slack.api.bolt.service.builtin.oauth.view.OAuthInstallPageRenderer;
import com.slack.api.model.event.AppHomeOpenedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Setter
@Getter
@Configuration
@RequiredArgsConstructor
public class SlackConfiguration {
  private final SlackConfigurationProperties properties;
  private final SlackBlockActionRegistry blockActionRegistry;
  private final SlackSlashCommandHandlerRegistry commandHandlerRegistry;

  private final BoltEventHandler<AppHomeOpenedEvent> appHomeOpenedEventBoltEventHandler;
  private final MessageShortcutHandler slackMessageShortcutHandler;

  @Bean
  public AppConfig slackBoltApplicationConfiguration(
      final OAuthInstallPageRenderer installPageRenderer) {
    return AppConfig.builder()
        .clientId(properties.getClientId())
        .clientSecret(properties.getClientSecret())
        .signingSecret(properties.getSigningSecret())
        .scope(properties.getScopes())
        .userScope(properties.getUserScopes())
        .oauthInstallPath(properties.getPathInstallation())
        .oAuthInstallPageRenderer(installPageRenderer)
        .oauthRedirectUriPath(properties.getPathRedirect())
        .oauthCompletionUrl(properties.getPathCompletion())
        .oauthCancellationUrl(properties.getPathCancellation())
        .alwaysRequestUserTokenNeeded(true)
        .build();
  }

  @Bean
  public App slackBoltApplication(
      final AppConfig slackBoltApplicationConfiguration,
      final InstallationService onlyofficeInstallationService,
      final OAuthStateService stateService) {
    var app =
        new App(slackBoltApplicationConfiguration)
            .service(onlyofficeInstallationService)
            .service(stateService)
            .asOAuthApp(true)
            .enableTokenRevocationHandlers();

    app.event(AppHomeOpenedEvent.class, appHomeOpenedEventBoltEventHandler);
    app.messageShortcut(properties.getFileManagerShortcutId(), slackMessageShortcutHandler);
    blockActionRegistry.getRegistry().forEach(app::blockAction);
    commandHandlerRegistry.getRegistry().forEach(app::command);

    return app;
  }
}
