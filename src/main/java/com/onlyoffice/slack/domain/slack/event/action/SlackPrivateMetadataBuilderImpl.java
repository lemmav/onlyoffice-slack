package com.onlyoffice.slack.domain.slack.event.action;

import org.springframework.stereotype.Service;

@Service
class SlackPrivateMetadataBuilderImpl implements SlackPrivateMetadataBuilder {
  @Override
  public String build(final String channelId, final String messageTs) {
    return channelId + ":" + messageTs;
  }
}
