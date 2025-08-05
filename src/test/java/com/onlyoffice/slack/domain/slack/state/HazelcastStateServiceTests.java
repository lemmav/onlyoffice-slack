package com.onlyoffice.slack.domain.slack.state;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hazelcast.map.IMap;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HazelcastStateServiceTests {
  @Mock private IMap<String, Instant> states;
  @InjectMocks private HazelcastStateService service;

  @Test
  void whenAddNewStateToDatastore_thenStateIsPutIfAbsent() {
    var state = "state";

    service.addNewStateToDatastore(state);

    verify(states, times(1)).putIfAbsent(eq(state), any(Instant.class));
  }

  @Test
  void whenIsAvailableInDatabase_thenReturnTrueIfExists() {
    var state = "state";

    when(states.containsKey(state)).thenReturn(true);

    assertTrue(service.isAvailableInDatabase(state));
  }

  @Test
  void whenIsAvailableInDatabase_thenReturnFalseIfNotExists() {
    var state = "state";

    when(states.containsKey(state)).thenReturn(false);

    assertFalse(service.isAvailableInDatabase(state));
  }

  @Test
  void whenDeleteStateFromDatastore_thenRemoveAsyncCalled() throws Exception {
    var state = "state";

    service.deleteStateFromDatastore(state);

    verify(states, times(1)).removeAsync(state);
  }
}
