package com.onlyoffice.slack.handler.action.document;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import com.hazelcast.map.IMap;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.slack.service.document.helper.DocumentFileKeyExtractor;
import com.onlyoffice.slack.transfer.cache.DocumentSessionKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentServerClosedCallbackHandlerTests {
  @Mock private DocumentFileKeyExtractor documentFileKeyExtractor;
  @Mock private IMap<String, DocumentSessionKey> keys;

  private DocumentServerClosedCallbackHandler handler;

  @BeforeEach
  void setUp() {
    handler = new DocumentServerClosedCallbackHandler(documentFileKeyExtractor, keys);
  }

  @Test
  void whenFileIdNull_thenReturnCallback() {
    var callback = mock(Callback.class);

    when(callback.getKey()).thenReturn("key");
    when(documentFileKeyExtractor.extract(anyString(), any())).thenReturn(null);

    var result = handler.getHandler().apply("team", "user", callback);

    assertSame(callback, result);
    verify(keys, never()).remove(any());
  }

  @Test
  void whenUsersEmpty_thenRemoveSessionKey() {
    var callback = mock(Callback.class);
    var fileId = "file";

    when(callback.getKey()).thenReturn("key");
    when(documentFileKeyExtractor.extract(anyString(), any())).thenReturn(fileId);
    when(callback.getUsers()).thenReturn(null);

    var result = handler.getHandler().apply("team", "user", callback);

    assertSame(callback, result);
    verify(keys).remove(fileId);

    reset(keys);
    when(callback.getUsers()).thenReturn(java.util.Collections.emptyList());

    handler.getHandler().apply("team", "user", callback);

    verify(keys).remove(fileId);
  }
}
