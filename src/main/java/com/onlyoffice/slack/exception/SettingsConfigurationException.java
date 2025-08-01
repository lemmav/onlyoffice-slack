package com.onlyoffice.slack.exception;

import lombok.Getter;

@Getter
public class SettingsConfigurationException extends RuntimeException {
  private final SettingsErrorType errorType;
  private final String title;
  private final String action;

  public SettingsConfigurationException(
      SettingsErrorType errorType, String title, String message, String action) {
    super(message);
    this.errorType = errorType;
    this.title = title;
    this.action = action;
  }

  public SettingsConfigurationException(
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
