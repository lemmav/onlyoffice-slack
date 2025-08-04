package com.onlyoffice.slack.shared.filter;

import com.onlyoffice.slack.domain.slack.event.action.SlackFileActionExtractor;
import com.onlyoffice.slack.shared.utils.HttpUtils;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {
  private static final String X_RATE_REMAINING = "X-Ratelimit-Remaining";
  private static final String X_RATE_RESET = "X-Ratelimit-Reset";

  private final Function<HttpMethod, Supplier<BucketConfiguration>> bucketFactory;
  private final SlackFileActionExtractor slackFileActionExtractor;
  private final ProxyManager<String> proxyManager;
  private final HttpUtils httpUtils;

  private final Map<String, Boolean> filterPath = Map.of("/slack/events", true, "/callback", true);

  private void addRateLimitHeaders(
      final HttpServletResponse response, final ConsumptionProbe probe) {
    response.setHeader(X_RATE_REMAINING, String.valueOf(probe.getRemainingTokens()));
    response.setHeader(
        X_RATE_RESET,
        String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));
  }

  private void handleRateLimitExceeded(
      final HttpServletResponse response, final ConsumptionProbe probe) throws IOException {
    response.setContentType("application/json");
    response.setHeader(X_RATE_REMAINING, String.valueOf(probe.getRemainingTokens()));
    response.setHeader(X_RATE_RESET, String.valueOf(probe.getNanosToWaitForRefill()));
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
  }

  @Override
  protected void doFilterInternal(
      @NotNull final HttpServletRequest request,
      @NotNull final HttpServletResponse response,
      @NotNull final FilterChain chain)
      throws ServletException, IOException {
    var method = httpUtils.getHttpMethod(request);
    var clientIdentifier =
        Optional.ofNullable(request.getParameter("session"))
            .map(
                session ->
                    slackFileActionExtractor.extract(session, SlackFileActionExtractor.Type.USER))
            .orElse(httpUtils.getFirstRequestIP(request));
    var bucketConfiguration = bucketFactory.apply(HttpMethod.valueOf(method));
    if (clientIdentifier != null) {
      var bucket =
          proxyManager
              .builder()
              .build(
                  String.format("integration:slack:%s:%s", method, clientIdentifier),
                  bucketConfiguration);
      var probe = bucket.tryConsumeAndReturnRemaining(1);
      if (probe.isConsumed()) {
        addRateLimitHeaders(response, probe);
        chain.doFilter(request, response);
      } else {
        handleRateLimitExceeded(response, probe);
      }
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  protected boolean shouldNotFilter(final HttpServletRequest request) throws ServletException {
    return filterPath.getOrDefault(request.getServletPath(), false);
  }
}
