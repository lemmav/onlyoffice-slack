package com.onlyoffice.slack.shared.filter;

import com.onlyoffice.slack.shared.configuration.message.MessageSourceSlackConfiguration;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Integer.MIN_VALUE)
public class GlobalRateLimiterFilter extends OncePerRequestFilter {
  private final MessageSourceSlackConfiguration messageSourceSlackConfiguration;
  private final MessageSource messageSource;

  private final RateLimiter globalRateLimiter;

  @Autowired
  public GlobalRateLimiterFilter(
      MessageSource messageSource,
      MessageSourceSlackConfiguration messageSourceSlackConfiguration) {
    this(
        RateLimiter.of(
            "global",
            RateLimiterConfig.custom()
                .limitForPeriod(2500)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build()),
        messageSource,
        messageSourceSlackConfiguration);
  }

  public GlobalRateLimiterFilter(
      RateLimiter rateLimiter,
      MessageSource messageSource,
      MessageSourceSlackConfiguration messageSourceSlackConfiguration) {
    this.globalRateLimiter = rateLimiter;
    this.messageSource = messageSource;
    this.messageSourceSlackConfiguration = messageSourceSlackConfiguration;
  }

  @Override
  protected void doFilterInternal(
      @NotNull final HttpServletRequest request,
      @NotNull final HttpServletResponse response,
      @NotNull final FilterChain chain)
      throws ServletException, IOException {
    if (!globalRateLimiter.acquirePermission()) {
      response.setStatus(429);
      request.setAttribute(
          "title",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorRateLimiterTitle(), null, Locale.ENGLISH));
      request.setAttribute(
          "text",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorRateLimiterText(), null, Locale.ENGLISH));
      request.setAttribute(
          "action",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorRateLimiterButton(), null, Locale.ENGLISH));
      request.getRequestDispatcher("/error").forward(request, response);
      return;
    }

    chain.doFilter(request, response);
  }
}
