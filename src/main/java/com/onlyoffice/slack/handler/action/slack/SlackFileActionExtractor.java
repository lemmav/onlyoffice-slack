package com.onlyoffice.slack.handler.action.slack;

@FunctionalInterface
public interface SlackFileActionExtractor {
  String extract(final String composite, final Type type);

  enum Type {
    SESSION,
    FILE
  }
}
