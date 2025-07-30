package com.onlyoffice.slack.service.data;

import com.onlyoffice.slack.transfer.request.SubmitSettingsRequest;
import com.onlyoffice.slack.transfer.response.SettingsResponse;
import com.slack.api.bolt.context.Context;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface SettingsService {
  void saveSettings(@NotNull final Context ctx, @NotNull final SubmitSettingsRequest request);

  SettingsResponse findSettings(@NotBlank final String teamId);

  SettingsResponse alwaysFindSettings(@NotBlank final String teamId);
}
