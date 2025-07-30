package com.onlyoffice.slack.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.slack.service.document.core.CallbackService;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DocumentCallbackControllerTests {
  private DocumentCallbackController controller;
  private CallbackService callbackService;

  @BeforeEach
  void setUp() {
    callbackService = mock(CallbackService.class);
    controller = new DocumentCallbackController(callbackService);
  }

  @Test
  void whenCallbackProcessedSuccessfully_thenReturnsSuccessResponse() {
    var headers = new HashMap<String, String>();
    headers.put("Authorization", "Bearer token");

    var callback = mock(Callback.class);

    var result = controller.callback(headers, callback);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(0, result.getBody().getError());
    verify(callbackService).processCallback(headers, callback);
  }

  @Test
  void whenCallbackProcessingThrowsException_thenReturnsErrorResponse() {
    var headers = new HashMap<String, String>();

    var callback = mock(Callback.class);

    doThrow(new RuntimeException("Processing failed"))
        .when(callbackService)
        .processCallback(headers, callback);

    var result = controller.callback(headers, callback);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, result.getBody().getError());
    verify(callbackService).processCallback(headers, callback);
  }

  @Test
  void whenCallbackWithEmptyHeaders_thenStillProcessesCallback() {
    var emptyHeaders = new HashMap<String, String>();

    var callback = mock(Callback.class);

    var result = controller.callback(emptyHeaders, callback);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(0, result.getBody().getError());
    verify(callbackService).processCallback(emptyHeaders, callback);
  }
}
