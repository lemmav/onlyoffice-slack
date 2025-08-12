package com.onlyoffice.slack.shared.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "server")
public class ServerConfigurationProperties {
  @NotBlank private String baseAddress;
  private CryptographyProperties cryptography = new CryptographyProperties();
  private DemoProperties demo = new DemoProperties();

  @Data
  @Builder
  @Validated
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CryptographyProperties {
    @NotBlank private String secret;
  }

  @Data
  @Builder
  @Validated
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DemoProperties {
    @NotBlank private String address;
    @NotBlank private String header;
    @NotBlank private String secret;
    private int durationDays = 7;
  }
}
