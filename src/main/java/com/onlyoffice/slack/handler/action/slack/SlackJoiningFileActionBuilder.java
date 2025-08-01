package com.onlyoffice.slack.handler.action.slack;

import org.springframework.stereotype.Service;

@Service
class SlackJoiningFileActionBuilder implements SlackFileActionBuilder {
  @Override
  public String build(final String userId, final String sessionId, final String fileId) {
    return userId + ":" + sessionId + ":" + fileId;
  }
}
