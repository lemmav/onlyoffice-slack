package com.onlyoffice.slack.transfer;

import static org.junit.jupiter.api.Assertions.*;

import com.onlyoffice.slack.shared.transfer.request.DownloadSessionRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DownloadSessionRequestValidationTests {
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  @Test
  void whenAnyFieldIsBlank_thenValidationFails() {
    var req = DownloadSessionRequest.builder().teamId("").userId("").fileId("").build();
    var violations = validator.validate(req);
    assertFalse(violations.isEmpty());
  }

  @Test
  void whenAllFieldsValid_thenValidationPasses() {
    var req = DownloadSessionRequest.builder().teamId("t").userId("u").fileId("f").build();
    var violations = validator.validate(req);
    assertTrue(violations.isEmpty());
  }
}
