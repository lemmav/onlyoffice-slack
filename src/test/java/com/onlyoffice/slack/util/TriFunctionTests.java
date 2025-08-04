package com.onlyoffice.slack.util;

import static org.junit.jupiter.api.Assertions.*;

import com.onlyoffice.slack.shared.utils.TriFunction;
import org.junit.jupiter.api.Test;

class TriFunctionTests {
  @Test
  void whenApply_thenReturnsResult() {
    TriFunction<Integer, Integer, Integer, Integer> sum = (a, b, c) -> a + b + c;
    assertEquals(6, sum.apply(1, 2, 3));
  }

  @Test
  void whenAndThen_thenComposesFunctions() {
    TriFunction<String, String, String, Integer> totalLength =
        (a, b, c) -> a.length() + b.length() + c.length();
    TriFunction<String, String, String, String> describeLength =
        totalLength.andThen(len -> "Total: " + len);
    assertEquals("Total: 8", describeLength.apply("abc", "de", "fgh"));
  }

  @Test
  void whenAndThenWithNullFunction_thenThrowsException() {
    TriFunction<Integer, Integer, Integer, Integer> sum = (a, b, c) -> a + b + c;
    assertThrows(NullPointerException.class, () -> sum.andThen(null));
  }
}
