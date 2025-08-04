package com.onlyoffice.slack.domain.document.editor.core;

import com.onlyoffice.slack.shared.transfer.request.SubmitSettingsRequest;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;

public interface DocumentSettingsValidationService {
  void validateConnection(@NotNull final SubmitSettingsRequest request) throws IOException;
}
