package com.onlyoffice.slack.domain.document.editor.core;

import com.hazelcast.map.IMap;
import com.onlyoffice.model.documenteditor.config.Document;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.onlyoffice.model.documenteditor.config.document.Permissions;
import com.onlyoffice.slack.domain.document.DocumentServerFormatsConfiguration;
import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.shared.configuration.SlackConfigurationProperties;
import com.onlyoffice.slack.shared.transfer.cache.DocumentSessionKey;
import com.onlyoffice.slack.shared.transfer.request.DownloadSessionRequest;
import com.slack.api.model.File;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Component
@Validated
@RequiredArgsConstructor
public class DocumentFileManagerServiceImpl implements DocumentFileManagerService {
  private final ServerConfigurationProperties serverConfigurationProperties;
  private final SlackConfigurationProperties slackConfigurationProperties;
  private final DocumentServerFormatsConfiguration formatsConfiguration;

  private final IMap<String, DocumentSessionKey> keys;
  private final DocumentFileKeyBuilder documentFileKeyBuilder;
  private final DocumentJwtManagerService documentJwtManagerService;

  @Override
  public String getExtension(@NotNull final File file) {
    if (file == null || file.getName() == null) {
      log.warn("File or file name is null in getExtension");
      return null;
    }

    var filename = file.getName();
    if (!filename.contains(".")) {
      log.warn("File name '{}' does not contain an extension", filename);
      return null;
    }

    var extension = filename.substring(filename.lastIndexOf(".") + 1);
    if (extension.isEmpty()) {
      log.warn("File name '{}' has an empty extension", filename);
      return null;
    }

    log.info("Extracted extension '{}' from file name '{}'", extension.toLowerCase(), filename);
    return extension.toLowerCase();
  }

  @Override
  public DocumentType getDocumentType(@NotNull final File file) {
    var fileExtension = getExtension(file);
    for (var format : formatsConfiguration.getFormats()) {
      if (format.getName().equals(fileExtension)) {
        log.info("Found document type '{}'", format.getType());

        return format.getType();
      }
    }

    log.warn("No document type found for extension");
    return null;
  }

  @Override
  public boolean isEditable(@NotNull final File file) {
    var fileExtension = getExtension(file);
    for (var format : formatsConfiguration.getFormats()) {
      if (format.getName().equals(fileExtension)) {
        var editable = format.getActions().contains("edit");

        log.info("File with extension is editable: {}", editable);

        return editable;
      }
    }

    log.warn("No editable format found for extension");

    return false;
  }

  private String getDocumentKey(
      final String teamId,
      final String userId,
      final File file,
      final String channelId,
      final String messageTs) {
    try {
      var newKey =
          documentFileKeyBuilder.build(file.getId(), teamId, userId, UUID.randomUUID().toString());
      var existingSession =
          keys.putIfAbsent(
              file.getId(),
              DocumentSessionKey.builder()
                  .key(newKey)
                  .channelId(channelId)
                  .messageTs(messageTs)
                  .build());

      if (existingSession == null) {
        log.info("Created new document session key");
        return newKey;
      }

      log.info("Reusing existing document session key");

      return existingSession.getKey();
    } catch (Exception e) {
      log.error("Error getting document key for file", e);
      return UUID.randomUUID().toString();
    }
  }

  @Override
  public Document getDocument(
      @NotBlank final String teamId,
      @NotBlank final String userId,
      @NotNull final File file,
      @NotBlank final String channelId,
      @NotBlank final String messageTs) {
    try {
      MDC.put("team_id", teamId);
      MDC.put("user_id", userId);
      MDC.put("file_id", file != null ? file.getId() : "null");
      MDC.put("channel_id", channelId);
      MDC.put("message_ts", messageTs);

      log.info("Building Document object");

      var token =
          documentJwtManagerService.createToken(
              DownloadSessionRequest.builder()
                  .fileId(file.getId())
                  .teamId(teamId)
                  .userId(userId)
                  .build(),
              serverConfigurationProperties.getCryptography().getSecret());
      var document =
          Document.builder()
              .fileType(getExtension(file))
              .key(getDocumentKey(teamId, userId, file, channelId, messageTs))
              .title(file.getName())
              .url(
                  slackConfigurationProperties
                      .getDownloadPathPattern()
                      .formatted(serverConfigurationProperties.getBaseAddress(), token))
              .permissions(Permissions.builder().edit(isEditable(file)).build())
              .build();

      log.info("Document object built successfully");

      return document;
    } finally {
      MDC.clear();
    }
  }
}
