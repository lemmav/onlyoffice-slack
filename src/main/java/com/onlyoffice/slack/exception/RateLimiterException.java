package com.onlyoffice.slack.exception;

import lombok.Getter;

@Getter
public class RateLimiterException extends RuntimeException {
  private final String title;
  private final String action;

  public RateLimiterException(String message, String title, String action) {
    super(message);
    this.title = title;
    this.action = action;
  }
}
