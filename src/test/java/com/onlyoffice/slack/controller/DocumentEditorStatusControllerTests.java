package com.onlyoffice.slack.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.transfer.cache.EditorSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DocumentEditorStatusControllerTests {
  private DocumentEditorStatusController controller;
  private IMap<String, EditorSession> sessions;

  @BeforeEach
  void setUp() {
    sessions = mock(IMap.class);
    controller = new DocumentEditorStatusController(sessions);
  }

  @Test
  void whenSessionExists_thenReturnsReadyStatus() {
    var sessionId = "session-id";
    var session = mock(EditorSession.class);

    when(sessions.get(sessionId)).thenReturn(session);

    var result = controller.checkSessionStatus(sessionId);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertTrue(result.getBody().isReady());
    assertEquals("Session is ready", result.getBody().getMessage());
  }

  @Test
  void whenSessionDoesNotExist_thenReturnsNotReadyStatus() {
    var sessionId = "bad-session-id";

    when(sessions.get(sessionId)).thenReturn(null);

    var result = controller.checkSessionStatus(sessionId);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertFalse(result.getBody().isReady());
    assertEquals("Session not found in cache", result.getBody().getMessage());
  }
}
