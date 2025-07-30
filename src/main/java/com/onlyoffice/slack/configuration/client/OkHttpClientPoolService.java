package com.onlyoffice.slack.configuration.client;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OkHttpClientPoolService {
  private final HttpClientConfigurationProperties httpClientProperties;

  private volatile OkHttpClient httpClient;

  public OkHttpClient getHttpClient() {
    if (httpClient == null) {
      synchronized (this) {
        if (httpClient == null) httpClient = createHttpClient();
      }
    }

    return httpClient;
  }

  private OkHttpClient createHttpClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(httpClientProperties.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
        .readTimeout(httpClientProperties.getReadTimeoutSeconds(), TimeUnit.SECONDS)
        .writeTimeout(httpClientProperties.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
        .callTimeout(httpClientProperties.getCallTimeoutSeconds(), TimeUnit.SECONDS)
        .connectionPool(
            new ConnectionPool(
                httpClientProperties.getMaxIdleConnections(),
                httpClientProperties.getKeepAliveDurationMinutes(),
                TimeUnit.MINUTES))
        .retryOnConnectionFailure(httpClientProperties.isRetryOnConnectionFailure())
        .followRedirects(httpClientProperties.isFollowRedirects())
        .followSslRedirects(httpClientProperties.isFollowSslRedirects())
        .addNetworkInterceptor(
            chain -> {
              var response = chain.proceed(chain.request());
              if (response.body() != null) {
                var contentLength = response.body().contentLength();
                if (contentLength > httpClientProperties.getMaxResponseSizeBytes()) {
                  response.close();
                  throw new IOException(
                      "Response size ("
                          + contentLength
                          + " bytes) exceeds maximum allowed ("
                          + httpClientProperties.getMaxResponseSizeBytes()
                          + " bytes)");
                }
              }
              return response;
            })
        .build();
  }
}
