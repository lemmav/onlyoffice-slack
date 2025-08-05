package com.onlyoffice.slack.shared.transfer;

import static org.junit.jupiter.api.Assertions.*;

import com.onlyoffice.slack.shared.transfer.cache.EditorSession;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EditorSessionValidationTests {
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  @Test
  void whenAnyFieldIsBlank_thenValidationFails() {
    var session =
        EditorSession.builder()
            .teamId("")
            .userId("")
            .userName("")
            .fileId("")
            .fileName("")
            .channelId("")
            .messageTs("")
            .build();
    var violations = validator.validate(session);
    assertFalse(violations.isEmpty());
  }

  @Test
  void whenAllFieldsValid_thenValidationPasses() {
    var session =
        EditorSession.builder()
            .teamId("t")
            .userId("u")
            .userName("n")
            .fileId("f")
            .fileName("fn")
            .channelId("c")
            .messageTs("m")
            .build();
    var violations = validator.validate(session);
    assertTrue(violations.isEmpty());
  }
}
