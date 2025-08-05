package com.onlyoffice.slack.domain.document.event.handler;

import com.hazelcast.map.IMap;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Status;
import com.onlyoffice.slack.domain.document.editor.core.DocumentFileKeyExtractor;
import com.onlyoffice.slack.domain.document.event.registry.DocumentCallbackRegistrar;
import com.onlyoffice.slack.domain.slack.installation.RotatingInstallationService;
import com.onlyoffice.slack.shared.configuration.client.OkHttpClientPoolService;
import com.onlyoffice.slack.shared.exception.domain.DocumentCallbackException;
import com.onlyoffice.slack.shared.transfer.cache.DocumentSessionKey;
import com.onlyoffice.slack.shared.utils.SafeOptional;
import com.onlyoffice.slack.shared.utils.TriFunction;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.files.FilesInfoRequest;
import com.slack.api.methods.request.files.FilesUploadV2Request;
import com.slack.api.methods.response.files.FilesInfoResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class DocumentSaveCallbackHandler implements DocumentCallbackRegistrar {
  private static final int MAX_FILE_SIZE = 25 * 1024 * 1024;

  private final DocumentFileKeyExtractor documentFileKeyExtractor;
  private final RotatingInstallationService installationService;
  private final OkHttpClientPoolService httpClientPoolService;
  private final IMap<String, DocumentSessionKey> keys;
  private final App app;

  private Optional<Long> getContentLength(final String downloadUrl) {
    try (var response =
        httpClientPoolService
            .getHttpClient()
            .newCall(new Request.Builder().url(downloadUrl).head().build())
            .execute()) {

      if (!response.isSuccessful()) {
        log.warn("Could not get file Content-Length with status {}", response.code());
        return Optional.empty();
      }

      var contentLengthHeader = response.header("Content-Length");
      if (contentLengthHeader == null) {
        log.warn("Could not get Content-Length header");
        return Optional.empty();
      }

      try {
        long contentLength = Long.parseLong(contentLengthHeader);
        return Optional.of(contentLength);
      } catch (NumberFormatException e) {
        log.warn("Could not parse Content-Length header value {}", contentLengthHeader);
        return Optional.empty();
      }
    } catch (IOException e) {
      log.warn("Failed to perform a request to get Content-Length", e);
      return Optional.empty();
    }
  }

  private byte[] downloadFile(final String downloadUrl) throws IOException {
    try (var response =
        httpClientPoolService
            .getHttpClient()
            .newCall(new Request.Builder().url(downloadUrl).build())
            .execute()) {
      if (!response.isSuccessful())
        throw new DocumentCallbackException(
            "Failed to download file from a document server. Status code is %d"
                .formatted(response.code()));

      var body = response.body();
      if (body == null)
        throw new DocumentCallbackException(
            "Empty response body when downloading file from a document server");

      return body.bytes();
    }
  }

  private Optional<FilesInfoResponse> findFile(final String fileId, final String token) {
    return SafeOptional.of(
        () ->
            app.getClient()
                .filesInfo(FilesInfoRequest.builder().file(fileId).token(token).build()));
  }

  private void uploadFile(
      final String downloadUrl,
      final String fileName,
      final String channelId,
      final String userToken,
      final String threadTs)
      throws IOException, SlackApiException {
    log.info("Starting upload to Slack for current file");

    var contentLength = getContentLength(downloadUrl);
    if (contentLength.isEmpty())
      throw new DocumentCallbackException("Could not determine Content-Length for file");

    var size = contentLength.get();
    if (size > MAX_FILE_SIZE)
      throw new DocumentCallbackException(
          "File size (%d bytes) exceeds maximum allowed size (%d bytes) for file %s"
              .formatted(size, MAX_FILE_SIZE, fileName));

    log.info("File size validation passed with {} bytes for current file", size);
    var fileContent = downloadFile(downloadUrl);
    if (fileContent.length == 0)
      throw new DocumentCallbackException(
          "Failed to download file content for file %s".formatted(fileName));

    log.info("Downloaded file content {} bytes for current file", fileContent.length);

    var requestBuilder =
        FilesUploadV2Request.builder()
            .token(userToken)
            .filename(fileName)
            .title(fileName)
            .fileData(fileContent);
    if (threadTs != null && !threadTs.trim().isEmpty()) {
      requestBuilder.channel(channelId);
      requestBuilder.threadTs(threadTs);
      log.info("Uploading file as reply to message in the channel");
    } else {
      requestBuilder.channels(List.of(channelId));
      log.info("Uploading file to the channel");
    }

    var uploadResponse = app.client().filesUploadV2(requestBuilder.build());

    if (!uploadResponse.isOk())
      throw new DocumentCallbackException(
          "Failed to upload a new file to Slack: %s".formatted(uploadResponse.getError()));
  }

  private void sendPersonalMessage(
      final String ownerId,
      final String fileName,
      final String botToken,
      final String editorUserId) {
    try {
      log.info("Sending personal message to file owner {}", ownerId);

      var message = String.format("Your file %s has been edited by <@%s>", fileName, editorUserId);

      var response =
          app.client()
              .chatPostMessage(
                  ChatPostMessageRequest.builder()
                      .token(botToken)
                      .channel(ownerId)
                      .text(message)
                      .build());

      if (!response.isOk()) {
        log.warn(
            "Failed to send personal message to file owner {}: {}", ownerId, response.getError());
      } else {
        log.info("Personal message sent successfully to file owner {} from bot", ownerId);
      }
    } catch (IOException | SlackApiException e) {
      log.warn("Error sending personal message to file owner {}: {}", ownerId, e.getMessage());
    }
  }

  @Override
  public Status getStatus() {
    return Status.SAVE;
  }

  @Override
  public TriFunction<String, String, Callback, Callback> getHandler() {
    return (teamId, userId, callback) -> {
      var fileId =
          documentFileKeyExtractor.extract(callback.getKey(), DocumentFileKeyExtractor.Type.FILE);

      if (fileId == null)
        throw new DocumentCallbackException(
            "Could not extract fileId from callback's key for user %s in team %s"
                .formatted(userId, teamId));

      var installer = installationService.findInstallerWithRotation(null, teamId, userId);
      if (installer == null)
        throw new DocumentCallbackException(
            "Could not find an installer for user %s in team %s".formatted(userId, teamId));

      var maybeFile = findFile(fileId, installer.getInstallerUserAccessToken());
      if (maybeFile.isEmpty() || !maybeFile.get().isOk())
        throw new DocumentCallbackException(
            "Failed to get file info from Slack for user %s in team %s".formatted(userId, teamId));

      try {
        var file = maybeFile.get().getFile();
        var sessionKey = keys.get(fileId);

        MDC.put("team_id", teamId);
        MDC.put("user_id", userId);
        MDC.put("file_id", file.getId());

        log.info("Session key retrieved: {}", sessionKey);

        String channelId = null;
        String messageTs = null;

        if (sessionKey != null) {
          channelId = sessionKey.getChannelId();
          messageTs = sessionKey.getMessageTs();
        }

        if (messageTs != null && (channelId == null || channelId.trim().isEmpty())) {
          log.warn(
              "MessageTs provided ({}) but channelId is null/empty. Cannot upload as reply, falling back to original channel.",
              messageTs);
          messageTs = null;
        }

        var targetChannelId = channelId;
        if (targetChannelId == null || targetChannelId.trim().isEmpty()) {
          if (file.getChannels() != null && !file.getChannels().isEmpty()) {
            targetChannelId = file.getChannels().getFirst();
            log.info("Using file's original channel as fallback");
          } else {
            throw new DocumentCallbackException("No channel available for file upload");
          }
        }

        uploadFile(
            callback.getUrl(),
            file.getName(),
            targetChannelId,
            installer.getInstallerUserAccessToken(),
            messageTs);

        if (!file.getUser().equalsIgnoreCase(userId))
          sendPersonalMessage(
              file.getUser(), file.getName(), installer.getBotAccessToken(), userId);
      } catch (IOException | SlackApiException e) {
        throw new DocumentCallbackException(
            "Could not upload a new file for user %s in team %s".formatted(userId, teamId), e);
      } finally {
        keys.remove(fileId);
        MDC.clear();
      }

      return callback;
    };
  }
}
