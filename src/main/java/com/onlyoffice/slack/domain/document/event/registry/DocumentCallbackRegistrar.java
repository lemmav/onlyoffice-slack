package com.onlyoffice.slack.domain.document.event.registry;

import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Status;
import com.onlyoffice.slack.shared.utils.TriFunction;

public interface DocumentCallbackRegistrar {
  Status getStatus();

  TriFunction<String, String, Callback, Callback> getHandler();
}
