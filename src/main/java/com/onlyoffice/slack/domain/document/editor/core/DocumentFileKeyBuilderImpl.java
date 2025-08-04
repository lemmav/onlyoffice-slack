package com.onlyoffice.slack.domain.document.editor.core;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class DocumentFileKeyBuilderImpl implements DocumentFileKeyBuilder {

  @Override
  public String build(
      @NotBlank final String fileId,
      @NotBlank final String teamId,
      @NotBlank final String userId,
      @NotBlank final String uuid) {
    return "%s_%s_%s_%s".formatted(fileId, teamId, userId, UUID.randomUUID().toString());
  }
}
