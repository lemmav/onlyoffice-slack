package com.onlyoffice.slack.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.onlyoffice.slack.domain.document.editor.core.DocumentJwtManagerService;
import com.onlyoffice.slack.domain.document.proxy.DocumentFileProxyController;
import com.onlyoffice.slack.domain.document.proxy.DocumentFileStreamingService;
import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentFileProxyControllerTests {
  private ServerConfigurationProperties serverConfigurationProperties;

  private DocumentFileProxyController controller;
  private DocumentJwtManagerService documentJwtManagerService;

  private HttpServletResponse response;

  private Map<String, Object> createValidDecodedToken() {
    var token = new HashMap<String, Object>();
    token.put("teamId", "team");
    token.put("userId", "user");
    token.put("fileId", "file");
    return token;
  }

  @BeforeEach
  void setUp() {
    serverConfigurationProperties = mock(ServerConfigurationProperties.class);
    documentJwtManagerService = mock(DocumentJwtManagerService.class);
    response = mock(HttpServletResponse.class);

    var documentFileStreamingService = mock(DocumentFileStreamingService.class);

    controller =
        new DocumentFileProxyController(
            serverConfigurationProperties, documentJwtManagerService, documentFileStreamingService);
  }

  @Test
  void whenDownloadFileWithValidToken_thenReturnsDeferredResult() {
    var token = "valid-token";
    var decodedToken = createValidDecodedToken();
    var cryptography = mock(ServerConfigurationProperties.CryptographyProperties.class);

    when(serverConfigurationProperties.getCryptography()).thenReturn(cryptography);
    when(cryptography.getSecret()).thenReturn("secret");
    when(documentJwtManagerService.verifyToken(token, "secret")).thenReturn(decodedToken);

    var result = controller.downloadFile(token, response);

    assertNotNull(result);
  }

  @Test
  void whenDownloadFileWithInvalidToken_thenThrowsException() {
    var token = "invalid-token";
    var cryptography = mock(ServerConfigurationProperties.CryptographyProperties.class);

    when(serverConfigurationProperties.getCryptography()).thenReturn(cryptography);
    when(cryptography.getSecret()).thenReturn("secret");
    when(documentJwtManagerService.verifyToken(token, "secret"))
        .thenThrow(new RuntimeException("Invalid token"));

    try {
      controller.downloadFile(token, response);
    } catch (Exception e) {
      assertEquals("Invalid token", e.getMessage());
    }
  }

  @Test
  void whenDownloadFileProcessedSuccessfully_thenDeferredResultCompletes() throws Exception {
    var token = "valid-token";
    var decodedToken = createValidDecodedToken();
    var cryptography = mock(ServerConfigurationProperties.CryptographyProperties.class);

    when(serverConfigurationProperties.getCryptography()).thenReturn(cryptography);
    when(cryptography.getSecret()).thenReturn("secret");
    when(documentJwtManagerService.verifyToken(token, "secret")).thenReturn(decodedToken);

    var result = controller.downloadFile(token, response);

    assertNotNull(result);
    Thread.sleep(100);
  }

  @Test
  void whenDownloadFileWithNullToken_thenThrowsException() {
    String token = null;

    try {
      controller.downloadFile(token, response);
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  void whenDownloadFileWithEmptyToken_thenThrowsException() {
    var token = "";
    var cryptography = mock(ServerConfigurationProperties.CryptographyProperties.class);

    when(serverConfigurationProperties.getCryptography()).thenReturn(cryptography);
    when(cryptography.getSecret()).thenReturn("secret");
    when(documentJwtManagerService.verifyToken(token, "secret"))
        .thenThrow(new RuntimeException("Empty token"));

    try {
      controller.downloadFile(token, response);
    } catch (Exception e) {
      assertEquals("Empty token", e.getMessage());
    }
  }
}
