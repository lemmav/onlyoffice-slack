package com.onlyoffice.slack.service.document.helper;

import jakarta.validation.constraints.NotBlank;

@FunctionalInterface
public interface DocumentFileKeyBuilder {
  String build(
      @NotBlank final String fileId,
      @NotBlank final String teamId,
      @NotBlank final String userId,
      @NotBlank final String uuid);
}
