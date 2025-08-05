package com.onlyoffice.slack.domain.slack.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.slack.api.bolt.App;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstallationWebServletTests {
  private InstallationWebServlet controller;

  @BeforeEach
  void setUp() {
    var app = mock(App.class);
    controller = new InstallationWebServlet(app);
  }

  @Test
  void whenControllerCreated_thenInstanceIsNotNull() {
    assertNotNull(controller);
  }

  @Test
  void whenControllerCreated_thenExtendsSlackOAuthAppServlet() {
    assertNotNull(controller);
    assertEquals("InstallationWebServlet", controller.getClass().getSimpleName());
  }
}
