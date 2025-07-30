package com.onlyoffice.slack.controller;

import com.onlyoffice.slack.configuration.slack.SlackMessageConfigurationProperties;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class SlackInstallationController {
  private final SlackMessageConfigurationProperties slackMessageConfigurationProperties;

  private final MessageSource messageSource;

  @GetMapping(value = "/slack/oauth/completion")
  public String completion(Model model) {
    model.addAttribute(
        "completionTitle",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCompletionTitle(), null, Locale.ENGLISH));
    model.addAttribute(
        "completionText",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCompletionText(), null, Locale.ENGLISH));
    model.addAttribute(
        "completionDescription",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCompletionDescription(),
            null,
            Locale.ENGLISH));
    model.addAttribute(
        "completionButton",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCompletionButton(),
            null,
            Locale.ENGLISH));

    return "completion";
  }

  @GetMapping(value = "/slack/oauth/cancellation")
  public String cancellation(Model model) {
    model.addAttribute(
        "cancellationTitle",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCancellationTitle(),
            null,
            Locale.ENGLISH));
    model.addAttribute(
        "cancellationText",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCancellationText(),
            null,
            Locale.ENGLISH));
    model.addAttribute(
        "cancellationDescription",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCancellationDescription(),
            null,
            Locale.ENGLISH));
    model.addAttribute(
        "cancellationButton",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCancellationButton(),
            null,
            Locale.ENGLISH));

    return "cancellation";
  }
}
