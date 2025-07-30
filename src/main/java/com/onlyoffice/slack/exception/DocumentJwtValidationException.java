package com.onlyoffice.slack.exception;

public class DocumentJwtValidationException extends RuntimeException {
  public DocumentJwtValidationException(String message) {
    super(message);
  }

  public DocumentJwtValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
