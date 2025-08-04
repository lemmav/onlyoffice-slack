package com.onlyoffice.slack.domain.document.proxy;

import com.onlyoffice.slack.domain.slack.installation.RotatingInstallationService;
import com.onlyoffice.slack.shared.configuration.client.OkHttpClientPoolService;
import com.onlyoffice.slack.shared.exception.FileContentLengthException;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.files.FilesInfoRequest;
import com.slack.api.methods.response.files.FilesInfoResponse;
import com.slack.api.model.File;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class DocumentFileStreamingServiceImpl implements DocumentFileStreamingService {
  // TODO: Test exceptions
  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
  private static final int BUFFER_SIZE = 64 * 1024;

  private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

  private final RotatingInstallationService installationService;
  private final OkHttpClientPoolService httpClientPoolService;
  private final App app;

  private CompletableFuture<Optional<FilesInfoResponse>> getFileInfoAsync(
      final String teamId, final String userId, final String fileId) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            log.info("Fetching file info from Slack API");

            var installer = installationService.findInstaller(null, teamId, userId);
            var fileInfo =
                app.getClient()
                    .filesInfo(
                        FilesInfoRequest.builder()
                            .file(fileId)
                            .token(installer.getInstallerUserAccessToken())
                            .build());

            if (!fileInfo.isOk()) {
              log.error("Failed to get file info from Slack API. Error: {}", fileInfo.getError());
              return Optional.empty();
            }

            return Optional.of(fileInfo);
          } catch (SlackApiException | IOException e) {
            log.error("Slack API error: {}", e.getMessage());
            return Optional.empty();
          }
        },
        virtualThreadExecutor);
  }

  private void validateFileSize(final File file) {
    if (file.getSize() > MAX_FILE_SIZE)
      throw new FileContentLengthException(
          String.format(
              "File size (%d bytes) exceeds maximum allowed size (%d bytes)",
              file.getSize(), MAX_FILE_SIZE));

    if (file.getSize() <= 0)
      throw new FileContentLengthException("File size is less or equal to 0 bytes");
  }

  private void setResponseHeaders(final HttpServletResponse response, final File file) {
    response.setContentType(file.getMimetype());
    response.setContentLength(file.getSize());
    response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");
  }

  private void streamWithNIO(
      final ReadableByteChannel sourceChannel,
      final WritableByteChannel targetChannel,
      final String fileName,
      final HttpServletResponse response)
      throws IOException {
    var buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    var totalBytesTransferred = 0L;

    var bytesRead = 0;
    while ((bytesRead = sourceChannel.read(buffer)) != -1) {
      if (bytesRead == 0) {
        Thread.onSpinWait();
        continue;
      }

      buffer.flip();
      while (buffer.hasRemaining()) {
        var bytesWritten = targetChannel.write(buffer);
        if (bytesWritten == 0) {
          Thread.onSpinWait();
          continue;
        }

        totalBytesTransferred += bytesWritten;
      }

      buffer.clear();
      if (totalBytesTransferred % (BUFFER_SIZE * 10) == 0) {
        try {
          response.getOutputStream().flush();
          response.getOutputStream().write(new byte[0]);
        } catch (IOException e) {
          log.warn(
              "Client disconnection detected during NIO streaming for: {} at {} bytes",
              fileName,
              totalBytesTransferred);
          throw new IOException("Client disconnected during file transfer", e);
        }
      }

      Thread.yield();
    }

    response.getOutputStream().flush();
  }

  private void streamFileContentNIO(
      final String teamId, final String userId, final File file, final HttpServletResponse response)
      throws IOException {
    var installer = installationService.findInstaller(null, teamId, userId);
    var request =
        new Request.Builder()
            .url(file.getUrlPrivateDownload())
            .header("Authorization", "Bearer " + installer.getInstallerUserAccessToken())
            .build();

    try (var okResponse = httpClientPoolService.getHttpClient().newCall(request).execute()) {
      if (!okResponse.isSuccessful() || okResponse.body() == null)
        throw new IOException("Failed to download file");

      try (var inputStream = okResponse.body().byteStream();
          ReadableByteChannel sourceChannel = Channels.newChannel(inputStream);
          WritableByteChannel targetChannel = Channels.newChannel(response.getOutputStream())) {
        streamWithNIO(sourceChannel, targetChannel, file.getName(), response);
      }
    }
  }

  @Override
  public void processDownloadAsync(
      @NotBlank final String teamId,
      @NotBlank final String userId,
      @NotBlank final String fileId,
      @NotNull final HttpServletResponse response)
      throws IOException {
    try {
      MDC.put("team_id", teamId);
      MDC.put("user_id", userId);
      MDC.put("file_id", fileId);

      var fileInfo = getFileInfoAsync(teamId, userId, fileId).join();
      var file =
          fileInfo.orElseThrow(() -> new IOException("Failed to retrieve file info")).getFile();

      validateFileSize(file);
      setResponseHeaders(response, file);
      streamFileContentNIO(teamId, userId, file, response);

      log.info("Successfully downloaded file ({} bytes)", file.getSize());
    } finally {
      MDC.clear();
    }
  }
}
