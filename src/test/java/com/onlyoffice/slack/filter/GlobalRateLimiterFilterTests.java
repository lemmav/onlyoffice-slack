package com.onlyoffice.slack.filter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.onlyoffice.slack.exception.GlobalRateLimiterException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalRateLimiterFilterTests {
  private GlobalRateLimiterFilter filter;
  private RateLimiter rateLimiter;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    rateLimiter = mock(RateLimiter.class);
    filter = new GlobalRateLimiterFilter(rateLimiter);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    chain = mock(FilterChain.class);
  }

  @Test
  void whenPermissionAcquired_thenFilterChainContinues() throws ServletException, IOException {
    when(rateLimiter.acquirePermission()).thenReturn(true);

    filter.doFilterInternal(request, response, chain);

    verify(chain, times(1)).doFilter(request, response);
  }

  @Test
  void whenPermissionNotAcquired_thenThrowsException() {
    when(rateLimiter.acquirePermission()).thenReturn(false);
    assertThrows(
        GlobalRateLimiterException.class, () -> filter.doFilterInternal(request, response, chain));
  }
}
