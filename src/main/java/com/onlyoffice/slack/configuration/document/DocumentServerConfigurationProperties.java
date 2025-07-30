package com.onlyoffice.slack.configuration.document;

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
@ConfigurationProperties(prefix = "server.documentserver")
public class DocumentServerConfigurationProperties {
  private JwtProperties jwt = new JwtProperties();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class JwtProperties {
    private int keepAliveMinutes = 5;
    private int acceptableLeewaySeconds = 10;
  }
}
