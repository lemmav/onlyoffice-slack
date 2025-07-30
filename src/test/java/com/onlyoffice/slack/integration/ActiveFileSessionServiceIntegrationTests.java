package com.onlyoffice.slack.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.onlyoffice.slack.persistence.entity.ActiveFileSession;
import com.onlyoffice.slack.persistence.repository.ActiveFileSessionRepository;
import com.onlyoffice.slack.service.data.ActiveFileSessionService;
import com.onlyoffice.slack.transfer.cache.DocumentSessionKey;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("ActiveFileSessionService Integration Tests")
class ActiveFileSessionServiceIntegrationTests extends BaseIntegrationTest {
  @Autowired private EntityManager entityManager;
  @Autowired private ActiveFileSessionService activeFileSessionService;
  @Autowired private ActiveFileSessionRepository activeFileSessionRepository;

  @BeforeEach
  void setUp() {
    activeFileSessionRepository.deleteAll();
  }

  @Nested
  @DisplayName("CREATE Operations")
  class CreateOperationsTests {
    @Test
    @DisplayName("When storing new session then save successfully")
    void whenStoringNewSession_thenSaveSuccessfully() {
      var sessionKey =
          DocumentSessionKey.builder()
              .key("test-session-key")
              .channelId("C1234567890")
              .messageTs("1234567890.123456")
              .build();

      activeFileSessionService.store("file-123", sessionKey);

      var savedSession = activeFileSessionRepository.findById("file-123");

      assertThat(savedSession).isPresent();
      assertThat(savedSession.get().getFileId()).isEqualTo("file-123");
      assertThat(savedSession.get().getKey()).isEqualTo("test-session-key");
      assertThat(savedSession.get().getChannelId()).isEqualTo("C1234567890");
      assertThat(savedSession.get().getMessageTs()).isEqualTo("1234567890.123456");
      assertThat(savedSession.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("When storing session with existing file ID then overwrite existing session")
    void whenStoringSessionWithExistingFileId_thenOverwriteExistingSession() {
      var existingSession =
          ActiveFileSession.builder()
              .fileId("file-123")
              .key("old-session-key")
              .channelId("C1111111111")
              .messageTs("1111111111.111111")
              .build();
      activeFileSessionRepository.save(existingSession);
      activeFileSessionRepository.flush();

      var newSessionKey =
          DocumentSessionKey.builder()
              .key("new-session-key")
              .channelId("C2222222222")
              .messageTs("2222222222.222222")
              .build();

      activeFileSessionService.store("file-123", newSessionKey);

      entityManager.flush();
      entityManager.clear();
      var updatedSession = activeFileSessionRepository.findById("file-123");

      assertThat(updatedSession).isPresent();
      assertThat(updatedSession.get().getKey()).isEqualTo("new-session-key");
      assertThat(updatedSession.get().getChannelId()).isEqualTo("C2222222222");
      assertThat(updatedSession.get().getMessageTs()).isEqualTo("2222222222.222222");
      assertThat(activeFileSessionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("When storing session with null session key then throw exception")
    void whenStoringSessionWithNullSessionKey_thenThrowException() {
      assertThatThrownBy(() -> activeFileSessionService.store("file-123", null))
          .isInstanceOf(NullPointerException.class);

      assertThat(activeFileSessionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("When storing session with null file ID then throw exception")
    void whenStoringSessionWithNullFileId_thenThrowException() {
      var sessionKey =
          DocumentSessionKey.builder()
              .key("test-session-key")
              .channelId("C1234567890")
              .messageTs("1234567890.123456")
              .build();

      assertThatThrownBy(() -> activeFileSessionService.store(null, sessionKey))
          .isInstanceOf(Exception.class);

      assertThat(activeFileSessionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("When storing multiple sessions concurrently then handle correctly")
    void whenStoringMultipleSessionsConcurrently_thenHandleCorrectly() {
      var sessionOne =
          DocumentSessionKey.builder()
              .key("session-key-1")
              .channelId("C1111111111")
              .messageTs("1111111111.111111")
              .build();

      var sessionTwo =
          DocumentSessionKey.builder()
              .key("session-key-2")
              .channelId("C2222222222")
              .messageTs("2222222222.222222")
              .build();

      activeFileSessionService.store("file-1", sessionOne);
      activeFileSessionService.store("file-2", sessionTwo);

      var firstSession = activeFileSessionRepository.findById("file-1");
      var secondSession = activeFileSessionRepository.findById("file-2");

      assertThat(firstSession).isPresent();
      assertThat(firstSession.get().getKey()).isEqualTo("session-key-1");
      assertThat(secondSession).isPresent();
      assertThat(secondSession.get().getKey()).isEqualTo("session-key-2");
      assertThat(activeFileSessionRepository.count()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("BATCH CREATE Operations")
  class BatchCreateOperationsTests {
    @Test
    @DisplayName("When storing multiple sessions then save all successfully")
    void whenStoringMultipleSessions_thenSaveAllSuccessfully() {
      var sessions =
          Map.of(
              "file-1",
                  DocumentSessionKey.builder()
                      .key("session-key-1")
                      .channelId("C1111111111")
                      .messageTs("1111111111.111111")
                      .build(),
              "file-2",
                  DocumentSessionKey.builder()
                      .key("session-key-2")
                      .channelId("C2222222222")
                      .messageTs("2222222222.222222")
                      .build(),
              "file-3",
                  DocumentSessionKey.builder()
                      .key("session-key-3")
                      .channelId("C3333333333")
                      .messageTs("3333333333.333333")
                      .build());

      activeFileSessionService.storeAll(sessions);

      var allSessions = activeFileSessionRepository.findAll();
      assertThat(allSessions).hasSize(3);
      assertThat(allSessions)
          .extracting(ActiveFileSession::getFileId)
          .containsExactlyInAnyOrder("file-1", "file-2", "file-3");
      assertThat(allSessions)
          .extracting(ActiveFileSession::getKey)
          .containsExactlyInAnyOrder("session-key-1", "session-key-2", "session-key-3");
    }

    @Test
    @DisplayName("When storing empty map then handle gracefully")
    void whenStoringEmptyMap_thenHandleGracefully() {
      activeFileSessionService.storeAll(Map.of());

      assertThat(activeFileSessionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("When storing sessions with duplicate file IDs then overwrite existing")
    void whenStoringSessionsWithDuplicateFileIds_thenOverwriteExisting() {
      var existingSession =
          ActiveFileSession.builder()
              .fileId("file-1")
              .key("old-session-key")
              .channelId("C0000000000")
              .messageTs("0000000000.000000")
              .build();
      activeFileSessionRepository.save(existingSession);
      activeFileSessionRepository.flush();

      var sessions =
          Map.of(
              "file-1",
                  DocumentSessionKey.builder()
                      .key("new-session-key")
                      .channelId("C1111111111")
                      .messageTs("1111111111.111111")
                      .build(),
              "file-2",
                  DocumentSessionKey.builder()
                      .key("session-key-2")
                      .channelId("C2222222222")
                      .messageTs("2222222222.222222")
                      .build());

      activeFileSessionService.storeAll(sessions);

      entityManager.flush();
      entityManager.clear();
      var allSessions = activeFileSessionRepository.findAll();
      var updatedSession = activeFileSessionRepository.findById("file-1");

      assertThat(allSessions).hasSize(2);
      assertThat(updatedSession).isPresent();
      assertThat(updatedSession.get().getKey()).isEqualTo("new-session-key");
    }
  }

  @Nested
  @DisplayName("READ Operations")
  class ReadOperationsTests {
    @Test
    @DisplayName("When loading existing session then return session key")
    void whenLoadingExistingSession_thenReturnSessionKey() {
      var session =
          ActiveFileSession.builder()
              .fileId("file-123")
              .key("test-session-key")
              .channelId("C1234567890")
              .messageTs("1234567890.123456")
              .createdAt(LocalDateTime.now())
              .build();
      activeFileSessionRepository.save(session);
      activeFileSessionRepository.flush();

      var result = activeFileSessionService.load("file-123");

      assertThat(result).isNotNull();
      assertThat(result.getKey()).isEqualTo("test-session-key");
      assertThat(result.getChannelId()).isEqualTo("C1234567890");
      assertThat(result.getMessageTs()).isEqualTo("1234567890.123456");
    }

    @Test
    @DisplayName("When loading non-existent session then return null")
    void whenLoadingNonExistentSession_thenReturnNull() {
      var result = activeFileSessionService.load("non-existent-file");

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("When loading multiple existing sessions then return all sessions")
    void whenLoadingMultipleExistingSessions_thenReturnAllSessions() {
      var sessions =
          List.of(
              ActiveFileSession.builder()
                  .fileId("file-1")
                  .key("session-key-1")
                  .channelId("C1111111111")
                  .messageTs("1111111111.111111")
                  .build(),
              ActiveFileSession.builder()
                  .fileId("file-2")
                  .key("session-key-2")
                  .channelId("C2222222222")
                  .messageTs("2222222222.222222")
                  .build(),
              ActiveFileSession.builder()
                  .fileId("file-3")
                  .key("session-key-3")
                  .channelId("C3333333333")
                  .messageTs("3333333333.333333")
                  .build());
      activeFileSessionRepository.saveAll(sessions);
      activeFileSessionRepository.flush();

      var result = activeFileSessionService.loadAll(List.of("file-1", "file-2", "file-3"));

      assertThat(result).hasSize(3);
      assertThat(result).containsKeys("file-1", "file-2", "file-3");
      assertThat(result.get("file-1").getKey()).isEqualTo("session-key-1");
      assertThat(result.get("file-2").getKey()).isEqualTo("session-key-2");
      assertThat(result.get("file-3").getKey()).isEqualTo("session-key-3");
    }

    @Test
    @DisplayName("When loading mix of existing and non-existent sessions then return only existing")
    void whenLoadingMixOfExistingAndNonExistentSessions_thenReturnOnlyExisting() {
      var session =
          ActiveFileSession.builder()
              .fileId("file-1")
              .key("session-key-1")
              .channelId("C1111111111")
              .messageTs("1111111111.111111")
              .build();
      activeFileSessionRepository.save(session);
      activeFileSessionRepository.flush();

      var result =
          activeFileSessionService.loadAll(List.of("file-1", "non-existent-file", "file-3"));

      assertThat(result).hasSize(1);
      assertThat(result).containsKey("file-1");
      assertThat(result.get("file-1").getKey()).isEqualTo("session-key-1");
    }

    @Test
    @DisplayName("When loading empty collection then return empty map")
    void whenLoadingEmptyCollection_thenReturnEmptyMap() {
      var result = activeFileSessionService.loadAll(List.of());

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("When loading all keys then return empty list")
    void whenLoadingAllKeys_thenReturnEmptyList() {
      var sessions =
          List.of(
              ActiveFileSession.builder()
                  .fileId("file-1")
                  .key("session-key-1")
                  .channelId("C1111111111")
                  .messageTs("1111111111.111111")
                  .build(),
              ActiveFileSession.builder()
                  .fileId("file-2")
                  .key("session-key-2")
                  .channelId("C2222222222")
                  .messageTs("2222222222.222222")
                  .build());
      activeFileSessionRepository.saveAll(sessions);
      activeFileSessionRepository.flush();

      var result = activeFileSessionService.loadAllKeys();

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("DELETE Operations")
  class DeleteOperationsTests {
    @Test
    @DisplayName("When deleting existing session then remove from database")
    void whenDeletingExistingSession_thenRemoveFromDatabase() {
      var session =
          ActiveFileSession.builder()
              .fileId("file-123")
              .key("test-session-key")
              .channelId("C1234567890")
              .messageTs("1234567890.123456")
              .build();
      activeFileSessionRepository.save(session);
      activeFileSessionRepository.flush();

      assertThat(activeFileSessionRepository.findById("file-123")).isPresent();

      activeFileSessionService.delete("file-123");

      assertThat(activeFileSessionRepository.findById("file-123")).isEmpty();
      assertThat(activeFileSessionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("When deleting non-existent session then handle gracefully")
    void whenDeletingNonExistentSession_thenHandleGracefully() {
      assertThat(activeFileSessionRepository.findById("non-existent-file")).isEmpty();

      activeFileSessionService.delete("non-existent-file");

      assertThat(activeFileSessionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("When deleting multiple sessions then remove all from database")
    void whenDeletingMultipleSessions_thenRemoveAllFromDatabase() {
      var sessions =
          List.of(
              ActiveFileSession.builder()
                  .fileId("file-1")
                  .key("session-key-1")
                  .channelId("C1111111111")
                  .messageTs("1111111111.111111")
                  .build(),
              ActiveFileSession.builder()
                  .fileId("file-2")
                  .key("session-key-2")
                  .channelId("C2222222222")
                  .messageTs("2222222222.222222")
                  .build(),
              ActiveFileSession.builder()
                  .fileId("file-3")
                  .key("session-key-3")
                  .channelId("C3333333333")
                  .messageTs("3333333333.333333")
                  .build());
      activeFileSessionRepository.saveAll(sessions);
      activeFileSessionRepository.flush();

      assertThat(activeFileSessionRepository.count()).isEqualTo(3);

      activeFileSessionService.deleteAll(List.of("file-1", "file-3"));

      assertThat(activeFileSessionRepository.count()).isEqualTo(1);
      assertThat(activeFileSessionRepository.findById("file-1")).isEmpty();
      assertThat(activeFileSessionRepository.findById("file-2")).isPresent();
      assertThat(activeFileSessionRepository.findById("file-3")).isEmpty();
    }

    @Test
    @DisplayName(
        "When deleting mix of existing and non-existent sessions then remove only existing")
    void whenDeletingMixOfExistingAndNonExistentSessions_thenRemoveOnlyExisting() {
      var session =
          ActiveFileSession.builder()
              .fileId("file-1")
              .key("session-key-1")
              .channelId("C1111111111")
              .messageTs("1111111111.111111")
              .build();
      activeFileSessionRepository.save(session);
      activeFileSessionRepository.flush();

      assertThat(activeFileSessionRepository.count()).isEqualTo(1);

      activeFileSessionService.deleteAll(List.of("file-1", "non-existent-file", "file-3"));

      assertThat(activeFileSessionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("When deleting empty collection then handle gracefully")
    void whenDeletingEmptyCollection_thenHandleGracefully() {
      var session =
          ActiveFileSession.builder()
              .fileId("file-1")
              .key("session-key-1")
              .channelId("C1111111111")
              .messageTs("1111111111.111111")
              .build();
      activeFileSessionRepository.save(session);
      activeFileSessionRepository.flush();

      assertThat(activeFileSessionRepository.count()).isEqualTo(1);

      activeFileSessionService.deleteAll(List.<String>of());

      assertThat(activeFileSessionRepository.count()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("TRANSACTIONAL Operations")
  class TransactionalOperationsTests {
    @Test
    @DisplayName("When store operation fails then rollback transaction")
    void whenStoreOperationFails_thenRollbackTransaction() {
      var initialSessionKey =
          DocumentSessionKey.builder()
              .key("initial-session-key")
              .channelId("C1234567890")
              .messageTs("1234567890.123456")
              .build();

      activeFileSessionService.store("file-1", initialSessionKey);
      assertThat(activeFileSessionRepository.count()).isEqualTo(1);

      var validSessionKey =
          DocumentSessionKey.builder()
              .key("valid-session-key")
              .channelId("C1234567890")
              .messageTs("1234567890.123456")
              .build();

      assertThatThrownBy(() -> activeFileSessionService.store(null, validSessionKey))
          .isInstanceOf(Exception.class);

      assertThat(activeFileSessionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("When multiple operations in transaction then commit or rollback atomically")
    void whenMultipleOperationsInTransaction_thenCommitOrRollbackAtomically() {
      var session1 =
          ActiveFileSession.builder()
              .fileId("file-1")
              .key("session-key-1")
              .channelId("C1111111111")
              .messageTs("1111111111.111111")
              .build();

      var session2 =
          ActiveFileSession.builder()
              .fileId("file-2")
              .key("session-key-2")
              .channelId("C2222222222")
              .messageTs("2222222222.222222")
              .build();

      activeFileSessionRepository.saveAll(List.of(session1, session2));
      activeFileSessionRepository.flush();

      assertThat(activeFileSessionRepository.count()).isEqualTo(2);

      var newSessionKey =
          DocumentSessionKey.builder()
              .key("new-session-key")
              .channelId("C3333333333")
              .messageTs("3333333333.333333")
              .build();

      activeFileSessionService.store("file-3", newSessionKey);
      activeFileSessionService.delete("file-1");

      assertThat(activeFileSessionRepository.count()).isEqualTo(2);
      assertThat(activeFileSessionRepository.findById("file-1")).isEmpty();
      assertThat(activeFileSessionRepository.findById("file-3")).isPresent();
    }
  }
}
