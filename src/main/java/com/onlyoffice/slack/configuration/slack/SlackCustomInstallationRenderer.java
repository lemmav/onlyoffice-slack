package com.onlyoffice.slack.configuration.slack;

import com.slack.api.bolt.service.builtin.oauth.view.OAuthInstallPageRenderer;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Configuration
@RequiredArgsConstructor
public class SlackCustomInstallationRenderer implements OAuthInstallPageRenderer {
  private final SlackMessageConfigurationProperties slackMessageConfigurationProperties;

  private final SpringTemplateEngine engine;
  private final MessageSource messageSource;

  public String render(final String installationUrl) {
    var ctx = new Context();
    ctx.setVariable(
        "title",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageInstallTitle(), null, Locale.ENGLISH));
    ctx.setVariable(
        "subtitle",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageInstallSubtitle(), null, Locale.ENGLISH));
    ctx.setVariable(
        "button",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageInstallButton(), null, Locale.ENGLISH));
    ctx.setVariable(
        "featureEdit",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageInstallFeatureEdit(),
            null,
            Locale.ENGLISH));
    ctx.setVariable(
        "featureCollaboration",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageInstallFeatureCollaboration(),
            null,
            Locale.ENGLISH));
    ctx.setVariable(
        "featureFile",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageInstallFeatureFileSupport(),
            null,
            Locale.ENGLISH));
    ctx.setVariable(
        "featureSecure",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageInstallFeatureSecureEditing(),
            null,
            Locale.ENGLISH));
    ctx.setVariable("url", installationUrl);

    return engine.process("oauth/install", ctx);
  }
}
