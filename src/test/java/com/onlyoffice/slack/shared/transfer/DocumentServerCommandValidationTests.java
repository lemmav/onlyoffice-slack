package com.onlyoffice.slack.shared.transfer;

import static org.junit.jupiter.api.Assertions.*;

import com.onlyoffice.slack.shared.transfer.command.DocumentServerCommand;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DocumentServerCommandValidationTests {
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  @Test
  void whenCommandIsBlank_thenValidationFails() {
    var cmd = DocumentServerCommand.builder().c("").build();
    var violations = validator.validate(cmd);
    assertFalse(violations.isEmpty());
  }

  @Test
  void whenCommandIsNull_thenValidationFails() {
    var cmd = DocumentServerCommand.builder().c(null).build();
    var violations = validator.validate(cmd);
    assertFalse(violations.isEmpty());
  }

  @Test
  void whenCommandIsNotBlank_thenValidationPasses() {
    var cmd = DocumentServerCommand.builder().c("valid").build();
    var violations = validator.validate(cmd);
    assertTrue(violations.isEmpty());
  }
}
