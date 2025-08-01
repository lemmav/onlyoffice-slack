package com.onlyoffice.slack.controller;

import com.onlyoffice.slack.configuration.slack.SlackMessageConfigurationProperties;
import com.onlyoffice.slack.exception.SettingsConfigurationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalErrorController implements ErrorController {
  private final SlackMessageConfigurationProperties slackMessageConfigurationProperties;
  private final MessageSource messageSource;

  @RequestMapping("/error")
  public String handleError(final HttpServletRequest request, final Model model) {
    var title =
        Optional.ofNullable(request.getAttribute("title"))
            .orElse(
                messageSource.getMessage(
                    slackMessageConfigurationProperties.getErrorGenericTitle(),
                    null,
                    Locale.ENGLISH))
            .toString();
    var text =
        Optional.ofNullable(request.getAttribute("text"))
            .orElse(
                messageSource.getMessage(
                    slackMessageConfigurationProperties.getErrorGenericText(),
                    null,
                    Locale.ENGLISH))
            .toString();
    var action =
        Optional.ofNullable(request.getAttribute("action"))
            .orElse(
                messageSource.getMessage(
                    slackMessageConfigurationProperties.getErrorGenericButton(),
                    null,
                    Locale.ENGLISH))
            .toString();

    model.addAttribute("title", title);
    model.addAttribute("text", text);
    model.addAttribute("button", action);

    return "error";
  }

  @ExceptionHandler(SettingsConfigurationException.class)
  public String handleSettingsConfigurationException(
      final SettingsConfigurationException ex, final Model model) {
    model.addAttribute("title", ex.getTitle());
    model.addAttribute("description", ex.getMessage());
    model.addAttribute("button", ex.getAction());

    return "nosettings";
  }

  @ExceptionHandler(Exception.class)
  public String handleGenericException(final Exception ex, final Model model) {
    model.addAttribute(
        "title",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getErrorGenericTitle(), null, Locale.ENGLISH));
    model.addAttribute(
        "text",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getErrorGenericText(), null, Locale.ENGLISH));
    model.addAttribute(
        "button",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getErrorGenericButton(), null, Locale.ENGLISH));

    return "error";
  }
}
