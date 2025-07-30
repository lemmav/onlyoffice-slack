package com.onlyoffice.slack.configuration.client;

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
@ConfigurationProperties(prefix = "http.client")
public class HttpClientConfigurationProperties {
  private int connectTimeoutSeconds = 15;
  private int readTimeoutSeconds = 60;
  private int writeTimeoutSeconds = 60;
  private int callTimeoutSeconds = 120;
  private int maxIdleConnections = 20;
  private int keepAliveDurationMinutes = 5;
  private long maxResponseSizeBytes = 25 * 1024 * 1024;
  private boolean retryOnConnectionFailure = true;
  private boolean followRedirects = false;
  private boolean followSslRedirects = false;
}
