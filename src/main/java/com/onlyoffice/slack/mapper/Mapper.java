package com.onlyoffice.slack.mapper;

public interface Mapper<S, T> {
  T map(S source);
}
