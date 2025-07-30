package com.onlyoffice.slack.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SlackInstallationControllerTests {
  private SlackInstallationController controller;

  @BeforeEach
  void setUp() {
    controller = new SlackInstallationController();
  }

  @Test
  void whenCompletionCalled_thenReturnsCompletionView() {
    var result = controller.completion();

    assertEquals("completion", result);
  }

  @Test
  void whenCancellationCalled_thenReturnsCancellationView() {
    var result = controller.cancellation();

    assertEquals("cancellation", result);
  }
}
