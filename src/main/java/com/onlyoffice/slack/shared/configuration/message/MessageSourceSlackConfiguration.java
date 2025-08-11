package com.onlyoffice.slack.shared.configuration.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Data
@Builder
@Configuration
@NoArgsConstructor
@AllArgsConstructor
public class MessageSourceSlackConfiguration {
  private String errorSettingsTitle = "error.settings.title";
  private String errorSettingsButton = "error.settings.button";
  private String errorSettingsInvalidConfigurationText = "error.settings.invalid.text";
  private String errorSettingsIncompleteText = "error.settings.incomplete.text";
  private String errorSettingsDemoText = "error.settings.demo.text";

  private String errorSessionTitle = "error.session.title";
  private String errorSessionText = "error.session.text";
  private String errorSessionButton = "error.session.button";

  private String errorAvailableTitle = "error.available.title";
  private String errorAvailableText = "error.available.text";
  private String errorAvailableButton = "error.available.button";

  private String errorSlackApiTitle = "error.slack.api.title";
  private String errorSlackApiText = "error.slack.api.text";
  private String errorSlackApiButton = "error.slack.api.button";

  private String errorRateLimiterTitle = "error.ratelimiter.title";
  private String errorRateLimiterText = "error.ratelimiter.text";
  private String errorRateLimiterButton = "error.ratelimiter.button";

  private String errorGenericTitle = "error.generic.title";
  private String errorGenericText = "error.generic.text";
  private String errorGenericButton = "error.generic.button";

  private String errorResourceTitle = "error.resource.title";
  private String errorResourceText = "error.resource.text";
  private String errorResourceButton = "error.resource.button";

  private String messageCancellationTitle = "cancellation.title";
  private String messageCancellationText = "cancellation.text";
  private String messageCancellationButton = "cancellation.button";

  private String messageCompletionTitle = "completion.title";
  private String messageCompletionText = "completion.text";
  private String messageCompletionButton = "completion.button";

  private String messageHelpGreeting = "help.greeting";
  private String messageHelpInstructions = "help.instructions";
  private String messageHelpLearnMore = "help.learnMore";
  private String messageHelpLearnMoreButton = "help.learnMoreButton";
  private String messageHelpFeedback = "help.feedback";
  private String messageHelpFeedbackButton = "help.feedbackButton";

  private String messageHomeTitle = "home.title";
  private String messageHomeSettingsTitle = "home.settings.title";
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
