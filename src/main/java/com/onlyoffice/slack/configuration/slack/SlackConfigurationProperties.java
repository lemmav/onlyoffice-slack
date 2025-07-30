package com.onlyoffice.slack.configuration.slack;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "slack.application")
public class SlackConfigurationProperties {
  private String clientId;
  private String clientSecret;
  private String signingSecret;
  private String scopes;
  private String userScopes;
  private String pathInstallation;
  private String pathRedirect;
  private String pathCompletion;
  private String pathCancellation;

  private String fileManagerShortcutId = "open_file_manager";
  private String openFileActionId = "open_file";
  private String submitSettingsActionId = "submit_settings";

  private String editorPathPattern = "%s/editor?session=%s";
}
