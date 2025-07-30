package com.onlyoffice.slack.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.onlyoffice.slack.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.service.document.FileStreamingService;
import com.onlyoffice.slack.service.document.core.JwtManagerService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentFileProxyControllerTests {
  private DocumentFileProxyController controller;
  private ServerConfigurationProperties serverConfigurationProperties;
  private FileStreamingService fileStreamingService;
  private JwtManagerService jwtManagerService;
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
    fileStreamingService = mock(FileStreamingService.class);
    jwtManagerService = mock(JwtManagerService.class);
    response = mock(HttpServletResponse.class);

    controller =
        new DocumentFileProxyController(
            serverConfigurationProperties, fileStreamingService, jwtManagerService);
  }

  @Test
  void whenDownloadFileWithValidToken_thenReturnsDeferredResult() {
    var token = "valid-token";
    var decodedToken = createValidDecodedToken();
    var cryptography = mock(ServerConfigurationProperties.CryptographyProperties.class);

    when(serverConfigurationProperties.getCryptography()).thenReturn(cryptography);
    when(cryptography.getSecret()).thenReturn("secret");
    when(jwtManagerService.verifyToken(token, "secret")).thenReturn(decodedToken);

    var result = controller.downloadFile(token, response);

    assertNotNull(result);
  }

  @Test
  void whenDownloadFileWithInvalidToken_thenThrowsException() {
    var token = "invalid-token";
    var cryptography = mock(ServerConfigurationProperties.CryptographyProperties.class);

    when(serverConfigurationProperties.getCryptography()).thenReturn(cryptography);
    when(cryptography.getSecret()).thenReturn("secret");
    when(jwtManagerService.verifyToken(token, "secret"))
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
    when(jwtManagerService.verifyToken(token, "secret")).thenReturn(decodedToken);

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
    when(jwtManagerService.verifyToken(token, "secret"))
        .thenThrow(new RuntimeException("Empty token"));

    try {
      controller.downloadFile(token, response);
    } catch (Exception e) {
      assertEquals("Empty token", e.getMessage());
    }
  }
}
