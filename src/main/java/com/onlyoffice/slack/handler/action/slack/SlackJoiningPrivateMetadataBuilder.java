package com.onlyoffice.slack.handler.action.slack;

import org.springframework.stereotype.Service;

@Service
class SlackJoiningPrivateMetadataBuilder implements SlackPrivateMetadataBuilder {
  @Override
  public String build(final String channelId, final String messageTs) {
    return channelId + ":" + messageTs;
  }
}
