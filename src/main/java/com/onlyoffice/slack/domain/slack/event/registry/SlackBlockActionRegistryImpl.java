package com.onlyoffice.slack.domain.slack.event.registry;

import com.slack.api.bolt.handler.builtin.BlockActionHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Getter
@Component
class SlackBlockActionRegistryImpl implements SlackBlockActionRegistry {
  private final Map<String, BlockActionHandler> registry = new HashMap<>();

  @Autowired
  public SlackBlockActionRegistryImpl(final List<SlackBlockActionHandlerRegistrar> registrars) {
    for (var registrar : registrars) {
      for (var action : registrar.getIds()) register(action, registrar.getAction());
    }
  }

  @Override
  public void register(final String callbackId, final BlockActionHandler actionHandler) {
    registry.putIfAbsent(callbackId, actionHandler);
  }

  @Override
  public Optional<BlockActionHandler> find(final String callbackId) {
    return Optional.ofNullable(registry.get(callbackId));
  }
}
