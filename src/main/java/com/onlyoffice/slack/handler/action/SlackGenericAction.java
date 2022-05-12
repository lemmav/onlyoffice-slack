package com.onlyoffice.slack.handler.action;

import com.onlyoffice.slack.SlackActions;
import com.onlyoffice.slack.handler.SlackHandler;
import com.onlyoffice.slack.SlackOperations;
import com.slack.api.bolt.App;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SlackGenericAction implements SlackHandler {
    @Autowired
    public void register(App app) {
        app.blockAction(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            log.debug("Generic action call");
            return ctx.ack();
        });
    }

    public SlackOperations getSlackRequestHandler() {
        return SlackActions.GENERIC_ACTION;
    }
}
