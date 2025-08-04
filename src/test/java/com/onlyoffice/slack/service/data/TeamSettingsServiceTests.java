package com.onlyoffice.slack.service.data;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onlyoffice.slack.domain.slack.settings.TeamSettingsRepository;
import com.onlyoffice.slack.domain.slack.settings.TeamSettingsService;
import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.shared.configuration.message.MessageSourceSlackConfiguration;
import com.onlyoffice.slack.shared.exception.domain.DocumentSettingsConfigurationException;
import com.onlyoffice.slack.shared.persistence.entity.TeamSettings;
import com.onlyoffice.slack.shared.transfer.request.SubmitSettingsRequest;
import com.onlyoffice.slack.shared.utils.AesEncryptionService;
import com.slack.api.bolt.context.Context;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class TeamSettingsServiceTests {
  @Mock private MessageSourceSlackConfiguration messageSourceSlackConfiguration;
  @Mock private ServerConfigurationProperties configurationProperties;
  @Mock private MessageSource messageSource;

  @Mock private TeamSettingsRepository teamSettingsRepository;
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

    verify(teamSettingsRepository)
        .upsertSettings(eq("team"), eq("address"), eq("header"), eq("encrypted"), eq(false));
  }

  @Test
  void whenSaveSettingsWithInvalidConfig_thenThrowException() {
    var ctx = mock(Context.class);
    var req = mock(SubmitSettingsRequest.class);

    when(req.isValidConfiguration()).thenReturn(false);
    when(messageSourceSlackConfiguration.getErrorSettingsTitle())
        .thenReturn("error.settings.title");
    when(messageSourceSlackConfiguration.getErrorSettingsInvalidConfigurationText())
        .thenReturn("error.settings.invalid.text");
    when(messageSourceSlackConfiguration.getErrorSettingsButton())
        .thenReturn("error.settings.button");
    when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Test message");

    assertThrows(
        DocumentSettingsConfigurationException.class, () -> service.saveSettings(ctx, req));
  }

  @Test
  void whenFindSettingsWithNoSettings_thenReturnEmptyResponse() {
    when(teamSettingsRepository.findById("team")).thenReturn(Optional.empty());

    var resp = service.findSettings("team");

    assertNotNull(resp);
  }

  @Test
  void whenFindSettingsWithIncompleteConfig_thenThrowException() {
    var settings = mock(TeamSettings.class);

    when(settings.getDemoEnabled()).thenReturn(false);
    when(settings.getAddress()).thenReturn(null);
    when(teamSettingsRepository.findById("team")).thenReturn(Optional.of(settings));
    when(messageSourceSlackConfiguration.getErrorSettingsTitle())
        .thenReturn("error.settings.title");
    when(messageSourceSlackConfiguration.getErrorSettingsIncompleteText())
        .thenReturn("error.settings.incomplete.text");
    when(messageSourceSlackConfiguration.getErrorSettingsButton())
        .thenReturn("error.settings.button");
    when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Test message");

    assertThrows(DocumentSettingsConfigurationException.class, () -> service.findSettings("team"));
  }

  @Test
  void whenFindSettingsWithExpiredDemo_thenThrowException() {
    var settings = mock(TeamSettings.class);

    when(settings.getDemoEnabled()).thenReturn(true);
    when(settings.getDemoStartedDate()).thenReturn(LocalDateTime.now().minusDays(10));

    var demoProps = mock(ServerConfigurationProperties.DemoProperties.class);

    when(demoProps.getDurationDays()).thenReturn(5);
    when(configurationProperties.getDemo()).thenReturn(demoProps);
    when(teamSettingsRepository.findById("team")).thenReturn(Optional.of(settings));
    when(messageSourceSlackConfiguration.getErrorSettingsTitle())
        .thenReturn("error.settings.title");
    when(messageSourceSlackConfiguration.getErrorSettingsDemoText())
        .thenReturn("error.settings.demo.text");
    when(messageSourceSlackConfiguration.getErrorSettingsButton())
        .thenReturn("error.settings.button");
    when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Test message");

    assertThrows(DocumentSettingsConfigurationException.class, () -> service.findSettings("team"));
  }

  @Test
  void whenAlwaysFindSettingsWithNoSettings_thenReturnEmptyResponse() {
    when(teamSettingsRepository.findById("team")).thenReturn(Optional.empty());

    var resp = service.alwaysFindSettings("team");

    assertNotNull(resp);
  }
}
