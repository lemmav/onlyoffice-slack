package com.onlyoffice.slack.domain.slack.settings;

import static org.junit.jupiter.api.Assertions.*;

import com.onlyoffice.slack.shared.transfer.request.SubmitSettingsRequest;
import com.onlyoffice.slack.shared.transfer.response.SettingsResponse;
import com.slack.api.bolt.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettingsServiceTests {
  private SettingsService service;

  @BeforeEach
  void setUp() {
    service =
        new SettingsService() {
          boolean saved = false;

          @Override
          public void saveSettings(Context ctx, SubmitSettingsRequest request) {
            saved = true;
          }

          @Override
          public SettingsResponse findSettings(String teamId) {
            return SettingsResponse.builder().address("addr").build();
          }

          @Override
          public SettingsResponse alwaysFindSettings(String teamId) {
            return SettingsResponse.builder().address("always").build();
          }
        };
  }

  @Test
  void whenSaveSettings_thenNoException() {
    assertDoesNotThrow(() -> service.saveSettings(null, null));
  }

  @Test
  void whenFindSettings_thenReturnSettingsResponse() {
    var resp = service.findSettings("team");

    assertNotNull(resp);
    assertEquals("addr", resp.getAddress());
  }

  @Test
  void whenAlwaysFindSettings_thenReturnSettingsResponse() {
    var resp = service.alwaysFindSettings("team");

    assertNotNull(resp);
    assertEquals("always", resp.getAddress());
  }
}
