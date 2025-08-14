package com.onlyoffice.slack.domain.document.session;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.shared.transfer.cache.DocumentSessionKey;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentFileSessionScheduler {
  private final DocumentFileSessionRepository repository;
  private final IMap<String, DocumentSessionKey> keys;

  @Scheduled(cron = "0 0/15 * * * *")
  public void removeStaleSessions() {
    var ids =
        repository.findLastStaleRecords(LocalDateTime.now().minusDays(7)).stream()
            .collect(Collectors.toMap(s -> s, s -> s));
    keys.removeAll((map) -> ids.containsKey(map.getKey()));
  }
}
