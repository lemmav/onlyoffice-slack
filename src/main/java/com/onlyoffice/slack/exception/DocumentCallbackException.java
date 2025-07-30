package com.onlyoffice.slack.exception;

public class DocumentCallbackException extends RuntimeException {
  public DocumentCallbackException(String message) {
    super(message);
  }

  public DocumentCallbackException(String message, Throwable cause) {
    super(message, cause);
  }
}
