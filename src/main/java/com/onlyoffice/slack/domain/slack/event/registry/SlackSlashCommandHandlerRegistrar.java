package com.onlyoffice.slack.domain.slack.event.registry;

import com.slack.api.bolt.handler.builtin.SlashCommandHandler;

public interface SlackSlashCommandHandlerRegistrar {
  String getSlash();

  SlashCommandHandler getHandler();
}
