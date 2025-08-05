package com.onlyoffice.slack.domain.document.editor.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.slack.shared.configuration.client.OkHttpClientPoolService;
import com.onlyoffice.slack.shared.transfer.command.DocumentServerCommand;
import com.onlyoffice.slack.shared.transfer.command.DocumentServerTokenCommand;
import com.onlyoffice.slack.shared.transfer.command.DocumentServerVersion;
import com.onlyoffice.slack.shared.transfer.request.SubmitSettingsRequest;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
class DocumentSettingsValidationServiceImpl implements DocumentSettingsValidationService {
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final OkHttpClientPoolService httpClientPoolService;
  private final DocumentJwtManagerService documentJwtManagerService;

  private String sanitizeAddress(final String address) {
    return address.replaceAll("/+$", "");
  }

  private URL validateConnectionAddress(final String address) throws MalformedURLException {
    var url = URI.create(sanitizeAddress(address)).toURL();
    if (url.getProtocol().equalsIgnoreCase("https")) return url;
    throw new MalformedURLException(
        "Received a malformed connection address. Expected to get https protocol");
  }

  private void validateDocumentServerHealth(final URL address) throws IOException {
    var request = new Request.Builder().url("%s/healthcheck".formatted(address)).build();
    try (var okResponse = httpClientPoolService.getHttpClient().newCall(request).execute()) {
      if (!okResponse.isSuccessful() || okResponse.body() == null)
        throw new IOException("Could not validate document server health");
    }
  }

  private void validateDocumentServerVersion(
      final URL address, final String header, final String secret) throws IOException {
    var command = DocumentServerCommand.builder().c("version").build();
    var token = documentJwtManagerService.createToken(command, secret);
    var commandToken = DocumentServerTokenCommand.builder().token(token).build();

    var jsonBody = objectMapper.writeValueAsString(commandToken);
    var request =
        new Request.Builder()
            .url("%s/command?shardkey=%s".formatted(address, UUID.randomUUID()))
            .header(header, token)
            .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
            .build();

    try (var okResponse = httpClientPoolService.getHttpClient().newCall(request).execute()) {
      if (!okResponse.isSuccessful() || okResponse.body() == null)
        throw new IOException("Could not validate document server version");

      var response = objectMapper.readValue(okResponse.body().bytes(), DocumentServerVersion.class);

      if (response.getError() != 0) throw new IOException("Could not receive a non-error response");
    }
  }

  @Override
  @Retry(name = "settings_validation")
  public void validateConnection(@NotNull final SubmitSettingsRequest request) throws IOException {
    try {
      MDC.put("address", request.getAddress());
      MDC.put("header", request.getHeader());
      log.info("Starting document server connection validation");

      if (request.isDemoEnabled() && !request.isValidConfiguration()) {
        log.info("Demo mode enabled without credentials, skipping validation");
        return;
      }

      var url = validateConnectionAddress(request.getAddress());
      log.info("Connection address validated");

      validateDocumentServerHealth(url);
      log.info("Document server health validated");

      validateDocumentServerVersion(url, request.getHeader(), request.getSecret());
      log.info("Document server version validated");

      log.info("Document server connection validation completed successfully");
    } finally {
      MDC.clear();
    }
  }
}
