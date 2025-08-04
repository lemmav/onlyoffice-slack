package com.onlyoffice.slack.domain.document.editor.core;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@FunctionalInterface
public interface DocumentFileKeyExtractor {
  String extract(@NotBlank final String key, @NotNull final Type type);

  enum Type {
    FILE,
    TEAM,
    USER
  }
}
