package com.onlyoffice.slack.domain.slack.event.registry;

import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import java.util.Optional;

public interface SlackSlashCommandHandlerRegistry {
  void register(String slash, SlashCommandHandler handler);

  Optional<SlashCommandHandler> find(String slash);

  java.util.Map<String, SlashCommandHandler> getRegistry();
}
