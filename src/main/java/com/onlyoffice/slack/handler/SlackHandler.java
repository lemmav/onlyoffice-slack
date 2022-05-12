package com.onlyoffice.slack.handler;

import com.onlyoffice.slack.SlackOperations;
import com.slack.api.bolt.App;

public interface SlackHandler {
    void register(App app);
    SlackOperations getSlackRequestHandler();
}
