package com.onlyoffice.slack.shared.configuration.resilience;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
@RequiredArgsConstructor
public class Bucket4jConfiguration {
  private final Bucket4jConfigurationProperties bucket4jConfiguration;

  /**
   * Creates and configures a Redis client based on the application properties. The Redis client is
   * used for establishing connections to the Redis server for distributed rate limiting.
   *
   * @return The configured {@link RedisClient}.
   */
  @Bean
  public RedisClient redisClient() {
    return RedisClient.create(
        RedisURI.builder()
            .withHost(bucket4jConfiguration.getRedis().getHost())
            .withPort(bucket4jConfiguration.getRedis().getPort())
            .withDatabase(bucket4jConfiguration.getRedis().getDatabase())
            .withSsl(bucket4jConfiguration.getRedis().isSsl())
            .withAuthentication(
                bucket4jConfiguration.getRedis().getUsername(),
                bucket4jConfiguration.getRedis().getPassword())
            .build());
  }

  /**
   * Creates and configures a Lettuce-based proxy manager for managing distributed rate limits. The
   * proxy manager uses Redis to persist rate-limiting data and supports expiration strategies.
   *
   * @param redisClient The Redis client used to establish a connection to the Redis server.
   * @return The configured {@link LettuceBasedProxyManager} instance for distributed rate limiting.
   */
  @Bean
  public LettuceBasedProxyManager<String> lettuceBasedProxyManager(final RedisClient redisClient) {
    var redisConnection =
        redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    return LettuceBasedProxyManager.builderFor(redisConnection)
        .withExpirationStrategy(
            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                Duration.ofMinutes(1L)))
        .build();
  }

  /**
   * Creates a supplier for Bucket4j configurations based on the application-defined rate limits.
   * This supplier provides Bucket configurations for each HTTP method based on the defined rate
   * limit properties.
   *
   * @return A {@link Function} mapping {@link HttpMethod} to a {@link Supplier} of {@link
   *     BucketConfiguration}.
   * @throws Exception If no configuration for the GET method is found, an exception is thrown.
   */
  @Bean
  public Function<HttpMethod, Supplier<BucketConfiguration>> bucketConfiguration()
      throws Exception {
    List<Bucket4jConfigurationProperties.RateLimitProperties.ClientRateLimitProperties>
        rateLimitProperties = bucket4jConfiguration.getRateLimits().getLimits();
    Bucket4jConfigurationProperties.RateLimitProperties.ClientRateLimitProperties getLimits =
        bucket4jConfiguration.getRateLimits().getLimits().stream()
            .filter(props -> props.getMethod().equalsIgnoreCase(HttpMethod.GET.name()))
            .findFirst()
            .orElseThrow(() -> new Exception("Could not initialize rate-limiter configuration"));
    return (HttpMethod method) -> {
      var config =
          rateLimitProperties.stream()
              .filter(props -> props.getMethod().equalsIgnoreCase(method.name()))
              .findFirst()
              .orElse(getLimits);
      return () ->
          BucketConfiguration.builder()
              .addLimit(
                  Bandwidth.classic(
                      config.getCapacity(),
                      Refill.greedy(
                          config.getRefill().getTokens(),
                          Duration.of(
                              config.getRefill().getPeriod(), config.getRefill().getTimeUnit()))))
              .build();
    };
  }
}
