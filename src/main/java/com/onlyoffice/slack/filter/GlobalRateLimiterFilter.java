package com.onlyoffice.slack.filter;

import com.onlyoffice.slack.exception.GlobalRateLimiterException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Integer.MIN_VALUE)
public class GlobalRateLimiterFilter extends OncePerRequestFilter {
  private final RateLimiter globalRateLimiter;

  public GlobalRateLimiterFilter() {
    this(
        RateLimiter.of(
            "global",
            RateLimiterConfig.custom()
                .limitForPeriod(5000)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build()));
  }

  public GlobalRateLimiterFilter(RateLimiter rateLimiter) {
    this.globalRateLimiter = rateLimiter;
  }

  @Override
  protected void doFilterInternal(
      @NotNull final HttpServletRequest request,
      @NotNull final HttpServletResponse response,
      @NotNull final FilterChain chain)
      throws ServletException, IOException {
    if (!globalRateLimiter.acquirePermission())
      throw new GlobalRateLimiterException("Service is busy. Please try again later");
    chain.doFilter(request, response);
  }
}
