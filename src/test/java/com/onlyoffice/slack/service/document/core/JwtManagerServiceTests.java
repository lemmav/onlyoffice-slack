package com.onlyoffice.slack.service.document.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onlyoffice.slack.configuration.document.DocumentServerConfigurationProperties;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtManagerServiceTests {
  private JwtManagerService jwtManagerService;

  @BeforeEach
  void setUp() {
    var properties = mock(DocumentServerConfigurationProperties.class);
    var jwtProps = mock(DocumentServerConfigurationProperties.JwtProperties.class);

    when(properties.getJwt()).thenReturn(jwtProps);
    when(jwtProps.getKeepAliveMinutes()).thenReturn(10);
    when(jwtProps.getAcceptableLeewaySeconds()).thenReturn(5);

    jwtManagerService = new JwtManagerService(properties);
  }

  @Test
  void whenCreateTokenWithValidInput_thenTokenIsCreated() {
    var payload = Map.of("foo", "bar");
    var token = jwtManagerService.createToken(payload, "secret");

    assertNotNull(token);
  }

  @Test
  void whenVerifyTokenWithValidToken_thenReturnPayload() {
    var payload = Map.of("foo", "bar");
    var token = jwtManagerService.createToken(payload, "secret");
    var result = jwtManagerService.verifyToken(token, "secret");

    assertEquals("bar", result.get("foo"));
  }

  @Test
  void whenVerifyTokenWithInvalidToken_thenThrowException() {
    assertThrows(Exception.class, () -> jwtManagerService.verifyToken("invalid.token", "secret"));
  }
}
