package com.onlyoffice.slack.service.document.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.slack.domain.document.editor.core.*;
import com.onlyoffice.slack.domain.document.event.registry.DocumentCallbackRegistry;
import com.onlyoffice.slack.domain.slack.settings.TeamSettingsService;
import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.shared.exception.domain.DocumentCallbackException;
import com.onlyoffice.slack.shared.transfer.response.SettingsResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentCallbackServiceTests {
  @Mock ServerConfigurationProperties serverConfigurationProperties;

  @Mock DocumentFileKeyExtractor documentFileKeyExtractor;
  @Mock DocumentCallbackRegistry documentCallbackRegistry;

  @Mock DocumentJwtManagerService documentJwtManagerService;
  @Mock TeamSettingsService settingsService;
  @InjectMocks DocumentCallbackServiceImpl documentCallbackService;

  @Test
  void whenProcessCallbackWithMissingTeamId_thenWarnAndReturn() {
    var callback = new Callback();
    callback.setKey("key");

    when(documentFileKeyExtractor.extract(anyString(), eq(DocumentFileKeyExtractor.Type.TEAM)))
        .thenReturn("");

    documentCallbackService.processCallback(Map.of(), callback);

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

    documentCallbackService.processCallback(Map.of(), callback);

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
    when(documentJwtManagerService.verifyToken(anyString(), anyString()))
        .thenThrow(DocumentCallbackException.class);
    assertThrows(
        DocumentCallbackException.class,
        () -> documentCallbackService.processCallback(Map.of(), callback));
  }
}
