package com.onlyoffice.slack.domain.slack.event.action;

@FunctionalInterface
public interface SlackFileActionExtractor {
  String extract(final String composite, final Type type);

  enum Type {
    USER,
    SESSION,
    FILE
  }
}
