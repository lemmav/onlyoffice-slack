package com.onlyoffice.slack.domain.slack.event.action;

import org.springframework.stereotype.Service;

@Service
class SlackFileActionExtractorImpl implements SlackFileActionExtractor {
  @Override
  public String extract(final String composite, final Type type) {
    var ids = composite.split(":");
    if (ids.length != 3) {
      return null;
    }

    return switch (type) {
      case USER -> ids[0];
      case SESSION -> ids[1];
      case FILE -> ids[2];
    };
  }
}
