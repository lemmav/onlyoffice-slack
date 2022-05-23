package com.onlyoffice.slack.handler.entrypoint;

import com.onlyoffice.slack.SlackActions;
import com.onlyoffice.slack.SlackOperations;
import com.onlyoffice.slack.configuration.general.IntegrationConfiguration;
import com.onlyoffice.slack.exception.UnableToPerformSlackOperationException;
import com.onlyoffice.slack.handler.SlackHandler;
import com.onlyoffice.slack.model.onlyoffice.OnlyofficeEditorToken;
import com.onlyoffice.slack.model.registry.Workspace;
import com.onlyoffice.slack.model.slack.Caller;
import com.onlyoffice.slack.model.slack.ScheduledOtp;
import com.onlyoffice.slack.service.registry.SlackOnlyofficeRegistryInstallationService;
import com.onlyoffice.slack.service.slack.SlackOtpGeneratorService;
import com.onlyoffice.slack.util.SlackFileConverter;
import com.onlyoffice.slack.util.SlackLinkConverter;
import com.onlyoffice.slack.util.TimeoutScheduler;
import com.slack.api.app_backend.interactive_components.payload.MessageShortcutPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.model.Installer;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.views.ViewsOpenRequest;
import com.slack.api.model.Message;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.element.BlockElements;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.view.View;
import core.security.OnlyofficeJwtSecurity;
import core.util.OnlyofficeFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.button;
import static com.slack.api.model.view.Views.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackMessageShortcutViewHandler implements SlackHandler {
    private static final String modalTitle = "ONLYOFFICE Files";

    private final SlackOtpGeneratorService otpGenerator;
    private final IntegrationConfiguration integrationConfiguration;
    private final SlackFileConverter fileConverter;
    private final SlackLinkConverter linkConverter;
    private final OnlyofficeFile fileUtil;
    private final OnlyofficeJwtSecurity jwtSecurity;
    private final TimeoutScheduler scheduler;
    private final SlackOnlyofficeRegistryInstallationService installationService;

    @Autowired
    public void register(App app) {
        app.messageShortcut(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            log.debug("Open file");
            scheduler.setTimeout(() -> {
                String wid = req.getPayload().getTeam().getId();
                Message message = req.getPayload().getMessage();
                message.setTeam(wid);
                message.setChannel(req.getPayload().getChannel().getId());
                message.setBotId(ctx.getBotId());

                try {
                    Workspace workspace = installationService.findWorkspace(wid);
                    if (workspace == null || workspace.getServerUrl() == null || workspace.getServerUrl().isBlank()) {
                        ctx.client()
                                .viewsOpen(ViewsOpenRequest
                                        .builder()
                                        .token(ctx.getBotToken())
                                        .triggerId(ctx.getTriggerId())
                                        .view(
                                                view(
                                                        view -> view.type("modal")
                                                                .notifyOnClose(false)
                                                                .title(viewTitle(title -> title.type("plain_text").text(modalTitle)))
                                                                .callbackId(SlackActions.GENERIC_ACTION.getEntrypoint())
                                                                .close(viewClose(close -> close.type("plain_text").text("Cancel")))
                                                                .blocks(asBlocks(
                                                                        header(h -> h.text(plainText("Seems you have not connected your Workspace"))),
                                                                        section(s -> s.text(
                                                                                markdownText("Please go to ONLYOFFICE App *<https://app.slack.com/client/"+ ctx.getTeamId() +"/apps|Home>* page and fill out all fields to connect your Workspace")
                                                                        ))
                                                                ))
                                                )
                                        )
                                        .build()
                                );
                        return;
                    }

                    Installer userInstaller = installationService.findInstaller(null, wid, ctx.getRequestUserId());
                    if (userInstaller == null) {
                        ctx.client()
                                .viewsOpen(ViewsOpenRequest
                                        .builder()
                                        .token(ctx.getBotToken())
                                        .triggerId(ctx.getTriggerId())
                                        .view(
                                                view(
                                                        view -> view.type("modal")
                                                                .notifyOnClose(false)
                                                                .title(viewTitle(title -> title.type("plain_text").text(modalTitle)))
                                                                .callbackId(SlackActions.GENERIC_ACTION.getEntrypoint())
                                                                .close(viewClose(close -> close.type("plain_text").text("Cancel")))
                                                                .blocks(asBlocks(
                                                                        header(h -> h.text(plainText("Seems you did not install ONLYOFFICE App"))),
                                                                        section(s -> s.text(
                                                                                markdownText("Please go to *<"+integrationConfiguration.getInstallUrl()+"|ONLYOFFICE Installation>* page to install the App")
                                                                        ))
                                                                ))
                                                )
                                        )
                                        .build()
                                );
                        return;
                    }

                    Installer ownerInstaller = installationService.findInstaller(null, wid, message.getUser());
                    if (ownerInstaller == null) {
                        ctx.client()
                                .viewsOpen(ViewsOpenRequest
                                        .builder()
                                        .token(ctx.getBotToken())
                                        .triggerId(ctx.getTriggerId())
                                        .view(
                                                view(
                                                        view -> view.type("modal")
                                                                .notifyOnClose(false)
                                                                .title(viewTitle(title -> title.type("plain_text").text(modalTitle)))
                                                                .callbackId(SlackActions.GENERIC_ACTION.getEntrypoint())
                                                                .close(viewClose(close -> close.type("plain_text").text("Cancel")))
                                                                .blocks(asBlocks(
                                                                        header(h -> h.text(plainText("Seems the owner did not install ONLYOFFICE App"))),
                                                                        section(s -> s.text(
                                                                                markdownText("Please ask the owner to go to *<"+integrationConfiguration.getInstallUrl()+"|ONLYOFFICE Installation>* page to install the App")
                                                                        ))
                                                                ))
                                                )
                                        )
                                        .build()
                                );
                        return;
                    }

                    MessageShortcutPayload.User user = req.getPayload().getUser();
                    ScheduledOtp otp = otpGenerator.generateScheduledOtp(Caller.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .wid(user.getTeamId())
                            .token(userInstaller.getInstallerUserAccessToken())
                            .build());

                    ctx.client().viewsOpen(r -> r
                            .triggerId(ctx.getTriggerId())
                            .token(req.getContext().getBotToken())
                            .view(View
                                    .builder()
                                    .type("modal")
                                    .notifyOnClose(true)
                                    .callbackId(SlackActions.CLOSE_ONLYOFFICE_FILE_MODAL.getEntrypoint())
                                    .privateMetadata(otp.getCode())
                                    .title(viewTitle(title -> title.type("plain_text").text(modalTitle)))
                                    .close(viewClose(close -> close.type("plain_text").text("Close")))
                                    .blocks(getBlocks(message, user, otp))
                                    .build()
                            )
                    );
                } catch (IOException | SlackApiException | UnableToPerformSlackOperationException e) {
                    log.warn(e.getMessage());
                }
            }, TimeUnit.MILLISECONDS, 25);
            return ctx.ack();
        });
    }

    //TODO: No files UI
    private List<LayoutBlock> getBlocks(Message message, MessageShortcutPayload.User user, ScheduledOtp otp) {
        log.debug("Building file block for message: " + message.getClientMsgId());
        List<LayoutBlock> blocks = new ArrayList<>();
        try {
            message.getFiles().forEach(file -> {
                if (!fileConverter.fileSizeAllowed(file) || !file.isPublicUrlShared()) return;

                OnlyofficeEditorToken token = OnlyofficeEditorToken
                        .builder()
                        .owner(message.getUser())
                        .user(user.getId())
                        .userName(user.getName())
                        .workspace(message.getTeam())
                        .channel(message.getChannel())
                        .threadTs(message.getThreadTs() == null ? message.getTs() : message.getThreadTs())
                        .messageTs(message.getTs())
                        .file(file.getId())
                        .fileName(file.getName())
                        .url(linkConverter.transformDownloadUrl(
                                file.getPermalinkPublic(),
                                fileConverter.covertDownloadName(file)
                        ))
                        .otpCode(otp.getCode())
                        .otpAt(otp.getAt())
                        .otpChannel(otp.getChannel())
                        .build();

                blocks.add(header(
                        h -> h.text(plainText(file.getName()))
                ));
                blocks.add(
                        section(s -> s
                                .text(markdownText(
                                        String.format(
                                                "Document type: *%s*\nFile extension: *%s*\nSize: *%s*\nCreated: *%s*",
                                                fileUtil.findDocumentType(file.getName()).getType(),
                                                fileUtil.findFileType(file.getName()),
                                                fileConverter.convertFileSize(file),
                                                fileConverter.convertFileTimestamp(file)
                                        )
                                ))
                                .accessory(BlockElements.image(i -> i
                                        .altText(file.getName())
                                        .imageUrl(fileConverter.convertFileIconUrl(file)))
                                )
                        )
                );

                LocalDate date = LocalDate.now().plusDays(1);
                Optional<String> signature = this.jwtSecurity.sign(
                        token, integrationConfiguration.getEditorSecret(),
                        Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
                );
                if (signature.isEmpty()) return;

                ButtonElement openButton = button(b -> b
                        .text(plainText("Open in ONLYOFFICE"))
                        .actionId(SlackActions.OPEN_ONLYOFFICE_FILE.getEntrypoint())
                        .value(file.getId())
                        .url(String.format("%s?token=%s",
                                integrationConfiguration.getEditorUrl().trim(), signature.get()))
                        .style("primary"));

                if (message.getUser().equals(user.getId())) {
                    String threadTs = message.getThreadTs() == null ? message.getTs() : message.getThreadTs();
                    blocks.add(
                            actions(a -> a.elements(
                                    List.of(
                                            openButton,
                                            button(b -> b
                                                    .text(plainText("Change Access"))
                                                    .value(String.format("%s;%s;%s;%s;%s;%s",
                                                            otp.getCode(),
                                                            file.getName(), file.getId(),
                                                            message.getChannel(), threadTs,
                                                            message.getTs())
                                                    )
                                                    .actionId(SlackActions.ONLYOFFICE_FILE_PERMISSIONS.getEntrypoint())
                                            )
                                    )
                            ))
                    );
                } else {
                    blocks.add(
                            actions(a -> a.elements(List.of(openButton)))
                    );
                }
            });
        } catch (UnableToPerformSlackOperationException e) {
            log.warn(e.getMessage());
            return null;
        }

        if (blocks.size() < 1) return asBlocks(
                divider(),
                section(s -> s.text(markdownText("Could not find any supported or public file"))),
                divider());

        return blocks;
    }

    public SlackOperations getSlackRequestHandler() {
        return SlackActions.OPEN_ONLYOFFICE_FILE;
    }
}
