package com.onlyoffice.slack.domain.slack.event.action;

@FunctionalInterface
public interface SlackPrivateMetadataExtractor {
  String extract(final String metadata, final Type type);

  enum Type {
    CHANNEL,
    MESSAGETS
  }
}
