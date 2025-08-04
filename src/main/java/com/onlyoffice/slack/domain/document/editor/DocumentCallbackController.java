package com.onlyoffice.slack.domain.document.editor;

import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.slack.domain.document.editor.core.DocumentCallbackService;
import com.onlyoffice.slack.shared.transfer.response.CallbackResponse;
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
  private final DocumentCallbackService documentCallbackService;

  @PostMapping(path = "/callback")
  public ResponseEntity<CallbackResponse> callback(
      @RequestHeader final Map<String, String> headers, @RequestBody final Callback callback) {
    try {
      documentCallbackService.processCallback(headers, callback);
      return ResponseEntity.ok(CallbackResponse.builder().error(0).build());
    } catch (Exception e) {
      return ResponseEntity.ok(CallbackResponse.builder().error(1).build());
    }
  }
}
