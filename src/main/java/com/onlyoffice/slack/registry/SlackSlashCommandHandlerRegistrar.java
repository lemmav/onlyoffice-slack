package com.onlyoffice.slack.registry;

import com.slack.api.bolt.handler.builtin.SlashCommandHandler;

public interface SlackSlashCommandHandlerRegistrar {
  String getSlash();

  SlashCommandHandler getHandler();
}
