package com.onlyoffice.slack.domain.slack.event.action;

@FunctionalInterface
public interface SlackFileActionBuilder {
  String build(final String userId, final String sessionId, final String fileId);
}
