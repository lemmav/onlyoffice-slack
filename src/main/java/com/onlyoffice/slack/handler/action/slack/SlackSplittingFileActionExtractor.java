package com.onlyoffice.slack.handler.action.slack;

import org.springframework.stereotype.Service;

@Service
class SlackSplittingFileActionExtractor implements SlackFileActionExtractor {
  @Override
  public String extract(final String composite, final Type type) {
    var ids = composite.split(":");
    if (ids.length != 2) {
      return null;
    }

    return switch (type) {
      case SESSION -> ids[0];
      case FILE -> ids[1];
    };
  }
}
