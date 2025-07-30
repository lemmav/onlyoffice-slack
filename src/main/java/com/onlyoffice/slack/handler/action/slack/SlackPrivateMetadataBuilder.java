package com.onlyoffice.slack.handler.action.slack;

@FunctionalInterface
public interface SlackPrivateMetadataBuilder {
  String build(final String channelId, final String messageTs);
}
