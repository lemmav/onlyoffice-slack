package com.onlyoffice.slack.service.data;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onlyoffice.slack.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.exception.SettingsConfigurationException;
import com.onlyoffice.slack.persistence.entity.TeamSettings;
import com.onlyoffice.slack.persistence.repository.TeamSettingsRepository;
import com.onlyoffice.slack.service.cryptography.AesEncryptionService;
import com.onlyoffice.slack.transfer.request.SubmitSettingsRequest;
import com.slack.api.bolt.context.Context;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamSettingsServiceTests {
  @Mock private ServerConfigurationProperties configurationProperties;
  @Mock private TeamSettingsRepository settingsRepository;
  @Mock private AesEncryptionService encryptionService;
  @InjectMocks private TeamSettingsService service;

  @Test
  void whenSaveSettingsWithValidRequest_thenRepositoryUpsertCalled() {
    var ctx = mock(Context.class);

    when(ctx.getTeamId()).thenReturn("team");

    var req = mock(SubmitSettingsRequest.class);

    when(req.getSecret()).thenReturn("secret");
    when(req.isValidConfiguration()).thenReturn(true);
    when(req.getAddress()).thenReturn("address");
    when(req.getHeader()).thenReturn("header");
    when(req.isDemoEnabled()).thenReturn(false);
    when(encryptionService.encrypt(anyString())).thenReturn("encrypted");

    service.saveSettings(ctx, req);

    verify(settingsRepository)
        .upsertSettings(eq("team"), eq("address"), eq("header"), eq("encrypted"), eq(false));
  }

  @Test
  void whenSaveSettingsWithInvalidConfig_thenThrowException() {
    var ctx = mock(Context.class);
    var req = mock(SubmitSettingsRequest.class);

    when(req.isValidConfiguration()).thenReturn(false);
    assertThrows(SettingsConfigurationException.class, () -> service.saveSettings(ctx, req));
  }

  @Test
  void whenFindSettingsWithNoSettings_thenReturnEmptyResponse() {
    when(settingsRepository.findById("team")).thenReturn(Optional.empty());

    var resp = service.findSettings("team");

    assertNotNull(resp);
  }

  @Test
  void whenFindSettingsWithIncompleteConfig_thenThrowException() {
    var settings = mock(TeamSettings.class);

    when(settings.getDemoEnabled()).thenReturn(false);
    when(settings.getAddress()).thenReturn(null);
    when(settingsRepository.findById("team")).thenReturn(Optional.of(settings));

    assertThrows(SettingsConfigurationException.class, () -> service.findSettings("team"));
  }

  @Test
  void whenFindSettingsWithExpiredDemo_thenThrowException() {
    var settings = mock(com.onlyoffice.slack.persistence.entity.TeamSettings.class);

    when(settings.getDemoEnabled()).thenReturn(true);
    when(settings.getDemoStartedDate()).thenReturn(LocalDateTime.now().minusDays(10));

    var demoProps = mock(ServerConfigurationProperties.DemoProperties.class);

    when(demoProps.getDurationDays()).thenReturn(5);
    when(configurationProperties.getDemo()).thenReturn(demoProps);
    when(settingsRepository.findById("team")).thenReturn(Optional.of(settings));
    assertThrows(SettingsConfigurationException.class, () -> service.findSettings("team"));
  }

  @Test
  void whenAlwaysFindSettingsWithNoSettings_thenReturnEmptyResponse() {
    when(settingsRepository.findById("team")).thenReturn(Optional.empty());

    var resp = service.alwaysFindSettings("team");

    assertNotNull(resp);
  }
}
