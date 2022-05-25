package com.onlyoffice.slack.handler.action;

import com.onlyoffice.slack.SlackActions;
import com.onlyoffice.slack.SlackOperations;
import com.onlyoffice.slack.handler.SlackHandler;
import com.onlyoffice.slack.model.slack.Caller;
import com.onlyoffice.slack.service.slack.SlackLocaleService;
import com.slack.api.bolt.App;
import com.slack.api.methods.request.views.ViewsUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.view.Views.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class SlackOpenFileButtonAction implements SlackHandler {
    private final SlackLocaleService slackLocaleService;
    private final MessageSource messageSource;

    @Autowired
    public void register(App app) {
        app.blockAction(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            log.debug("Post open file handler");

            Locale locale = slackLocaleService.getLocale(Caller
                    .builder()
                            .token(ctx.getBotToken())
                            .isRoot(false)
                            .wid(ctx.getTeamId())
                            .name(ctx.getRequestUserId())
                            .id(ctx.getRequestUserId())
                    .build()
            );

            String closeText = messageSource.getMessage("file.modal.close", null, locale);
            String titleText = messageSource.getMessage("file.modal.title", null, locale);
            String mayCloseText = messageSource.getMessage("file.modal.mayClose", null, locale);

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
