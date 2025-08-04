package com.onlyoffice.slack.shared.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
public class HttpUtils {
  private static final String[] IP_HEADERS = {
    "X-Remote-Ip-Address",
    "X-Real-IP",
    "X-Forwarded-For",
    "X-Forwarded-Host",
    "Proxy-Client-IP",
    "WL-Proxy-Client-IP",
    "HTTP_X_FORWARDED_FOR",
    "HTTP_X_FORWARDED",
    "HTTP_X_CLUSTER_CLIENT_IP",
    "HTTP_CLIENT_IP",
    "HTTP_FORWARDED_FOR",
    "HTTP_FORWARDED",
    "HTTP_VIA",
    "REMOTE_ADDR"
  };

  /**
   * Retrieves the HTTP method from the request with a fallback to "GET" for invalid methods.
   *
   * <p>This method attempts to determine the HTTP method used in the request. If the method is not
   * specified or is invalid, it defaults to "GET".
   *
   * @param request The HttpServletRequest object from which to extract the HTTP method.
   * @return The HTTP method as a string, defaults to "GET" if the method is invalid or not
   *     specified.
   */
  public String getHttpMethod(@NotNull final HttpServletRequest request) {
    var method = request.getMethod();
    if (method == null || method.isBlank()) return "GET";

    try {
      return HttpMethod.valueOf(method).name();
    } catch (IllegalArgumentException e) {
      return "GET";
    }
  }

  /**
   * Retrieves the first IP address from the request headers.
   *
   * @param request HttpServletRequest object
   * @return The first IP address found in the request headers, or the remote address if none found
   */
  public String getFirstRequestIP(@NotNull final HttpServletRequest request) {
    for (var header : IP_HEADERS) {
      var value = request.getHeader(header);
      if (value != null && !value.isEmpty()) return value.split("\\s*,\\s*")[0];
    }

    return request.getRemoteAddr();
  }
}
