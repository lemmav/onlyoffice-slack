package com.onlyoffice.slack.shared.exception;

import com.onlyoffice.slack.shared.configuration.message.MessageSourceSlackConfiguration;
import com.onlyoffice.slack.shared.exception.domain.DocumentSettingsConfigurationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@Controller
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorController {
  private final MessageSourceSlackConfiguration messageSourceSlackConfiguration;
  private final MessageSource messageSource;

  @RequestMapping("/error")
  public String handleError(final HttpServletRequest request, final Model model) {
    var title =
        Optional.ofNullable(request.getAttribute("title"))
            .orElse(
                messageSource.getMessage(
                    messageSourceSlackConfiguration.getErrorGenericTitle(), null, Locale.ENGLISH))
            .toString();
    var text =
        Optional.ofNullable(request.getAttribute("text"))
            .orElse(
                messageSource.getMessage(
                    messageSourceSlackConfiguration.getErrorGenericText(), null, Locale.ENGLISH))
            .toString();
    var action =
        Optional.ofNullable(request.getAttribute("action"))
            .orElse(
                messageSource.getMessage(
                    messageSourceSlackConfiguration.getErrorGenericButton(), null, Locale.ENGLISH))
            .toString();

    model.addAttribute("title", title);
    model.addAttribute("text", text);
    model.addAttribute("button", action);

    return "errors/global";
  }

  @ExceptionHandler(DocumentSettingsConfigurationException.class)
  public String handleSettingsConfigurationException(
      final DocumentSettingsConfigurationException ex, final Model model) {
    model.addAttribute("title", ex.getTitle());
    model.addAttribute("text", ex.getMessage());
    model.addAttribute("button", ex.getAction());

    return "errors/no_settings";
  }

  @ExceptionHandler(FileContentLengthException.class)
  public ResponseEntity<?> handleFileContentLengthException(final FileContentLengthException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public String handleResourceException(final NoResourceFoundException ex, final Model model) {
    model.addAttribute(
        "title",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getErrorResourceTitle(), null, Locale.ENGLISH));
    model.addAttribute(
        "text",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getErrorResourceText(), null, Locale.ENGLISH));
    model.addAttribute(
        "button",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getErrorResourceButton(), null, Locale.ENGLISH));

    return "errors/global";
  }

  @ExceptionHandler(Exception.class)
  public String handleGenericException(final Exception ex, final Model model) {
    model.addAttribute(
        "title",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getErrorGenericTitle(), null, Locale.ENGLISH));
    model.addAttribute(
        "text",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getErrorGenericText(), null, Locale.ENGLISH));
    model.addAttribute(
        "button",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getErrorGenericButton(), null, Locale.ENGLISH));

    return "errors/global";
  }
}
