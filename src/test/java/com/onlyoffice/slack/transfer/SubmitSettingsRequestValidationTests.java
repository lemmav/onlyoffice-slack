package com.onlyoffice.slack.transfer;

import static org.junit.jupiter.api.Assertions.*;

import com.onlyoffice.slack.shared.transfer.request.SubmitSettingsRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SubmitSettingsRequestValidationTests {
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  @Test
  void whenAddressIsInvalidUrl_thenValidationFails() {
    var req = SubmitSettingsRequest.builder().address("not-a-url").build();
    var violations = validator.validate(req);
    assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("address")));
  }

  @Test
  void whenAddressIsValidUrl_thenValidationPasses() {
    var req = SubmitSettingsRequest.builder().address("https://example.com").build();
    var violations = validator.validate(req);
    assertTrue(violations.isEmpty());
  }

  @Test
  void whenCredentialsComplete_returnsTrueWhenAllFieldsPresent() {
    var req =
        SubmitSettingsRequest.builder()
            .address("https://example.com")
            .header("header")
            .secret("secret")
            .build();
    assertTrue(req.isCredentialsComplete());
  }

  @Test
  void whenCredentialsComplete_returnsFalseWhenAnyFieldMissing() {
    var req = SubmitSettingsRequest.builder().address("").header("header").secret("secret").build();
    assertFalse(req.isCredentialsComplete());
  }

  @Test
  void whenValidConfiguration_returnsTrueIfDemoEnabled() {
    var req = SubmitSettingsRequest.builder().demoEnabled(true).build();
    assertTrue(req.isValidConfiguration());
  }

  @Test
  void whenValidConfiguration_returnsTrueIfCredentialsComplete() {
    var req =
        SubmitSettingsRequest.builder()
            .address("https://example.com")
            .header("header")
            .secret("secret")
            .demoEnabled(false)
            .build();
    assertTrue(req.isValidConfiguration());
  }

  @Test
  void whenValidConfiguration_returnsFalseIfDemoDisabledAndCredentialsIncomplete() {
    var req =
        SubmitSettingsRequest.builder()
            .address("")
            .header("")
            .secret("")
            .demoEnabled(false)
            .build();
    assertFalse(req.isValidConfiguration());
  }
}
