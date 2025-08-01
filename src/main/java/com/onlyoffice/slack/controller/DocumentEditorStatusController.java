package com.onlyoffice.slack.controller;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.handler.action.slack.SlackFileActionExtractor;
import com.onlyoffice.slack.transfer.cache.EditorSession;
import com.onlyoffice.slack.transfer.response.SessionStatusResponse;
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
public class DocumentEditorStatusController {
  private final SlackFileActionExtractor slackFileActionExtractor;
  private final IMap<String, EditorSession> sessions;

  @ResponseBody
  @GetMapping(path = "/editor/status")
  public ResponseEntity<SessionStatusResponse> checkSessionStatus(
      @RequestParam("session") final String sessionId) {
    var session =
        slackFileActionExtractor.extract(sessionId, SlackFileActionExtractor.Type.SESSION);
    var storedSession = sessions.get(session);
    var ready = storedSession != null;
    var message = ready ? "Session is ready" : "Session not found in cache";
    return ResponseEntity.ok(SessionStatusResponse.builder().ready(ready).message(message).build());
  }
}
