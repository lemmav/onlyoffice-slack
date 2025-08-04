package com.onlyoffice.slack.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.domain.slack.state.HazelcastStateService;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("HazelcastOAuthStateService Integration Tests")
class HazelcastStateServiceIntegrationTests extends BaseIntegrationTest {
  @MockitoBean private IMap<String, Instant> states;

  private HazelcastStateService oAuthStateService;

  @BeforeEach
  void setUp() {
    oAuthStateService = new HazelcastStateService(states);
  }

  @Nested
  @DisplayName("CREATE Operations")
  class CreateOperationsTests {
    @Test
    @DisplayName("When adding new state to datastore then store successfully")
    void whenAddingNewStateToDatastore_thenStoreSuccessfully() {
      var state = "state";

      when(states.putIfAbsent(eq(state), any(Instant.class))).thenReturn(null);

      oAuthStateService.addNewStateToDatastore(state);

      verify(states).putIfAbsent(eq(state), any(Instant.class));
    }

    @Test
    @DisplayName("When adding state that already exists then do not overwrite")
    void whenAddingStateThatAlreadyExists_thenDoNotOverwrite() {
      var state = "state";
      var existingTimestamp = Instant.now().minusSeconds(60);

      when(states.putIfAbsent(eq(state), any(Instant.class))).thenReturn(existingTimestamp);

      oAuthStateService.addNewStateToDatastore(state);

      verify(states).putIfAbsent(eq(state), any(Instant.class));
    }

    @Test
    @DisplayName("When adding null state then handle gracefully")
    void whenAddingNullState_thenHandleGracefully() {
      when(states.putIfAbsent(eq(null), any(Instant.class))).thenReturn(null);

      oAuthStateService.addNewStateToDatastore(null);

      verify(states).putIfAbsent(eq(null), any(Instant.class));
    }

    @Test
    @DisplayName("When adding empty state then handle gracefully")
    void whenAddingEmptyState_thenHandleGracefully() {
      var state = "";

      when(states.putIfAbsent(eq(state), any(Instant.class))).thenReturn(null);

      oAuthStateService.addNewStateToDatastore(state);

      verify(states).putIfAbsent(eq(state), any(Instant.class));
    }

    @Test
    @DisplayName("When datastore operation throws exception then propagate exception")
    void whenDatastoreOperationThrowsException_thenPropagateException() {
      var state = "state";

      when(states.putIfAbsent(eq(state), any(Instant.class)))
          .thenThrow(new RuntimeException("Hazelcast connection failed"));

      assertThatThrownBy(() -> oAuthStateService.addNewStateToDatastore(state))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Hazelcast connection failed");
    }

    @Test
    @DisplayName("When adding multiple different states then store all successfully")
    void whenAddingMultipleDifferentStates_thenStoreAllSuccessfully() {
      var state1 = "state-1";
      var state2 = "state-2";
      var state3 = "state-3";

      when(states.putIfAbsent(eq(state1), any(Instant.class))).thenReturn(null);
      when(states.putIfAbsent(eq(state2), any(Instant.class))).thenReturn(null);
      when(states.putIfAbsent(eq(state3), any(Instant.class))).thenReturn(null);

      oAuthStateService.addNewStateToDatastore(state1);
      oAuthStateService.addNewStateToDatastore(state2);
      oAuthStateService.addNewStateToDatastore(state3);

      verify(states).putIfAbsent(eq(state1), any(Instant.class));
      verify(states).putIfAbsent(eq(state2), any(Instant.class));
      verify(states).putIfAbsent(eq(state3), any(Instant.class));
    }
  }

  @Nested
  @DisplayName("READ Operations")
  class ReadOperationsTests {
    @Test
    @DisplayName("When checking existing state then return true")
    void whenCheckingExistingState_thenReturnTrue() {
      var state = "state";

      when(states.containsKey(state)).thenReturn(true);

      var result = oAuthStateService.isAvailableInDatabase(state);

      assertThat(result).isTrue();
      verify(states).containsKey(state);
    }

    @Test
    @DisplayName("When checking non-existent state then return false")
    void whenCheckingNonExistentState_thenReturnFalse() {
      var state = "state";

      when(states.containsKey(state)).thenReturn(false);

      var result = oAuthStateService.isAvailableInDatabase(state);

      assertThat(result).isFalse();
      verify(states).containsKey(state);
    }

    @Test
    @DisplayName("When checking null state then return false")
    void whenCheckingNullState_thenReturnFalse() {
      when(states.containsKey(null)).thenReturn(false);

      var result = oAuthStateService.isAvailableInDatabase(null);

      assertThat(result).isFalse();
      verify(states).containsKey(null);
    }

    @Test
    @DisplayName("When checking empty state then return false")
    void whenCheckingEmptyState_thenReturnFalse() {
      var state = "";

      when(states.containsKey(state)).thenReturn(false);

      var result = oAuthStateService.isAvailableInDatabase(state);

      assertThat(result).isFalse();
      verify(states).containsKey(state);
    }

    @Test
    @DisplayName("When datastore throws exception during check then propagate exception")
    void whenDatastoreThrowsExceptionDuringCheck_thenPropagateException() {
      var state = "state";

      when(states.containsKey(state)).thenThrow(new RuntimeException("Hazelcast read failed"));

      assertThatThrownBy(() -> oAuthStateService.isAvailableInDatabase(state))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Hazelcast read failed");
    }

    @Test
    @DisplayName("When checking multiple states with different results then return correctly")
    void whenCheckingMultipleStatesWithDifferentResults_thenReturnCorrectly() {
      var existingState = "existing-state";
      var nonExistentState = "non-existent-state";

      when(states.containsKey(existingState)).thenReturn(true);
      when(states.containsKey(nonExistentState)).thenReturn(false);

      var existingResult = oAuthStateService.isAvailableInDatabase(existingState);
      var nonExistentResult = oAuthStateService.isAvailableInDatabase(nonExistentState);

      assertThat(existingResult).isTrue();
      assertThat(nonExistentResult).isFalse();
      verify(states).containsKey(existingState);
      verify(states).containsKey(nonExistentState);
    }
  }

  @Nested
  @DisplayName("DELETE Operations")
  class DeleteOperationsTests {
    @Test
    @DisplayName("When deleting existing state then remove successfully")
    void whenDeletingExistingState_thenRemoveSuccessfully() throws Exception {
      var state = "state";
      var mockFuture = mock(CompletableFuture.class);

      when(states.removeAsync(state)).thenReturn(mockFuture);

      oAuthStateService.deleteStateFromDatastore(state);

      verify(states).removeAsync(state);
    }

    @Test
    @DisplayName("When deleting non-existent state then handle gracefully")
    void whenDeletingNonExistentState_thenHandleGracefully() throws Exception {
      var state = "state";
      var mockFuture = mock(CompletableFuture.class);

      when(states.removeAsync(state)).thenReturn(mockFuture);

      oAuthStateService.deleteStateFromDatastore(state);

      verify(states).removeAsync(state);
    }

    @Test
    @DisplayName("When deleting null state then handle gracefully")
    void whenDeletingNullState_thenHandleGracefully() throws Exception {
      var mockFuture = mock(CompletableFuture.class);

      when(states.removeAsync(null)).thenReturn(mockFuture);

      oAuthStateService.deleteStateFromDatastore(null);

      verify(states).removeAsync(null);
    }

    @Test
    @DisplayName("When deleting empty state then handle gracefully")
    void whenDeletingEmptyState_thenHandleGracefully() throws Exception {
      var state = "";
      var mockFuture = mock(CompletableFuture.class);

      when(states.removeAsync(state)).thenReturn(mockFuture);

      oAuthStateService.deleteStateFromDatastore(state);

      verify(states).removeAsync(state);
    }

    @Test
    @DisplayName("When datastore throws exception during delete then propagate exception")
    void whenDatastoreThrowsExceptionDuringDelete_thenPropagateException() {
      var state = "state";

      when(states.removeAsync(state)).thenThrow(new RuntimeException("Hazelcast delete failed"));

      assertThatThrownBy(() -> oAuthStateService.deleteStateFromDatastore(state))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Hazelcast delete failed");
    }

    @Test
    @DisplayName("When deleting multiple states then remove all successfully")
    void whenDeletingMultipleStates_thenRemoveAllSuccessfully() throws Exception {
      var state1 = "state-1";
      var state2 = "state-2";
      var state3 = "state-3";

      var mockFuture1 = mock(CompletableFuture.class);
      var mockFuture2 = mock(CompletableFuture.class);
      var mockFuture3 = mock(CompletableFuture.class);

      when(states.removeAsync(state1)).thenReturn(mockFuture1);
      when(states.removeAsync(state2)).thenReturn(mockFuture2);
      when(states.removeAsync(state3)).thenReturn(mockFuture3);

      oAuthStateService.deleteStateFromDatastore(state1);
      oAuthStateService.deleteStateFromDatastore(state2);
      oAuthStateService.deleteStateFromDatastore(state3);

      verify(states).removeAsync(state1);
      verify(states).removeAsync(state2);
      verify(states).removeAsync(state3);
    }

    @Test
    @DisplayName("When removeAsync throws runtime exception then propagate exception")
    void whenRemoveAsyncThrowsRuntimeException_thenPropagateException() throws Exception {
      var state = "state";
      var runtimeException = new RuntimeException("Runtime exception during remove");

      when(states.removeAsync(state)).thenThrow(runtimeException);

      assertThatThrownBy(() -> oAuthStateService.deleteStateFromDatastore(state))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Runtime exception during remove");
    }
  }

  @Nested
  @DisplayName("WORKFLOW Operations")
  class WorkflowOperationsTests {
    @Test
    @DisplayName("When complete OAuth flow then add check and delete state")
    void whenCompleteOAuthFlow_thenAddCheckAndDeleteState() throws Exception {
      var state = "state";
      var mockFuture = mock(CompletableFuture.class);

      when(states.putIfAbsent(eq(state), any(Instant.class))).thenReturn(null);
      when(states.containsKey(state)).thenReturn(true);
      when(states.removeAsync(state)).thenReturn(mockFuture);

      oAuthStateService.addNewStateToDatastore(state);
      var isAvailable = oAuthStateService.isAvailableInDatabase(state);
      oAuthStateService.deleteStateFromDatastore(state);

      assertThat(isAvailable).isTrue();
      verify(states).putIfAbsent(eq(state), any(Instant.class));
      verify(states).containsKey(state);
      verify(states).removeAsync(state);
    }

    @Test
    @DisplayName("When state is used before being added then return false")
    void whenStateIsUsedBeforeBeingAdded_thenReturnFalse() {
      var state = "state";

      when(states.containsKey(state)).thenReturn(false);

      var result = oAuthStateService.isAvailableInDatabase(state);

      assertThat(result).isFalse();
      verify(states).containsKey(state);
      verify(states, never()).putIfAbsent(anyString(), any(Instant.class));
    }

    @Test
    @DisplayName("When state is checked after deletion then return false")
    void whenStateIsCheckedAfterDeletion_thenReturnFalse() throws Exception {
      var state = "state";
      var mockFuture = mock(CompletableFuture.class);

      when(states.putIfAbsent(eq(state), any(Instant.class))).thenReturn(null);
      when(states.removeAsync(state)).thenReturn(mockFuture);
      when(states.containsKey(state)).thenReturn(false);

      oAuthStateService.addNewStateToDatastore(state);
      oAuthStateService.deleteStateFromDatastore(state);
      var result = oAuthStateService.isAvailableInDatabase(state);

      assertThat(result).isFalse();
      verify(states).putIfAbsent(eq(state), any(Instant.class));
      verify(states).removeAsync(state);
      verify(states).containsKey(state);
    }

    @Test
    @DisplayName("When handling concurrent operations then maintain consistency")
    void whenHandlingConcurrentOperations_thenMaintainConsistency() throws Exception {
      var state1 = "state-1";
      var state2 = "state-2";
      var mockFuture = mock(CompletableFuture.class);

      when(states.putIfAbsent(eq(state1), any(Instant.class))).thenReturn(null);
      when(states.putIfAbsent(eq(state2), any(Instant.class))).thenReturn(null);
      when(states.containsKey(state1)).thenReturn(true);
      when(states.containsKey(state2)).thenReturn(true);
      when(states.removeAsync(state1)).thenReturn(mockFuture);
      when(states.removeAsync(state2)).thenReturn(mockFuture);

      oAuthStateService.addNewStateToDatastore(state1);
      oAuthStateService.addNewStateToDatastore(state2);

      var result1 = oAuthStateService.isAvailableInDatabase(state1);
      var result2 = oAuthStateService.isAvailableInDatabase(state2);

      oAuthStateService.deleteStateFromDatastore(state1);
      oAuthStateService.deleteStateFromDatastore(state2);

      assertThat(result1).isTrue();
      assertThat(result2).isTrue();
      verify(states).putIfAbsent(eq(state1), any(Instant.class));
      verify(states).putIfAbsent(eq(state2), any(Instant.class));
      verify(states).containsKey(state1);
      verify(states).containsKey(state2);
      verify(states).removeAsync(state1);
      verify(states).removeAsync(state2);
    }
  }

  @Nested
  @DisplayName("ERROR HANDLING Operations")
  class ErrorHandlingOperationsTests {
    @Test
    @DisplayName("When multiple operations fail then handle each independently")
    void whenMultipleOperationsFail_thenHandleEachIndependently() throws Exception {
      var state1 = "state-1";
      var state2 = "state-2";
      var mockFuture = mock(CompletableFuture.class);

      when(states.putIfAbsent(eq(state1), any(Instant.class)))
          .thenThrow(new RuntimeException("Add failed"));
      when(states.putIfAbsent(eq(state2), any(Instant.class))).thenReturn(null);
      when(states.containsKey(state2)).thenReturn(true);

      assertThatThrownBy(() -> oAuthStateService.addNewStateToDatastore(state1))
          .isInstanceOf(RuntimeException.class);

      oAuthStateService.addNewStateToDatastore(state2);
      var result = oAuthStateService.isAvailableInDatabase(state2);

      assertThat(result).isTrue();
      verify(states).putIfAbsent(eq(state1), any(Instant.class));
      verify(states).putIfAbsent(eq(state2), any(Instant.class));
      verify(states).containsKey(state2);
    }

    @Test
    @DisplayName("When operations fail at different stages then handle appropriately")
    void whenOperationsFailAtDifferentStages_thenHandleAppropriately() throws Exception {
      var addFailState = "add-fail-state";
      var checkFailState = "check-fail-state";
      var deleteFailState = "delete-fail-state";
      var mockFuture = mock(CompletableFuture.class);

      when(states.putIfAbsent(eq(addFailState), any(Instant.class)))
          .thenThrow(new RuntimeException("Add operation failed"));
      when(states.putIfAbsent(eq(checkFailState), any(Instant.class))).thenReturn(null);
      when(states.containsKey(checkFailState))
          .thenThrow(new RuntimeException("Check operation failed"));
      when(states.putIfAbsent(eq(deleteFailState), any(Instant.class))).thenReturn(null);
      when(states.removeAsync(deleteFailState))
          .thenThrow(new RuntimeException("Delete operation failed"));

      assertThatThrownBy(() -> oAuthStateService.addNewStateToDatastore(addFailState))
          .hasMessageContaining("Add operation failed");

      oAuthStateService.addNewStateToDatastore(checkFailState);
      assertThatThrownBy(() -> oAuthStateService.isAvailableInDatabase(checkFailState))
          .hasMessageContaining("Check operation failed");

      oAuthStateService.addNewStateToDatastore(deleteFailState);
      assertThatThrownBy(() -> oAuthStateService.deleteStateFromDatastore(deleteFailState))
          .hasMessageContaining("Delete operation failed");
    }
  }
}
