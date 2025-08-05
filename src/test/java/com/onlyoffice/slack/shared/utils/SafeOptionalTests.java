package com.onlyoffice.slack.shared.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SafeOptionalTests {
  @Test
  void whenSupplierReturnsValue_thenOptionalOfValue() {
    var result = SafeOptional.of(() -> "value");
    assertTrue(result.isPresent());
    assertEquals("value", result.get());
  }

  @Test
  void whenSupplierReturnsNull_thenOptionalEmpty() {
    var result = SafeOptional.of(() -> null);
    assertTrue(result.isEmpty());
  }

  @Test
  void whenSupplierThrowsException_thenOptionalEmpty() {
    var result =
        SafeOptional.of(
            () -> {
              throw new Exception("exception");
            });
    assertTrue(result.isEmpty());
  }
}
