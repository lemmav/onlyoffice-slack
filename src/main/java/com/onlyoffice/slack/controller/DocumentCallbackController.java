package com.onlyoffice.slack.controller;

import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.slack.service.document.core.CallbackService;
import com.onlyoffice.slack.transfer.response.CallbackResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DocumentCallbackController {
  private final CallbackService callbackService;

  @PostMapping(path = "/callback")
  public ResponseEntity<CallbackResponse> callback(
      @RequestHeader final Map<String, String> headers, @RequestBody final Callback callback) {
    try {
      callbackService.processCallback(headers, callback);
      return ResponseEntity.ok(CallbackResponse.builder().error(0).build());
    } catch (Exception e) {
      return ResponseEntity.ok(CallbackResponse.builder().error(1).build());
    }
  }
}
