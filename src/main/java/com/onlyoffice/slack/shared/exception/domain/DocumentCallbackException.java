package com.onlyoffice.slack.shared.exception.domain;

public class DocumentCallbackException extends RuntimeException {
  public DocumentCallbackException(String message) {
    super(message);
  }

  public DocumentCallbackException(String message, Throwable cause) {
    super(message, cause);
  }
}
