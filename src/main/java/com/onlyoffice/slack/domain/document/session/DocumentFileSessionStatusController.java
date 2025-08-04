package com.onlyoffice.slack.domain.document.session;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.shared.transfer.cache.EditorSession;
import com.onlyoffice.slack.shared.transfer.response.SessionStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DocumentFileSessionStatusController {
  private final IMap<String, EditorSession> sessions;

  @ResponseBody
  @GetMapping(path = "/editor/status")
  public ResponseEntity<SessionStatusResponse> checkSessionStatus(
      @RequestParam("session") final String sessionId) {
    var storedSession = sessions.get(sessionId);
    var ready = storedSession != null;
    return ResponseEntity.ok(SessionStatusResponse.builder().ready(ready).build());
  }
}
