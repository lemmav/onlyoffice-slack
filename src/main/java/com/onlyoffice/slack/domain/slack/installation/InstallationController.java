package com.onlyoffice.slack.domain.slack.installation;

import com.onlyoffice.slack.shared.configuration.message.MessageSourceSlackConfiguration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
class InstallationController {
  private final MessageSourceSlackConfiguration messageSourceSlackConfiguration;

  private final MessageSource messageSource;

  @GetMapping(value = "/slack/oauth/completion")
  public String completion(Model model) {
    model.addAttribute(
        "title",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageCompletionTitle(), null, Locale.ENGLISH));
    model.addAttribute(
        "text",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageCompletionText(), null, Locale.ENGLISH));
    model.addAttribute(
        "button",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageCompletionButton(), null, Locale.ENGLISH));

    return "installation/completion";
  }

  @GetMapping(value = "/slack/oauth/cancellation")
  public String cancellation(Model model) {
    model.addAttribute(
        "title",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageCancellationTitle(), null, Locale.ENGLISH));
    model.addAttribute(
        "text",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageCancellationText(), null, Locale.ENGLISH));
    model.addAttribute(
        "button",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageCancellationButton(), null, Locale.ENGLISH));

    return "installation/cancellation";
  }
}
