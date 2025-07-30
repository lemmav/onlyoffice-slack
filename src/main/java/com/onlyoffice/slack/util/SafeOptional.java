package com.onlyoffice.slack.util;

import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SafeOptional {
  public static <T> Optional<T> of(final CheckedSupplier<T> supplier) {
    try {
      return Optional.ofNullable(supplier.get());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @FunctionalInterface
  public interface CheckedSupplier<T> {
    T get() throws Exception;
  }
}
