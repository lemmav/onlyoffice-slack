package com.onlyoffice.slack.service.document.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.slack.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.exception.DocumentCallbackException;
import com.onlyoffice.slack.registry.DocumentServerCallbackRegistry;
import com.onlyoffice.slack.service.data.TeamSettingsService;
import com.onlyoffice.slack.service.document.helper.DocumentFileKeyExtractor;
import com.onlyoffice.slack.transfer.response.SettingsResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CallbackServiceTests {
  @Mock ServerConfigurationProperties serverConfigurationProperties;
  @Mock DocumentFileKeyExtractor documentFileKeyExtractor;
  @Mock DocumentServerCallbackRegistry callbackRegistry;
  @Mock JwtManagerService jwtManagerService;
  @Mock TeamSettingsService settingsService;
  @InjectMocks CallbackService callbackService;

  @Test
  void whenProcessCallbackWithMissingTeamId_thenWarnAndReturn() {
    var callback = new Callback();
    callback.setKey("key");

    when(documentFileKeyExtractor.extract(anyString(), eq(DocumentFileKeyExtractor.Type.TEAM)))
        .thenReturn("");

    callbackService.processCallback(Map.of(), callback);

    verify(documentFileKeyExtractor).extract(anyString(), eq(DocumentFileKeyExtractor.Type.TEAM));
  }

  @Test
  void whenProcessCallbackWithMissingUserId_thenWarnAndReturn() {
    var callback = new Callback();
    callback.setKey("key");

    when(documentFileKeyExtractor.extract(anyString(), eq(DocumentFileKeyExtractor.Type.TEAM)))
        .thenReturn("team");
    when(documentFileKeyExtractor.extract(anyString(), eq(DocumentFileKeyExtractor.Type.USER)))
        .thenReturn("");

    callbackService.processCallback(Map.of(), callback);

    verify(documentFileKeyExtractor).extract(anyString(), eq(DocumentFileKeyExtractor.Type.USER));
  }

  @Test
  void whenProcessCallbackWithInvalidToken_thenThrowException() {
    var callback = new Callback();
    callback.setKey("key");
    callback.setToken("invalid");

    when(documentFileKeyExtractor.extract(anyString(), eq(DocumentFileKeyExtractor.Type.TEAM)))
        .thenReturn("team");
    when(documentFileKeyExtractor.extract(anyString(), eq(DocumentFileKeyExtractor.Type.USER)))
        .thenReturn("user");

    var settings =
        SettingsResponse.builder().demoEnabled(false).header("header").secret("secret").build();

    when(settingsService.findSettings(anyString())).thenReturn(settings);
    when(jwtManagerService.verifyToken(anyString(), anyString()))
        .thenThrow(DocumentCallbackException.class);
    assertThrows(
        DocumentCallbackException.class, () -> callbackService.processCallback(Map.of(), callback));
  }
}
