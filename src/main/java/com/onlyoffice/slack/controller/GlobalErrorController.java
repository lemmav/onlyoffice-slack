package com.onlyoffice.slack.controller;

import com.onlyoffice.slack.exception.SettingsConfigurationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@ControllerAdvice
public class GlobalErrorController implements ErrorController {

  @RequestMapping("/error")
  public String handleError(final HttpServletRequest request, final Model model) {
    var statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    var errorMessage = (String) request.getAttribute("jakarta.servlet.error.message");
    var exception = (Throwable) request.getAttribute("jakarta.servlet.error.exception");

    log.error("Error occurred: status={}, message={}", statusCode, errorMessage, exception);

    var status =
        statusCode != null ? HttpStatus.valueOf(statusCode) : HttpStatus.INTERNAL_SERVER_ERROR;

    var title = getErrorTitle(status);
    var message = getErrorMessage(status, errorMessage);
    var buttonText = getButtonText(status);

    model.addAttribute("errorTitle", title);
    model.addAttribute("errorMessage", message);
    model.addAttribute("buttonText", buttonText);
    model.addAttribute("statusCode", status.value());

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

  @ExceptionHandler(ResponseStatusException.class)
  public String handleResponseStatusException(final ResponseStatusException ex, final Model model) {
    log.error(
        "ResponseStatusException occurred: status={}, reason={}",
        ex.getStatusCode(),
        ex.getReason(),
        ex);

    var title = getErrorTitle(HttpStatus.valueOf(ex.getStatusCode().value()));
    var message =
        ex.getReason() != null
            ? ex.getReason()
            : getErrorMessage(HttpStatus.valueOf(ex.getStatusCode().value()), null);
    var buttonText = getButtonText(HttpStatus.valueOf(ex.getStatusCode().value()));

    model.addAttribute("errorTitle", title);
    model.addAttribute("errorMessage", message);
    model.addAttribute("buttonText", buttonText);
    model.addAttribute("statusCode", ex.getStatusCode().value());

    return "error";
  }

  @ExceptionHandler(Exception.class)
  public String handleGenericException(final Exception ex, final Model model) {
    log.error("Unexpected error occurred", ex);

    model.addAttribute("errorTitle", "Oops! Something went wrong");
    model.addAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
    model.addAttribute("buttonText", "Back to Slack");
    model.addAttribute("statusCode", 500);

    return "error";
  }

  private String getErrorTitle(final HttpStatus status) {
    return switch (status) {
      case NOT_FOUND -> "File Not Found";
      case FORBIDDEN -> "Access Denied";
      case UNAUTHORIZED -> "Authentication Required";
      case REQUEST_TIMEOUT -> "Request Timeout";
      case BAD_REQUEST -> "Invalid Request";
      case SERVICE_UNAVAILABLE -> "Service Not Available";
      default -> "Oops! Something went wrong";
    };
  }

  private String getErrorMessage(final HttpStatus status, final String originalMessage) {
    if (originalMessage != null && !originalMessage.trim().isEmpty()) return originalMessage;

    return switch (status) {
      case NOT_FOUND ->
          "The requested file or resource could not be found. It may have been moved, deleted, or you may not have permission to access it.";
      case FORBIDDEN ->
          "Your session has expired or you don't have permission to access this resource.";
      case UNAUTHORIZED -> "Please authenticate with Slack to access this resource.";
      case REQUEST_TIMEOUT -> "The request took too long to complete. Please try again.";
      case BAD_REQUEST ->
          "The request contains invalid parameters. Please check your input and try again.";
      case SERVICE_UNAVAILABLE ->
          "The service is not properly configured. Please contact your administrator.";
      default ->
          "An unexpected error occurred while processing your request. Please try again later.";
    };
  }

  private String getButtonText(final HttpStatus status) {
    return switch (status) {
      case UNAUTHORIZED -> "Sign in with Slack";
      case FORBIDDEN -> "Return to Slack";
      default -> "Back to Slack";
    };
  }
}
