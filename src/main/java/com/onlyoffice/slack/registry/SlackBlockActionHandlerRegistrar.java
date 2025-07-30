package com.onlyoffice.slack.registry;

import com.slack.api.bolt.handler.builtin.BlockActionHandler;

public interface SlackBlockActionHandlerRegistrar {
  String getId();

  BlockActionHandler getAction();
}
