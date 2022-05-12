package com.onlyoffice.slack.configuration.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.admin.client.Keycloak;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RestTemplateConfigurer {
    private final RestClientConfiguration clientConfiguration;
    private final Keycloak instance;
    private final CloseableHttpClient httpClient;
    private final RestTemplateErrorHandlerConfigurer responseErrorHandler;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder(
                rt -> rt.getInterceptors().add((request, body, execution) -> {
                    request
                            .getHeaders()
                            .add(
                                    clientConfiguration.getAuthorizationHeader().strip(),
                                    String.format("%s %s", clientConfiguration.getAuthorizationPrefix().strip(), instance.tokenManager()
                                            .getAccessTokenString())
                            );
                    try {
                        log.debug("{} request {}", request.getMethod().toString(), request.getURI());
                        return execution.execute(request, body);
                    } catch (IOException e) {
                        log.error(e.getMessage());
                        return new ClientHttpResponse() {
                            public HttpStatus getStatusCode() {
                                return HttpStatus.SERVICE_UNAVAILABLE;
                            }

                            public int getRawStatusCode() {
                                return 503;
                            }

                            public String getStatusText()  {
                                return HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase();
                            }

                            public void close() {}

                            public InputStream getBody() {
                                return null;
                            }

                            public HttpHeaders getHeaders() {
                                return new HttpHeaders();
                            }
                        };
                    }
                }))
                .requestFactory(() -> clientHttpRequestFactory())
                .errorHandler(this.responseErrorHandler)
                .build();
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(httpClient);
        return clientHttpRequestFactory;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("poolScheduler");
        scheduler.setPoolSize(50);
        return scheduler;
    }
}
