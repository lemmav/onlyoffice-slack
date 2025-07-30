package com.onlyoffice.slack.handler.action.document;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.configuration.client.OkHttpClientPoolService;
import com.onlyoffice.slack.exception.DocumentCallbackException;
import com.onlyoffice.slack.service.data.InstallationService;
import com.onlyoffice.slack.service.document.helper.DocumentFileKeyExtractor;
import com.onlyoffice.slack.transfer.cache.DocumentSessionKey;
import com.slack.api.bolt.App;
import com.slack.api.bolt.model.Installer;
import com.slack.api.methods.response.files.FilesInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentServerSaveCallbackHandlerTests {
  @Mock private DocumentFileKeyExtractor documentFileKeyExtractor;
  @Mock private OkHttpClientPoolService httpClientPoolService;
  @Mock private InstallationService installationService;
  @Mock private IMap<String, DocumentSessionKey> keys;
  @Mock private App app;

  private DocumentServerSaveCallbackHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new DocumentServerSaveCallbackHandler(
            documentFileKeyExtractor, httpClientPoolService, installationService, keys, app);
  }

  @Test
  void whenFileIdNull_thenThrowException() {
    var callback = mock(com.onlyoffice.model.documenteditor.Callback.class);
    when(callback.getKey()).thenReturn("some-key");
    when(documentFileKeyExtractor.extract(anyString(), any())).thenReturn(null);

    var ex =
        assertThrows(
            DocumentCallbackException.class,
            () -> handler.getHandler().apply("team", "user", callback));

    assertTrue(ex.getMessage().contains("Could not extract fileId"));
  }

  @Test
  void whenInstallerNotFound_thenThrowException() {
    var callback = mock(com.onlyoffice.model.documenteditor.Callback.class);

    when(callback.getKey()).thenReturn("some-key");
    when(documentFileKeyExtractor.extract(anyString(), any())).thenReturn("fileId");
    when(installationService.findInstallerWithRotation(any(), any(), any())).thenReturn(null);

    var ex =
        assertThrows(
            DocumentCallbackException.class,
            () -> handler.getHandler().apply("team", "user", callback));

    assertTrue(ex.getMessage().contains("Could not find an installer"));
  }

  @Test
  void whenFileInfoNotOk_thenThrowException() {
    var callback = mock(com.onlyoffice.model.documenteditor.Callback.class);

    when(callback.getKey()).thenReturn("some-key");
    when(documentFileKeyExtractor.extract(anyString(), any())).thenReturn("fileId");

    var installer = mock(Installer.class);

    when(installer.getInstallerUserAccessToken()).thenReturn("token");
    when(installationService.findInstallerWithRotation(any(), any(), any())).thenReturn(installer);

    var filesInfoResponse = mock(FilesInfoResponse.class);

    when(filesInfoResponse.isOk()).thenReturn(false);

    var slackClient = mock(com.slack.api.methods.MethodsClient.class);
    try {
      when(slackClient.filesInfo(any(com.slack.api.methods.request.files.FilesInfoRequest.class)))
          .thenReturn(filesInfoResponse);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    when(app.getClient()).thenReturn(slackClient);

    var ex =
        assertThrows(
            DocumentCallbackException.class,
            () -> handler.getHandler().apply("team", "user", callback));

    assertTrue(ex.getMessage().contains("Failed to get file info"));
  }
}
