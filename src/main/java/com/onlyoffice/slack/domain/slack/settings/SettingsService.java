package com.onlyoffice.slack.domain.slack.settings;

import com.onlyoffice.slack.shared.transfer.request.SubmitSettingsRequest;
import com.onlyoffice.slack.shared.transfer.response.SettingsResponse;
import com.slack.api.bolt.context.Context;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface SettingsService {
  void saveSettings(@NotNull final Context ctx, @NotNull final SubmitSettingsRequest request);

  SettingsResponse findSettings(@NotBlank final String teamId);

  SettingsResponse alwaysFindSettings(@NotBlank final String teamId);
}
