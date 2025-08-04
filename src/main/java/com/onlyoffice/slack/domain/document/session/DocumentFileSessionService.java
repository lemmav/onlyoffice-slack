package com.onlyoffice.slack.domain.document.session;

import com.hazelcast.map.MapStore;
import com.onlyoffice.slack.domain.document.session.entity.ActiveFileSession;
import com.onlyoffice.slack.shared.transfer.cache.DocumentSessionKey;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DocumentFileSessionService implements MapStore<String, DocumentSessionKey> {
  @Autowired private DocumentFileSessionRepository documentFileSessionRepository;

  @Override
  @Retry(name = "active_file_session")
  @Transactional(rollbackFor = Exception.class, timeout = 2)
  public void store(String fileId, DocumentSessionKey session) {
    try {
      MDC.put("file_id", fileId);
      MDC.put("channel_id", session.getChannelId());
      MDC.put("message_ts", session.getMessageTs());

      documentFileSessionRepository.save(
          ActiveFileSession.builder()
              .fileId(fileId)
              .key(session.getKey())
              .channelId(session.getChannelId())
              .messageTs(session.getMessageTs())
              .build());

      log.info("Successfully stored a new document session");
    } finally {
      MDC.clear();
    }
  }

  @Override
  @Retry(name = "active_file_session")
  @Transactional(rollbackFor = Exception.class, timeout = 2)
  public void storeAll(Map<String, DocumentSessionKey> map) {
    documentFileSessionRepository.saveAll(
        map.entrySet().stream()
            .map(
                pair ->
                    ActiveFileSession.builder()
                        .fileId(pair.getKey())
                        .key(pair.getValue().getKey())
                        .channelId(pair.getValue().getChannelId())
                        .messageTs(pair.getValue().getMessageTs())
                        .build())
            .toList());

    log.info("Successfully stored {} document sessions", map.size());
  }

  @Override
  @Retry(name = "active_file_session")
  @Transactional(rollbackFor = Exception.class, timeout = 2)
  public void delete(String fileId) {
    try {
      MDC.put("file_id", fileId);
      log.info("Trying to delete a document session");

      documentFileSessionRepository.deleteById(fileId);

      log.info("Successfully deleted a document session");
    } finally {
      MDC.clear();
    }
  }

  @Override
  @Retry(name = "active_file_session")
  @Transactional(rollbackFor = Exception.class, timeout = 2)
  public void deleteAll(Collection<String> ids) {
    log.info("Deleting {} document sessions", ids.size());

    documentFileSessionRepository.deleteAllById(ids);

    log.info("Deleted {} document sessions", ids.size());
  }

  @Override
  @Retry(name = "active_file_session")
  @Transactional(readOnly = true, timeout = 1)
  public DocumentSessionKey load(String fileId) {
    try {
      MDC.put("file_id", fileId);
      log.info("Loading a document session");

      var maybeEntity = documentFileSessionRepository.findById(fileId);
      if (maybeEntity.isPresent()) {
        var entity = maybeEntity.get();
        return DocumentSessionKey.builder()
            .key(entity.getKey())
            .channelId(entity.getChannelId())
            .messageTs(entity.getMessageTs())
            .build();
      }

      log.info("Document session not found");

      return null;
    } finally {
      MDC.clear();
    }
  }

  @Override
  @Retry(name = "active_file_session")
  @Transactional(readOnly = true, timeout = 2)
  public Map<String, DocumentSessionKey> loadAll(Collection<String> keys) {
    log.info("Loading document sessions");

    var result = new HashMap<String, DocumentSessionKey>();
    var entities = documentFileSessionRepository.findAllById(keys);

    for (var entity : entities) {
      var activity =
          DocumentSessionKey.builder()
              .key(entity.getKey())
              .channelId(entity.getChannelId())
              .messageTs(entity.getMessageTs())
              .build();
      result.put(entity.getFileId(), activity);
    }

    log.info("Successfully loaded {} document sessions", result.size());

    return result;
  }

  @Override
  public Iterable<String> loadAllKeys() {
    return List.of();
  }
}
