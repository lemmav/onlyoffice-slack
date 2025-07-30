package com.onlyoffice.slack.transfer.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitSettingsRequest {
  @URL private String address;
  private String header;
  private String secret;
  private boolean demoEnabled;

  public boolean isCredentialsComplete() {
    return address != null
        && !address.trim().isEmpty()
        && header != null
        && !header.trim().isEmpty()
        && secret != null
        && !secret.trim().isEmpty();
  }

  public boolean isValidConfiguration() {
    return demoEnabled || isCredentialsComplete();
  }
}
