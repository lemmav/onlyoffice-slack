package com.onlyoffice.slack.handler.action.slack;

@FunctionalInterface
public interface SlackFileActionBuilder {
  String build(final String sessionId, final String fileId);
}
