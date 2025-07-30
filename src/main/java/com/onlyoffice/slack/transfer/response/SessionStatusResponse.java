package com.onlyoffice.slack.transfer.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionStatusResponse {
  private boolean ready;
  private String message;
}
