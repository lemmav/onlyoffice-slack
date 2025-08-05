package com.onlyoffice.slack.domain.document.event.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.domain.document.editor.core.DocumentFileKeyExtractor;
import com.onlyoffice.slack.domain.slack.installation.RotatingInstallationService;
import com.onlyoffice.slack.shared.configuration.client.OkHttpClientPoolService;
import com.onlyoffice.slack.shared.exception.domain.DocumentCallbackException;
import com.onlyoffice.slack.shared.transfer.cache.DocumentSessionKey;
import com.slack.api.bolt.App;
import com.slack.api.bolt.model.Installer;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.files.FilesInfoRequest;
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
  @Mock private RotatingInstallationService installationService;
  @Mock private IMap<String, DocumentSessionKey> keys;
  @Mock private App app;

  private DocumentSaveCallbackHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new DocumentSaveCallbackHandler(
            documentFileKeyExtractor, installationService, httpClientPoolService, keys, app);
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

    var slackClient = mock(MethodsClient.class);
    try {
      when(slackClient.filesInfo(any(FilesInfoRequest.class))).thenReturn(filesInfoResponse);
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
