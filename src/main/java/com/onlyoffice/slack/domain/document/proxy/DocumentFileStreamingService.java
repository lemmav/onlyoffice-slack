package com.onlyoffice.slack.domain.document.proxy;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;

public interface DocumentFileStreamingService {
  void processDownloadAsync(
      @NotBlank final String teamId,
      @NotBlank final String userId,
      @NotBlank final String fileId,
      @NotNull final HttpServletResponse response)
      throws IOException;
}
