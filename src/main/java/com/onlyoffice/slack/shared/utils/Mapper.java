package com.onlyoffice.slack.shared.utils;

public interface Mapper<S, T> {
  T map(S source);
}
