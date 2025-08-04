package com.onlyoffice.slack.registry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onlyoffice.slack.domain.slack.event.registry.SlackBlockActionRegistry;
import com.slack.api.bolt.handler.builtin.BlockActionHandler;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SlackBlockActionRegistryTests {
  @Test
  void whenRegisteringHandler_thenHandlerCanBeFound() {
    var registry = new SlackBlockActionRegistry(Collections.emptyList());
    var handler = mock(BlockActionHandler.class);

    registry.register("firstCallback", handler);
    var found = registry.find("firstCallback");

    assertTrue(found.isPresent());
    assertEquals(handler, found.get());
  }

  @Test
  void whenRegisteringDuplicateHandler_thenOriginalHandlerIsNotOverwritten() {
    var registry = new SlackBlockActionRegistry(Collections.emptyList());
    var firstHandler = mock(BlockActionHandler.class);
    var secondHandler = mock(BlockActionHandler.class);

    registry.register("firstCallback", firstHandler);
    registry.register("secondCallback", secondHandler);
    var found = registry.find("firstCallback");

    assertTrue(found.isPresent());
    assertEquals(firstHandler, found.get());
  }

  @Test
  void whenFindingUnregisteredHandler_thenEmptyIsReturned() {
    var registry = new SlackBlockActionRegistry(Collections.emptyList());
    var found = registry.find("not_registered");

    assertTrue(found.isEmpty());
  }
}
