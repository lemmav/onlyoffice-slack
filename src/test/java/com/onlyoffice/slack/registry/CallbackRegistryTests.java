package com.onlyoffice.slack.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Status;
import com.onlyoffice.slack.domain.document.event.registry.DocumentCallbackRegistry;
import com.onlyoffice.slack.shared.utils.TriFunction;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class CallbackRegistryTests {
  @Test
  void whenRegisterAndFindHandler_thenReturnsHandler() {
    var registry = new DocumentCallbackRegistry(Collections.emptyList());
    @SuppressWarnings("unchecked")
    TriFunction<String, String, Callback, Callback> handler = mock(TriFunction.class);

    registry.register(Status.SAVE, handler);
    var found = registry.find(Status.SAVE);

    assertTrue(found.isPresent());
    assertEquals(handler, found.get());
  }

  @Test
  void whenRegisterDuplicateHandler_thenDoesNotOverwrite() {
    var registry = new DocumentCallbackRegistry(Collections.emptyList());
    @SuppressWarnings("unchecked")
    TriFunction<String, String, Callback, Callback> firstHandler = mock(TriFunction.class);
    @SuppressWarnings("unchecked")
    TriFunction<String, String, Callback, Callback> secondHandler = mock(TriFunction.class);

    registry.register(Status.SAVE, firstHandler);
    registry.register(Status.SAVE, secondHandler);
    var found = registry.find(Status.SAVE);

    assertTrue(found.isPresent());
    assertEquals(firstHandler, found.get());
  }
}
