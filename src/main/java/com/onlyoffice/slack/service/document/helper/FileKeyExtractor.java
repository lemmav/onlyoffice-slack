package com.onlyoffice.slack.service.document.helper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
public class FileKeyExtractor implements DocumentFileKeyExtractor {
  @Override
  public String extract(@NotBlank final String key, @NotNull final Type type) {
    try {
      var parts = key.split("_");
      if (parts.length != 4) {
        MDC.put("length", String.valueOf(parts.length));
        log.error("Malformed document key. Invalid number of parts");
        return null;
      }

      switch (type) {
        case FILE -> {
          return parts[0];
        }
        case TEAM -> {
          return parts[1];
        }
        case USER -> {
          return parts[2];
        }
        default -> {
          return parts[3];
        }
      }
    } finally {
      MDC.clear();
    }
  }
}
