package com.onlyoffice.slack.shared.exception.domain;

public class DocumentTokenValidationException extends RuntimeException {
  public DocumentTokenValidationException(String message) {
    super(message);
  }

  public DocumentTokenValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
