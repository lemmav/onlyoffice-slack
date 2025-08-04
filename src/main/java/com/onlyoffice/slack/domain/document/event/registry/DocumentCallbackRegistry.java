package com.onlyoffice.slack.domain.document.event.registry;

import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Status;
import com.onlyoffice.slack.shared.utils.TriFunction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DocumentCallbackRegistry {
  private final Map<Status, TriFunction<String, String, Callback, Callback>> registry =
      new HashMap<>();

  @Autowired
  public DocumentCallbackRegistry(final List<DocumentCallbackRegistrar> registrars) {
    for (var registrar : registrars) register(registrar.getStatus(), registrar.getHandler());
  }

  public void register(
      final Status status, final TriFunction<String, String, Callback, Callback> callbackHandler) {
    registry.putIfAbsent(status, callbackHandler);
  }

  public Optional<TriFunction<String, String, Callback, Callback>> find(final Status status) {
    return Optional.of(registry.getOrDefault(status, (teamId, userId, callback) -> callback));
  }
}
