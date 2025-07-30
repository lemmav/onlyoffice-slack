package com.onlyoffice.slack.service.document.core;

import com.onlyoffice.slack.transfer.request.SubmitSettingsRequest;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;

public interface DocumentSettingsValidationService {
  void validateConnection(@NotNull final SubmitSettingsRequest request) throws IOException;
}
