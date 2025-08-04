package com.onlyoffice.slack.transfer;

import static org.junit.jupiter.api.Assertions.*;

import com.onlyoffice.slack.shared.transfer.command.DocumentServerTokenCommand;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DocumentServerTokenCommandValidationTests {
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  @Test
  void whenTokenIsBlank_thenValidationFails() {
    var cmd = DocumentServerTokenCommand.builder().token("").build();
    var violations = validator.validate(cmd);
    assertFalse(violations.isEmpty());
  }

  @Test
  void whenTokenIsNull_thenValidationFails() {
    var cmd = DocumentServerTokenCommand.builder().token(null).build();
    var violations = validator.validate(cmd);
    assertFalse(violations.isEmpty());
  }

  @Test
  void whenTokenIsNotBlank_thenValidationPasses() {
    var cmd = DocumentServerTokenCommand.builder().token("valid").build();
    var violations = validator.validate(cmd);
    assertTrue(violations.isEmpty());
  }
}
