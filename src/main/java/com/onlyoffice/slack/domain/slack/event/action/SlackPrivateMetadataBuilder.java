package com.onlyoffice.slack.domain.slack.event.action;

@FunctionalInterface
public interface SlackPrivateMetadataBuilder {
  String build(final String channelId, final String messageTs);
}
