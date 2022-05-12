package com.onlyoffice.slack.handler.action;

import com.onlyoffice.slack.SlackActions;
import com.onlyoffice.slack.SlackOperations;
import com.onlyoffice.slack.handler.SlackHandler;
import com.onlyoffice.slack.model.registry.Workspace;
import com.onlyoffice.slack.service.registry.SlackOnlyofficeRegistryInstallationService;
import com.slack.api.bolt.App;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    private final SlackOnlyofficeRegistryInstallationService installationService;

    @Autowired
    public void register(App app) {
        app.blockAction(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            log.debug("Open settings call");

            Workspace workspace = installationService.findWorkspace(ctx.getTeamId());
            if (workspace == null) return null;

            ctx.client().viewsOpen(r -> r
                    .triggerId(ctx.getTriggerId())
                    .view(view(view -> view
                            .callbackId(getSlackRequestHandler().getEntrypoint())
                            .type("modal")
                            .notifyOnClose(false)
                            .title(viewTitle(title -> title.type("plain_text").text("ONLYOFFICE Settings")))
                            .submit(viewSubmit(submit -> submit.type("plain_text").text("Submit")))
                            .close(viewClose(close -> close.type("plain_text").text("Cancel")))
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
                                            .label(plainText(pt -> pt.text("Document Server URL").emoji(false)))
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
                                            .label(plainText(pt -> pt.text("Document Server JWT Secret").emoji(false)))
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
                                            .label(plainText(pt -> pt.text("Document Server JWT Header").emoji(false)))
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
