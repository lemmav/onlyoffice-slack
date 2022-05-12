package com.onlyoffice.slack.configuration.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.service.InstallationService;
import com.slack.api.bolt.service.builtin.oauth.view.OAuthInstallPageRenderer;
import com.slack.api.model.event.AppUninstalledEvent;
import com.slack.api.model.event.TokensRevokedEvent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@ConfigurationProperties(prefix = "slack.application")
@Setter
@Getter
@EnableRetry
public class SlackConfiguration {
    private String clientID;
    private String clientSecret;
    private String clientSecretSigning;
    private String scopes;
    private String userScopes;
    private String pathInstallation;
    private String pathRedirect;
    private String pathCompletion;
    private String pathCancellation;

    @Bean
    public App initializeSlackApplication(
            InstallationService onlyofficeInstallationService,
            OAuthInstallPageRenderer installPageRenderer
    ) {
        App app = new App(AppConfig
                .builder()
                .clientId(clientID)
                .clientSecret(clientSecret)
                .signingSecret(clientSecretSigning)
                .scope(scopes)
                .userScope(userScopes)
                .oauthInstallPath(pathInstallation)
                .oAuthInstallPageRenderer(installPageRenderer)
                .oauthRedirectUriPath(pathRedirect)
                .oauthCompletionUrl(pathCompletion)
                .oauthCancellationUrl(pathCancellation)
                .build())
                .service(onlyofficeInstallationService)
                .asOAuthApp(true);
        app.event(TokensRevokedEvent.class, app.defaultTokensRevokedEventHandler());
        app.event(AppUninstalledEvent.class, app.defaultAppUninstalledEventHandler());
        return app;
    }
}
