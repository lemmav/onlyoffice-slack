package com.onlyoffice.slack.shared.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "server")
public class ServerConfigurationProperties {
  private String baseAddress;
  private CryptographyProperties cryptography = new CryptographyProperties();
  private DemoProperties demo = new DemoProperties();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CryptographyProperties {
    private String secret;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DemoProperties {
    private String address;
    private String header;
    private String secret;
    private int durationDays = 7;
  }
}
