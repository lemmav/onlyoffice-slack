package com.onlyoffice.slack.service.document.core;

import com.onlyoffice.model.documenteditor.Callback;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public interface DocumentCallbackService {
  void processCallback(
      @NotNull final Map<String, String> headers, @NotNull final Callback callback);
}
