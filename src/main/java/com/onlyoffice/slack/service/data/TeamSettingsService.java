package com.onlyoffice.slack.service.data;

import com.onlyoffice.slack.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.exception.SettingsConfigurationException;
import com.onlyoffice.slack.persistence.repository.TeamSettingsRepository;
import com.onlyoffice.slack.service.cryptography.AesEncryptionService;
import com.onlyoffice.slack.transfer.request.SubmitSettingsRequest;
import com.onlyoffice.slack.transfer.response.SettingsResponse;
import com.slack.api.bolt.context.Context;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class TeamSettingsService implements SettingsService {
  private final ServerConfigurationProperties configurationProperties;

  private final TeamSettingsRepository settingsRepository;
  private final AesEncryptionService encryptionService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void saveSettings(
      @NotNull final Context ctx, @NotNull final SubmitSettingsRequest request) {
    var secret = request.getSecret();
    if (secret != null && !secret.isBlank()) secret = encryptionService.encrypt(secret);
    if (!request.isValidConfiguration())
      throw new SettingsConfigurationException(
          SettingsConfigurationException.SettingsErrorType.INVALID_CONFIGURATION,
          "Could not validate settings");

    settingsRepository.upsertSettings(
        ctx.getTeamId(),
        request.getAddress(),
        request.getHeader(),
        secret,
        request.isDemoEnabled());
  }

  @Override
  public SettingsResponse findSettings(@NotBlank final String teamId) {
    var maybeSettings = settingsRepository.findById(teamId);
    if (maybeSettings.isEmpty()) return SettingsResponse.builder().build();
    var settings = maybeSettings.get();

    if (!settings.getDemoEnabled()
        && (settings.getAddress() == null
            || settings.getAddress().trim().isEmpty()
            || settings.getSecret() == null
            || settings.getSecret().trim().isEmpty())) {
      throw new SettingsConfigurationException(
          SettingsConfigurationException.SettingsErrorType.INCOMPLETE_CONFIGURATION,
          "ONLYOFFICE settings are incomplete. Please ask your Slack administrator to complete the configuration in the app Home tab");
    }

    if (settings.getDemoEnabled()
        && settings
            .getDemoStartedDate()
            .plusDays(configurationProperties.getDemo().getDurationDays())
            .isBefore(LocalDateTime.now()))
      throw new SettingsConfigurationException(
          SettingsConfigurationException.SettingsErrorType.INVALID_CONFIGURATION,
          "ONLYOFFICE demo has expired.");

    var secret = settings.getSecret();
    if (secret != null && !secret.isBlank()) secret = encryptionService.decrypt(secret);

    return SettingsResponse.builder()
        .address(settings.getAddress())
        .header(settings.getHeader())
        .secret(secret)
        .demoEnabled(settings.getDemoEnabled())
        .demoStartedDate(settings.getDemoStartedDate())
        .build();
  }

  @Override
  public SettingsResponse alwaysFindSettings(@NotBlank final String teamId) {
    var maybeSettings = settingsRepository.findById(teamId);
    if (maybeSettings.isEmpty()) return SettingsResponse.builder().build();
    var settings = maybeSettings.get();

    var secret = settings.getSecret();
    if (secret != null && !secret.isBlank()) secret = encryptionService.decrypt(secret);

    return SettingsResponse.builder()
        .address(settings.getAddress())
        .header(settings.getHeader())
        .secret(secret)
        .demoEnabled(settings.getDemoEnabled())
        .demoStartedDate(settings.getDemoStartedDate())
        .build();
  }
}
