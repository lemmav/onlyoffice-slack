package com.onlyoffice.slack.handler.action;

import com.onlyoffice.slack.SlackActions;
import com.onlyoffice.slack.SlackOperations;
import com.onlyoffice.slack.handler.SlackHandler;
import com.onlyoffice.slack.model.registry.Workspace;
import com.onlyoffice.slack.model.slack.Caller;
import com.onlyoffice.slack.service.registry.SlackOnlyofficeRegistryInstallationService;
import com.onlyoffice.slack.service.slack.SlackLocaleService;
import com.slack.api.bolt.App;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.slack.api.model.block.Blocks.asBlocks;
import static com.slack.api.model.block.Blocks.input;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.plainTextInput;
import static com.slack.api.model.view.Views.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackOpenOnlyofficeSettingsAction implements SlackHandler {
    public static final String urlKey = "ds-url";
    public static final String secretKey = "ds-secret";
    public static final String headerKey = "ds-header";

    private final SlackLocaleService slackLocaleService;
    private final MessageSource messageSource;
    private final SlackOnlyofficeRegistryInstallationService installationService;

    @Autowired
    public void register(App app) {
        app.blockAction(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            log.debug("open settings call");

            Workspace workspace = installationService.findWorkspace(ctx.getTeamId());
            if (workspace == null) return null;

            Locale locale = slackLocaleService.getLocale(Caller
                    .builder()
                            .token(ctx.getBotToken())
                            .isRoot(false)
                            .wid(ctx.getTeamId())
                            .name(ctx.getRequestUserId())
                            .id(ctx.getRequestUserId())
                    .build()
            );

            String submitButton = messageSource.getMessage("file.modal.submit", null, locale);
            String cancelButton = messageSource.getMessage("file.modal.cancel", null, locale);
            String settingsTitle = messageSource.getMessage("file.modal.settings.title", null, locale);
            String settingsUrl = messageSource.getMessage("file.modal.settings.url", null, locale);
            String settingsJwt = messageSource.getMessage("file.modal.settings.jwt", null, locale);
            String settingsHeader = messageSource.getMessage("file.modal.settings.header", null, locale);

            ctx.client().viewsOpen(r -> r
                    .triggerId(ctx.getTriggerId())
                    .view(view(view -> view
                            .callbackId(getSlackRequestHandler().getEntrypoint())
                            .type("modal")
                            .notifyOnClose(false)
                            .title(viewTitle(title -> title.type("plain_text").text(settingsTitle)))
                            .submit(viewSubmit(submit -> submit.type("plain_text").text(submitButton)))
                            .close(viewClose(close -> close.type("plain_text").text(cancelButton)))
                            .blocks(asBlocks(
                                    input(input -> input
                                            .element(plainTextInput(i -> i
                                                            .multiline(false)
                                                            .actionId(urlKey)
                                                            .initialValue(workspace.getServerUrl())
                                                            .placeholder(plainText("https://example.com"))
                                                    )
                                            )
                                            .blockId(urlKey)
                                            .label(plainText(pt -> pt.text(settingsUrl).emoji(false)))
                                    ),
                                    input(input -> input
                                            .element(plainTextInput(i -> i
                                                            .multiline(false)
                                                            .actionId(secretKey)
                                                            .initialValue(workspace.getServerSecret())
                                                            .placeholder(plainText("secret"))
                                                    )
                                            )
                                            .blockId(secretKey)
                                            .label(plainText(pt -> pt.text(settingsJwt).emoji(false)))
                                    ),
                                    input(input -> input
                                            .element(plainTextInput(i -> i
                                                            .multiline(false)
                                                            .actionId(headerKey)
                                                            .initialValue(workspace.getServerHeader())
                                                            .placeholder(plainText("Authorization"))
                                                    )
                                            )
                                            .blockId(headerKey)
                                            .label(plainText(pt -> pt.text(settingsHeader).emoji(false)))
                                    )
                            ))))
            );

            return ctx.ack();
        });
    }

    public SlackOperations getSlackRequestHandler() {
        return SlackActions.OPEN_SETTINGS;
    }
}
