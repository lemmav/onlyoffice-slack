package com.onlyoffice.slack.domain.slack.event.action;

import org.springframework.stereotype.Service;

@Service
class SlackFileActionBuilderImpl implements SlackFileActionBuilder {
  @Override
  public String build(final String userId, final String sessionId, final String fileId) {
    return userId + ":" + sessionId + ":" + fileId;
  }
}
