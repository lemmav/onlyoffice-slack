package com.onlyoffice.slack.domain.document.editor.core;

import jakarta.validation.constraints.NotBlank;

@FunctionalInterface
public interface DocumentFileKeyBuilder {
  String build(
      @NotBlank final String fileId,
      @NotBlank final String teamId,
      @NotBlank final String userId,
      @NotBlank final String uuid);
}
