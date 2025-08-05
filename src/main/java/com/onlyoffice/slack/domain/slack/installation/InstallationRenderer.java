package com.onlyoffice.slack.domain.slack.installation;

import com.onlyoffice.slack.shared.configuration.message.MessageSourceSlackConfiguration;
import com.slack.api.bolt.service.builtin.oauth.view.OAuthInstallPageRenderer;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Configuration
@RequiredArgsConstructor
class InstallationRenderer implements OAuthInstallPageRenderer {
  private final MessageSourceSlackConfiguration messageSourceSlackConfiguration;

  private final SpringTemplateEngine engine;
  private final MessageSource messageSource;

  public String render(final String installationUrl) {
    var ctx = new Context();
    ctx.setVariable(
        "title",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageInstallTitle(), null, Locale.ENGLISH));
    ctx.setVariable(
        "subtitle",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageInstallSubtitle(), null, Locale.ENGLISH));
    ctx.setVariable(
        "button",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageInstallButton(), null, Locale.ENGLISH));
    ctx.setVariable(
        "featureEdit",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageInstallFeatureEdit(), null, Locale.ENGLISH));
    ctx.setVariable(
        "featureCollaboration",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageInstallFeatureCollaboration(),
            null,
            Locale.ENGLISH));
    ctx.setVariable(
        "featureFile",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageInstallFeatureFileSupport(),
            null,
            Locale.ENGLISH));
    ctx.setVariable(
        "featureSecure",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageInstallFeatureSecureEditing(),
            null,
            Locale.ENGLISH));
    ctx.setVariable("url", installationUrl);

    return engine.process("installation/install", ctx);
  }
}
