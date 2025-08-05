package com.onlyoffice.slack.shared.filter;

import static org.mockito.Mockito.*;

import com.onlyoffice.slack.shared.configuration.message.MessageSourceSlackConfiguration;
import io.github.resilience4j.ratelimiter.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

class GlobalRateLimiterFilterTests {
  private GlobalRateLimiterFilter filter;
  private RateLimiter rateLimiter;

  private FilterChain chain;
  private HttpServletRequest request;
  private HttpServletResponse response;

  @BeforeEach
  void setUp() {
    rateLimiter = mock(RateLimiter.class);

    var messageSource = mock(MessageSource.class);
    var messageSourceSlackConfiguration = mock(MessageSourceSlackConfiguration.class);

    filter =
        new GlobalRateLimiterFilter(rateLimiter, messageSource, messageSourceSlackConfiguration);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    chain = mock(FilterChain.class);
  }

  @Test
  void whenPermissionAcquired_thenFilterChainContinues() throws ServletException, IOException {
    when(rateLimiter.acquirePermission()).thenReturn(true);

    filter.doFilter(request, response, chain);

    verify(chain, times(1)).doFilter(request, response);
  }

  @Test
  void whenPermissionNotAcquired_thenSetsResponseStatus() throws ServletException, IOException {
    when(rateLimiter.acquirePermission()).thenReturn(false);
    when(request.getRequestDispatcher("/error")).thenReturn(mock(RequestDispatcher.class));

    filter.doFilter(request, response, chain);

    verify(response, times(1)).setStatus(429);
    verify(chain, never()).doFilter(request, response);
  }
}
