package com.onlyoffice.slack.shared.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "slack.application")
public class SlackConfigurationProperties {
  @NotBlank private String clientId;
  @NotBlank private String clientSecret;
  @NotBlank private String signingSecret;
  @NotBlank private String scopes;
  @NotBlank private String userScopes;
  @NotBlank private String pathInstallation;
  @NotBlank private String pathRedirect;
  @NotBlank private String pathCompletion;
  @NotBlank private String pathCancellation;

  private String fileManagerShortcutId = "open_file_manager";
  private String openFileActionId = "open_file";
  private String submitSettingsActionId = "submit_settings";
  private String readMoreActionId = "read_more";
  private String suggestFeatureActionId = "suggest_feature";
  private String getCloudActionId = "get_cloud";
  private String shareFeedbackActionId = "share_feedback";
  private String learnMoreActionId = "learn_more";

  private String downloadPathPattern = "%s/download/%s";
  private String editorPathPattern = "%s/editor?session=%s";

  private String welcomeReadMoreUrl = "https://github.com/ONLYOFFICE/onlyoffice-slack";
  private String welcomeSuggestFeatureUrl = "https://github.com/ONLYOFFICE/onlyoffice-slack/issues";
  private String getCloudUrl = "https://www.onlyoffice.com";
}
