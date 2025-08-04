package com.onlyoffice.slack.shared.transfer.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionStatusResponse {
  private boolean ready;
}
