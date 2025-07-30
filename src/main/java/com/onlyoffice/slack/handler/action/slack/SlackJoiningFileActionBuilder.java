package com.onlyoffice.slack.handler.action.slack;

import org.springframework.stereotype.Service;

@Service
class SlackJoiningFileActionBuilder implements SlackFileActionBuilder {
  @Override
  public String build(final String sessionId, final String fileId) {
    return sessionId + ":" + fileId;
  }
}
