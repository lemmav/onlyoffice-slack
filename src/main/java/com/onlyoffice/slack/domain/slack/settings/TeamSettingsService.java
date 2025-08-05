package com.onlyoffice.slack.domain.slack.settings;

import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.shared.configuration.message.MessageSourceSlackConfiguration;
import com.onlyoffice.slack.shared.exception.domain.DocumentSettingsConfigurationException;
import com.onlyoffice.slack.shared.persistence.entity.TeamSettings;
import com.onlyoffice.slack.shared.transfer.request.SubmitSettingsRequest;
import com.onlyoffice.slack.shared.transfer.response.SettingsResponse;
import com.onlyoffice.slack.shared.utils.AesEncryptionService;
import com.onlyoffice.slack.shared.utils.Mapper;
import com.slack.api.bolt.context.Context;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
class TeamSettingsService implements SettingsService {
  private final MessageSourceSlackConfiguration messageSourceSlackConfiguration;
  private final ServerConfigurationProperties configurationProperties;
  private final MessageSource messageSource;

  private final Mapper<TeamSettings, SettingsResponse> teamSettingsMapper;

  private final TeamSettingsRepository teamSettingsRepository;
  private final AesEncryptionService encryptionService;

  @Override
  @Retry(name = "team_settings")
  @Transactional(rollbackFor = Exception.class, timeout = 2)
  public void saveSettings(
      @NotNull final Context ctx, @NotNull final SubmitSettingsRequest request) {
    var secret = request.getSecret();
    if (secret != null && !secret.isBlank()) secret = encryptionService.encrypt(secret);
    if (!request.isValidConfiguration())
      throw new DocumentSettingsConfigurationException(
          DocumentSettingsConfigurationException.SettingsErrorType.INVALID_CONFIGURATION,
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSettingsTitle(), null, Locale.ENGLISH),
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSettingsInvalidConfigurationText(),
              null,
              Locale.ENGLISH),
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSettingsButton(), null, Locale.ENGLISH));

    teamSettingsRepository.upsertSettings(
        ctx.getTeamId(),
        request.getAddress(),
        request.getHeader(),
        secret,
        request.isDemoEnabled());
  }

  @Override
  @Retry(name = "team_settings")
  @Transactional(readOnly = true, timeout = 1)
  public SettingsResponse findSettings(@NotBlank final String teamId) {
    var maybeSettings = teamSettingsRepository.findById(teamId);
    if (maybeSettings.isEmpty()) return SettingsResponse.builder().build();
    var settings = maybeSettings.get();

    if (!settings.getDemoEnabled()
        && (settings.getAddress() == null
            || settings.getAddress().trim().isEmpty()
            || settings.getSecret() == null
            || settings.getSecret().trim().isEmpty())) {
      throw new DocumentSettingsConfigurationException(
          DocumentSettingsConfigurationException.SettingsErrorType.INCOMPLETE_CONFIGURATION,
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSettingsTitle(), null, Locale.ENGLISH),
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSettingsIncompleteText(),
              null,
              Locale.ENGLISH),
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSettingsButton(), null, Locale.ENGLISH));
    }

    if (settings.getDemoEnabled()
        && settings
            .getDemoStartedDate()
            .plusDays(configurationProperties.getDemo().getDurationDays())
            .isBefore(LocalDateTime.now()))
      throw new DocumentSettingsConfigurationException(
          DocumentSettingsConfigurationException.SettingsErrorType.DEMO_EXPIRED,
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSettingsTitle(), null, Locale.ENGLISH),
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSettingsDemoText(), null, Locale.ENGLISH),
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSettingsButton(), null, Locale.ENGLISH));

    var secret = settings.getSecret();
    if (secret != null && !secret.isBlank()) secret = encryptionService.decrypt(secret);

    var response = teamSettingsMapper.map(settings);
    response.setSecret(secret);
    return response;
  }

  @Override
  @Retry(name = "team_settings")
  @Transactional(readOnly = true, timeout = 1)
  public SettingsResponse alwaysFindSettings(@NotBlank final String teamId) {
    var maybeSettings = teamSettingsRepository.findById(teamId);
    if (maybeSettings.isEmpty()) return SettingsResponse.builder().build();
    var settings = maybeSettings.get();

    var secret = settings.getSecret();
    if (secret != null && !secret.isBlank()) secret = encryptionService.decrypt(secret);

    var response = teamSettingsMapper.map(settings);
    response.setSecret(secret);
    return response;
  }
}
