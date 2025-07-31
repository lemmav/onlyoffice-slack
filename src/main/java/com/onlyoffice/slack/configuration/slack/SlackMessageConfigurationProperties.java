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
@ConfigurationProperties(prefix = "slack.application.messages")
public class SlackMessageConfigurationProperties {
  private String messageCancellationTitle = "cancellation.title";
  private String messageCancellationText = "cancellation.text";
  private String messageCancellationDescription = "cancellation.description";
  private String messageCancellationButton = "cancellation.button";

  private String messageCompletionTitle = "completion.title";
  private String messageCompletionText = "completion.text";
  private String messageCompletionDescription = "completion.description";
  private String messageCompletionButton = "completion.button";

  private String messageHelpGreeting = "help.greeting";
  private String messageHelpInstructions = "help.instructions";
  private String messageHelpLearnMore = "help.learnMore";
  private String messageHelpLearnMoreButton = "help.learnMoreButton";
  private String messageHelpFeedback = "help.feedback";
  private String messageHelpFeedbackButton = "help.feedbackButton";

  private String messageHomeTitle = "home.title";
  private String messageHomeSettingsTitle = "home.settings.title";
  private String messageHomeDemoActive = "home.demo.active";
  private String messageHomeInputHttpsAddressLabel = "home.input.httpsAddress.label";
  private String messageHomeInputHttpsAddressPlaceholder = "home.input.httpsAddress.placeholder";
  private String messageHomeInputSecretLabel = "home.input.secret.label";
  private String messageHomeInputHeaderLabel = "home.input.header.label";
  private String messageHomeInputDemoSettingsLabel = "home.input.demoSettings.label";
  private String messageHomeCheckboxEnableDemo = "home.checkbox.enableDemo";
  private String messageHomeButtonSaveSettings = "home.button.saveSettings";
  private String messageHomeErrorRenderView = "home.error.renderView";
  private String messageHomeWelcomeTitle = "home.welcome.title";
  private String messageHomeWelcomeDescription = "home.welcome.description";
  private String messageHomeReadMore = "home.readMore";
  private String messageHomeSuggestFeature = "home.suggestFeature";
  private String messageHomeReadMoreEmoji = "home.readMore.emoji";
  private String messageHomeSuggestFeatureEmoji = "home.suggestFeature.emoji";
  private String messageHomeCloudTitle = "home.cloud.title";
  private String messageHomeCloudDescription = "home.cloud.description";
  private String messageHomeCloudButton = "home.cloud.button";
  private String messageHomeSecretHelp = "home.secret.help";
  private String messageHomeHeaderHelp = "home.header.help";
  private String messageHomeDemoHelp = "home.demo.help";

  private String messageInstallButton = "install.button";
  private String messageInstallTitle = "install.title";
  private String messageInstallSubtitle = "install.subtitle";
  private String messageInstallFeatureEdit = "install.feature.edit";
  private String messageInstallFeatureCollaboration = "install.feature.collaboration";
  private String messageInstallFeatureFileSupport = "install.feature.fileSupport";
  private String messageInstallFeatureSecureEditing = "install.feature.secureEditing";

  private String messageLoadingTitle = "loading.title";
  private String messageLoadingDescription = "loading.description";
  private String messageLoadingError = "loading.error";
  private String messageLoadingRetry = "loading.retry";
  private String messageLoadingCancel = "loading.cancel";

  private String messageManagerModalTitle = "manager.modal.title";
  private String messageManagerModalHeader = "manager.modal.header";
  private String messageManagerModalFileInfo = "manager.modal.fileInfo";
  private String messageManagerModalFileStatusPublic = "manager.modal.fileStatus.public";
  private String messageManagerModalFileStatusPrivate = "manager.modal.fileStatus.private";
  private String messageManagerModalNoFilesFound = "manager.modal.noFiles";
  private String messageManagerModalOpenButton = "manager.modal.button.open";
  private String messageManagerModalCloseButton = "manager.modal.button.close";
}
