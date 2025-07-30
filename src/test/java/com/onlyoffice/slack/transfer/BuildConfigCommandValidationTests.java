package com.onlyoffice.slack.transfer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.slack.transfer.command.BuildConfigCommand;
import com.slack.api.model.File;
import com.slack.api.model.User;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BuildConfigCommandValidationTests {
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  @Test
  void whenRequiredFieldsAreNullOrBlank_thenValidationFails() {
    var cmd =
        BuildConfigCommand.builder()
            .channelId("")
            .messageTs(null)
            .signingSecret("")
            .user(null)
            .file(null)
            .mode(null)
            .type(null)
            .build();
    var violations = validator.validate(cmd);
    assertFalse(violations.isEmpty());
  }

  @Test
  void whenAllFieldsValid_thenValidationPasses() {
    var cmd =
        BuildConfigCommand.builder()
            .channelId("channel")
            .messageTs("ts")
            .signingSecret("secret")
            .user(mock(User.class))
            .file(mock(File.class))
            .mode(mock(Mode.class))
            .type(mock(Type.class))
            .build();
    var violations = validator.validate(cmd);
    assertTrue(violations.isEmpty());
  }
}
