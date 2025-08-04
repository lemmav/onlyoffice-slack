package com.onlyoffice.slack.shared.exception.domain;

import lombok.Getter;

@Getter
public class DocumentSettingsConfigurationException extends RuntimeException {
  private final SettingsErrorType errorType;
  private final String title;
  private final String action;

  public DocumentSettingsConfigurationException(
      SettingsErrorType errorType, String title, String message, String action) {
    super(message);
    this.errorType = errorType;
    this.title = title;
    this.action = action;
  }

  public DocumentSettingsConfigurationException(
      SettingsErrorType errorType, String title, String message, String action, Throwable cause) {
    super(message, cause);
    this.errorType = errorType;
    this.title = title;
    this.action = action;
  }

  public enum SettingsErrorType {
    INCOMPLETE_CONFIGURATION,
    INVALID_CONFIGURATION,
    DEMO_EXPIRED
  }
}
