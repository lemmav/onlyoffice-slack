package com.onlyoffice.slack.service.document.core;

import com.onlyoffice.model.documenteditor.config.Document;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.slack.api.model.File;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface DocumentFileManagerService {
  String getExtension(@NotNull final File file);

  DocumentType getDocumentType(@NotNull final File file);

  boolean isEditable(@NotNull final File file);

  Document getDocument(
      @NotBlank final String teamId,
      @NotBlank final String userId,
      @NotNull final File file,
      @NotBlank final String channelId,
      @NotBlank final String messageTs);
}
