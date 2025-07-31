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
  private String readMoreActionId = "read_more";
  private String suggestFeatureActionId = "suggest_feature";
  private String getCloudActionId = "get_cloud";
  private String shareFeedbackActionId = "share_feedback";
  private String learnMoreActionId = "learn_more";

  private String editorPathPattern = "%s/editor?session=%s";

  private String welcomeReadMoreUrl = "https://github.com/ONLYOFFICE/onlyoffice-slack";
  private String welcomeSuggestFeatureUrl = "https://github.com/ONLYOFFICE/onlyoffice-slack/issues";
  private String getCloudUrl = "https://www.onlyoffice.com";
}
