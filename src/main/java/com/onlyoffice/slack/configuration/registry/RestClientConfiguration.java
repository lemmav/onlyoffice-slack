package com.onlyoffice.slack.configuration.registry;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "onlyoffice.registry.client")
@Getter
@Setter
public class RestClientConfiguration {
    @Value("${onlyoffice.registry.client.timeout.connect:3000}") private int connectTimeout; // Until a connection is established (ms)
    @Value("${onlyoffice.registry.client.timeout.request:2000}") private int requestTimeout;
    @Value("${onlyoffice.registry.client.timeout.socket:2000}") private int socketTimeout; // Timeout for waiting for data (ms)
    @Value("${onlyoffice.registry.client.total_connections:30}") private int maxTotalConnections;
    @Value("${onlyoffice.registry.client.keep_alive:10000}") private int defaultKeepAliveTime;
    @Value("${onlyoffice.registry.client.close_idle:30000}") private int closeIdleConnectionWaitTime;

    private String workspaceType;
    private String resourceServer;
    private String authorizationHeader;
    private String authorizationPrefix;
}
