package com.onlyoffice.slack.domain.slack.event.action;

import org.springframework.stereotype.Service;

@Service
public class SlackPrivateMetadataExtractorImpl implements SlackPrivateMetadataExtractor {
  @Override
  public String extract(final String metadata, final Type type) {
    var parts = metadata.split(":", 2);
    if (parts.length != 2) return null;

    return switch (type) {
      case CHANNEL -> parts[0];
      case MESSAGETS -> parts[1];
    };
  }
}
