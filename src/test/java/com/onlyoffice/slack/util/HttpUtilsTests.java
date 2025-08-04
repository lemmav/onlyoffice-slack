package com.onlyoffice.slack.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onlyoffice.slack.shared.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpUtilsTests {
  private HttpUtils httpUtils;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    httpUtils = new HttpUtils();
    request = mock(HttpServletRequest.class);
  }

  @Test
  void whenValidMethod_thenReturnMethodName() {
    when(request.getMethod()).thenReturn("POST");
    assertEquals("POST", httpUtils.getHttpMethod(request));
  }

  @Test
  void whenNullMethod_thenReturnGET() {
    when(request.getMethod()).thenReturn(null);
    assertEquals("GET", httpUtils.getHttpMethod(request));
  }

  @Test
  void whenBlankMethod_thenReturnGET() {
    when(request.getMethod()).thenReturn("");
    assertEquals("GET", httpUtils.getHttpMethod(request));
  }

  @Test
  void whenHeaderPresent_thenReturnHeaderIP() {
    when(request.getHeader(anyString())).thenReturn(null);
    when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.1");
    assertEquals("192.168.1.1", httpUtils.getFirstRequestIP(request));
  }

  @Test
  void whenHeaderWithMultipleIPs_thenReturnFirstIP() {
    when(request.getHeader(anyString())).thenReturn(null);
    when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
    assertEquals("10.0.0.1", httpUtils.getFirstRequestIP(request));
  }

  @Test
  void whenNoHeader_thenReturnRemoteAddr() {
    when(request.getHeader(anyString())).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    assertEquals("127.0.0.1", httpUtils.getFirstRequestIP(request));
  }
}
