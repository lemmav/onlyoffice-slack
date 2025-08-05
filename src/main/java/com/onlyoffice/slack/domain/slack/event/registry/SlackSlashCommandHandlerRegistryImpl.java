package com.onlyoffice.slack.domain.slack.event.registry;

import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Getter
@Component
class SlackSlashCommandHandlerRegistryImpl implements SlackSlashCommandHandlerRegistry {
  private final Map<String, SlashCommandHandler> registry = new HashMap<>();

  @Autowired
  public SlackSlashCommandHandlerRegistryImpl(
      final List<SlackSlashCommandHandlerRegistrar> registrars) {
    for (var registrar : registrars) {
      register(registrar.getSlash(), registrar.getHandler());
    }
  }

  @Override
  public void register(final String slash, final SlashCommandHandler handler) {
    registry.putIfAbsent(slash, handler);
  }

  @Override
  public Optional<SlashCommandHandler> find(final String slash) {
    return Optional.ofNullable(registry.get(slash));
  }
}
