package com.onlyoffice.slack.transfer;

import static org.junit.jupiter.api.Assertions.*;

import com.onlyoffice.slack.shared.transfer.cache.DocumentSessionKey;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DocumentSessionKeyValidationTests {
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  @Test
  void whenAnyFieldIsBlank_thenValidationFails() {
    var key = DocumentSessionKey.builder().key("").channelId("").messageTs("").build();
    var violations = validator.validate(key);
    assertFalse(violations.isEmpty());
  }

  @Test
  void whenAllFieldsValid_thenValidationPasses() {
    var key = DocumentSessionKey.builder().key("k").channelId("c").messageTs("m").build();
    var violations = validator.validate(key);
    assertTrue(violations.isEmpty());
  }
}
