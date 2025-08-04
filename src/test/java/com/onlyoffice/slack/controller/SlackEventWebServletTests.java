package com.onlyoffice.slack.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.onlyoffice.slack.domain.slack.event.SlackEventWebServlet;
import com.slack.api.bolt.App;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SlackEventWebServletTests {
  private SlackEventWebServlet controller;

  @BeforeEach
  void setUp() {
    var app = mock(App.class);
    controller = new SlackEventWebServlet(app);
  }

  @Test
  void whenControllerCreated_thenInstanceIsNotNull() {
    assertNotNull(controller);
  }

  @Test
  void whenControllerCreated_thenExtendsSlackAppServlet() {
    assertNotNull(controller);
    assertEquals("SlackEventWebServlet", controller.getClass().getSimpleName());
  }
}
