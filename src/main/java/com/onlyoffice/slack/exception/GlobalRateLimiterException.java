package com.onlyoffice.slack.exception;

public class GlobalRateLimiterException extends RuntimeException {
  public GlobalRateLimiterException(String message) {
    super(message);
  }
}
