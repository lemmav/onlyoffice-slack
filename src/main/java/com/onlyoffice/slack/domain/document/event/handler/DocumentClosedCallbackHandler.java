package com.onlyoffice.slack.domain.document.event.handler;

import com.hazelcast.map.IMap;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Status;
import com.onlyoffice.slack.domain.document.editor.core.DocumentFileKeyExtractor;
import com.onlyoffice.slack.domain.document.event.registry.DocumentCallbackRegistrar;
import com.onlyoffice.slack.shared.transfer.cache.DocumentSessionKey;
import com.onlyoffice.slack.shared.utils.TriFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class DocumentClosedCallbackHandler implements DocumentCallbackRegistrar {
  private final DocumentFileKeyExtractor documentFileKeyExtractor;
  private final IMap<String, DocumentSessionKey> keys;

  @Override
  public Status getStatus() {
    return Status.CLOSED;
  }

  @Override
  public TriFunction<String, String, Callback, Callback> getHandler() {
    return (teamId, userId, callback) -> {
      try {
        var fileId =
            documentFileKeyExtractor.extract(callback.getKey(), DocumentFileKeyExtractor.Type.FILE);

        if (fileId == null) return callback;

        MDC.put("file_id", fileId);

        if (callback.getUsers() == null || callback.getUsers().isEmpty()) {
          log.info("Removing an active session on all users exit for current file");
          keys.remove(fileId);
        }

        return callback;
      } finally {
        MDC.clear();
      }
    };
  }
}
