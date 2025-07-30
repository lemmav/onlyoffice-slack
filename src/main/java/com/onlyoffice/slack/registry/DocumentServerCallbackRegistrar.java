package com.onlyoffice.slack.registry;

import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Status;
import com.onlyoffice.slack.util.TriFunction;

public interface DocumentServerCallbackRegistrar {
  Status getStatus();

  TriFunction<String, String, Callback, Callback> getHandler();
}
