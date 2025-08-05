package com.onlyoffice.slack.domain.document.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.slack.domain.document.editor.core.DocumentJwtManagerService;
import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.shared.transfer.request.DownloadSessionRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@RestController(value = "/files")
class DocumentFileProxyController {
  private static final ExecutorService VIRTUAL_THREAD_EXECUTOR =
      Executors.newVirtualThreadPerTaskExecutor();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final ServerConfigurationProperties serverConfigurationProperties;

  private final DocumentJwtManagerService documentJwtManagerService;
  private final DocumentFileStreamingService fileStreamingService;

  private DownloadSessionRequest decodeDownloadToken(final String token) {
    var decoded =
        documentJwtManagerService.verifyToken(
            token, serverConfigurationProperties.getCryptography().getSecret());
    return OBJECT_MAPPER.convertValue(decoded, DownloadSessionRequest.class);
  }

  private DeferredResult<Void> createDeferredResult() {
    DeferredResult<Void> deferredResult = new DeferredResult<>(30_000L);
    deferredResult.onTimeout(
        () ->
            deferredResult.setErrorResult(
                new ResponseStatusException(
                    HttpStatus.REQUEST_TIMEOUT, "Download request timed out")));
    deferredResult.onCompletion(() -> log.debug("Download completed for file"));
    deferredResult.onError(throwable -> log.error("Download error for file", throwable));
    return deferredResult;
  }

  private void runDownloadAsync(
      final DownloadSessionRequest downloadToken,
      final HttpServletResponse response,
      final DeferredResult<Void> deferredResult) {
    CompletableFuture.runAsync(
        () -> {
          try {
            fileStreamingService.processDownloadAsync(
                downloadToken.getTeamId(),
                downloadToken.getUserId(),
                downloadToken.getFileId(),
                response);
            deferredResult.setResult(null);
          } catch (Exception e) {
            deferredResult.setErrorResult(e);
          }
        },
        VIRTUAL_THREAD_EXECUTOR);
  }

  @GetMapping("/download/{token}")
  public DeferredResult<Void> downloadFile(
      @PathVariable final String token, final HttpServletResponse response) {
    try {
      var downloadToken = decodeDownloadToken(token);

      MDC.put("team_id", downloadToken.getTeamId());
      MDC.put("user_id", downloadToken.getUserId());
      MDC.put("file_id", downloadToken.getFileId());

      log.info("Initializing file download");

      var deferredResult = createDeferredResult();
      runDownloadAsync(downloadToken, response, deferredResult);

      return deferredResult;
    } finally {
      MDC.clear();
    }
  }
}
