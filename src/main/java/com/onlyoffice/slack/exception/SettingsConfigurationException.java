package com.onlyoffice.slack.exception;

public class SettingsConfigurationException extends RuntimeException {

  private final SettingsErrorType errorType;

  public SettingsConfigurationException(SettingsErrorType errorType, String message) {
    super(message);
    this.errorType = errorType;
  }

  public SettingsConfigurationException(
      SettingsErrorType errorType, String message, Throwable cause) {
    super(message, cause);
    this.errorType = errorType;
  }

  public SettingsErrorType getErrorType() {
    return errorType;
  }

  public enum SettingsErrorType {
    NOT_CONFIGURED,
    INCOMPLETE_CONFIGURATION,
    INVALID_CONFIGURATION
  }
}
