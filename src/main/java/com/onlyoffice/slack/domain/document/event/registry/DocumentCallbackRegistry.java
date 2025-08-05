package com.onlyoffice.slack.domain.document.event.registry;

import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Status;
import com.onlyoffice.slack.shared.utils.TriFunction;
import java.util.Map;
import java.util.Optional;

public interface DocumentCallbackRegistry {
  void register(Status status, TriFunction<String, String, Callback, Callback> callbackHandler);

  Optional<TriFunction<String, String, Callback, Callback>> find(Status status);

  Map<Status, TriFunction<String, String, Callback, Callback>> getRegistry();
}
