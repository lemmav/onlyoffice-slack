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
        "title",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCompletionTitle(), null, Locale.ENGLISH));
    model.addAttribute(
        "text",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCompletionText(), null, Locale.ENGLISH));
    model.addAttribute(
        "button",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCompletionButton(),
            null,
            Locale.ENGLISH));

    return "oauth/completion";
  }

  @GetMapping(value = "/slack/oauth/cancellation")
  public String cancellation(Model model) {
    model.addAttribute(
        "title",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCancellationTitle(),
            null,
            Locale.ENGLISH));
    model.addAttribute(
        "text",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCancellationText(),
            null,
            Locale.ENGLISH));
    model.addAttribute(
        "button",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageCancellationButton(),
            null,
            Locale.ENGLISH));

    return "oauth/cancellation";
  }
}
