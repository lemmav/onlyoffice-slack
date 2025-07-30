package com.onlyoffice.slack.handler.action.slack;

@FunctionalInterface
public interface SlackPrivateMetadataExtractor {
  String extract(final String metadata, final Type type);

  enum Type {
    CHANNEL,
    MESSAGETS
  }
}
