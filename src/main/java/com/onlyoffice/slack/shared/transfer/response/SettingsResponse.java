package com.onlyoffice.slack.shared.transfer.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsResponse {
  private String address;
  private String header;
  private String secret;
  private boolean demoEnabled;
  private LocalDateTime demoStartedDate;
}
