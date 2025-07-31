package com.onlyoffice.slack.registry;

import com.slack.api.bolt.handler.builtin.BlockActionHandler;
import java.util.List;

public interface SlackBlockActionHandlerRegistrar {
  List<String> getIds();

  BlockActionHandler getAction();
}
