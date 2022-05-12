package com.onlyoffice.slack.handler.action;

import com.onlyoffice.slack.SlackActions;
import com.onlyoffice.slack.handler.SlackHandler;
import com.onlyoffice.slack.SlackOperations;
import com.slack.api.bolt.App;
import com.slack.api.methods.request.views.ViewsUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.view.Views.*;

//TODO: Proper UI
@Component
@Slf4j
public class SlackOpenFileButtonAction implements SlackHandler {
    private static final String closeText = "Close";
    private static final String titleText = "ONLYOFFICE Files";
    private static final String mayCloseText = "This window may now be closed";

    @Autowired
    public void register(App app) {
        app.blockAction(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            log.debug("Post open file handler");
            ctx.client().viewsUpdate(ViewsUpdateRequest
                    .builder()
                    .token(ctx.getBotToken())
                    .viewId(req.getPayload().getView().getId())
                    .view(
                            view(
                                    view -> view.type("modal")
                                            .notifyOnClose(false)
                                            .title(viewTitle(title -> title.type("plain_text").text(titleText)))
                                            .close(viewClose(close -> close.type("plain_text").text(closeText)))
                                            .blocks(asBlocks(
                                                    divider(),
                                                    section(s -> s.text(
                                                            markdownText(mayCloseText)
                                                    )),
                                                    divider()
                                            ))
                            )
                    )
                    .build()
            );
            return ctx.ack();
        });
    }

    public SlackOperations getSlackRequestHandler() {
        return SlackActions.OPEN_ONLYOFFICE_FILE;
    }
}
