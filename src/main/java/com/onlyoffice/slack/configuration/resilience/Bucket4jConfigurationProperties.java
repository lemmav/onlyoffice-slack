package com.onlyoffice.slack.configuration.resilience;

import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "bucket4j")
public class Bucket4jConfigurationProperties {
  /** Redis connection properties. */
  private RedisProperties redis;

  /** Rate limit properties. */
  private RateLimitProperties rateLimits;

  /** Configuration properties for Redis connection. */
  @Data
  public static class RedisProperties {
    /** The host of the Redis server. */
    private String host;

    /** The port of the Redis server. */
    private int port;

    /** The Redis database index to use. */
    private int database;

    /** The username for authenticating with the Redis server. */
    private String username;

    /** The password for authenticating with the Redis server. */
    private String password;

    /** Indicates whether SSL is enabled for the Redis connection. */
    private boolean ssl;
  }

  /** Configuration properties for rate limiting. */
  @Data
  public static class RateLimitProperties {
    /** A list of rate limit rules, each defined for a specific client or method. */
    private List<ClientRateLimitProperties> limits;

    /** Configuration properties for individual client rate limiting. */
    @Data
    public static class ClientRateLimitProperties {
      /** The HTTP method to which the rate limit applies (e.g., GET, POST). */
      private String method;

      /** The maximum number of tokens available in the rate-limiting bucket. */
      private int capacity;

      /** Refill properties for replenishing tokens in the rate-limiting bucket. */
      private Refill refill;

      /**
       * Configuration properties for rate limiter refill. Specifies how tokens are replenished over
       * time in the rate-limiting bucket.
       */
      @Data
      public static class Refill {
        /** The number of tokens added to the bucket during each refill period. */
        private int tokens;

        /** The duration of the refill period. */
        private int period;

        /** The time unit for the refill period (e.g., SECONDS, MINUTES). */
        private ChronoUnit timeUnit;
      }
    }
  }
}
