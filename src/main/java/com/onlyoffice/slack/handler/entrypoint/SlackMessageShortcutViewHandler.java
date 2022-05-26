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
import com.onlyoffice.slack.service.slack.SlackLocaleService;
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
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
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
    private final MessageSource messageSource;
    private final SlackLocaleService slackLocaleService;
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

                Locale locale = slackLocaleService.getLocale(Caller
                        .builder()
                                .name(ctx.getRequestUserId())
                                .id(ctx.getRequestUserId())
                                .wid(ctx.getTeamId())
                                .isRoot(false)
                                .token(ctx.getBotToken())
                        .build()
                );

                try {
                    String modalTitle = messageSource.getMessage("file.modal.title", null, locale);
                    String cancelButton = messageSource.getMessage("file.modal.cancel", null, locale);

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
                                                                .close(viewClose(close -> close.type("plain_text").text(cancelButton)))
                                                                .blocks(asBlocks(
                                                                        header(h -> h.text(plainText(messageSource.getMessage("file.modal.workspace.error.header", null, locale)))),
                                                                        section(s -> s.text(
                                                                                markdownText(String.format(messageSource.getMessage("file.modal.workspace.error.body", null, locale), ctx.getTeamId()))
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
                                                                .close(viewClose(close -> close.type("plain_text").text(cancelButton)))
                                                                .blocks(asBlocks(
                                                                        header(h -> h.text(plainText(messageSource.getMessage("file.modal.user.error.header", null, locale)))),
                                                                        section(s -> s.text(
                                                                                markdownText(String.format(messageSource.getMessage("file.modal.user.error.body", null, locale), integrationConfiguration.getInstallUrl()))
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
                                                                .close(viewClose(close -> close.type("plain_text").text(cancelButton)))
                                                                .blocks(asBlocks(
                                                                        header(h -> h.text(plainText(messageSource.getMessage("file.modal.owner.error.header", null, locale)))),
                                                                        section(s -> s.text(
                                                                                markdownText(String.format(messageSource.getMessage("file.modal.owner.error.body", null, locale), integrationConfiguration.getInstallUrl()))
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
                                    .close(viewClose(close -> close.type("plain_text").text(cancelButton)))
                                    .blocks(getBlocks(message, user, otp, locale))
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

    private List<LayoutBlock> getBlocks(Message message, MessageShortcutPayload.User user, ScheduledOtp otp, Locale locale) {
        log.debug("Building file block for message: " + message.getClientMsgId());
        List<LayoutBlock> blocks = new ArrayList<>();

        String fileInfo = messageSource.getMessage("file.modal.blocks.info", null, locale);
        String noblocksHeader = messageSource.getMessage("file.modal.noblocks.header", null, locale);
        String noblocksHeaderContext = messageSource.getMessage("file.modal.noblocks.header.context", null, locale);

        String sectionsFirst = messageSource.getMessage("file.modal.noblocks.sections.first", null, locale);
        String sectionsSecond = messageSource.getMessage("file.modal.noblocks.sections.second", null, locale);
        String sectionsThird = messageSource.getMessage("file.modal.noblocks.sections.third", null, locale);
        String sectionsFourth = messageSource.getMessage("file.modal.noblocks.sections.fourth", null, locale);

        String footerContext = messageSource.getMessage("file.modal.noblocks.footer.context", null, locale);

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
                                                fileInfo,
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
                        .text(plainText(messageSource.getMessage("file.modal.blocks.button.open", null, locale)))
                        .actionId(SlackActions.OPEN_ONLYOFFICE_FILE.getEntrypoint())
                        .value(file.getId())
                        .url(String.format("%s?token=%s&lang=%s",
                                integrationConfiguration.getEditorUrl().trim(), signature.get(), locale))
                        .style("primary"));

                if (message.getUser().equals(user.getId())) {
                    String threadTs = message.getThreadTs() == null ? message.getTs() : message.getThreadTs();
                    blocks.add(
                            actions(a -> a.elements(
                                    List.of(
                                            openButton,
                                            button(b -> b
                                                    .text(plainText(messageSource.getMessage("file.modal.blocks.button.access", null, locale)))
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
                header(h -> h.text(plainText(noblocksHeader))),
                divider(),
                context(List.of(
                        markdownText(noblocksHeaderContext)
                )),
                section(s -> s.text(markdownText(sectionsFirst))),
                section(s -> s.text(markdownText(sectionsSecond))),
                section(s -> s.text(markdownText(sectionsThird))),
                section(s -> s.text(markdownText(sectionsFourth))),
                divider(),
                context(List.of(
                        markdownText(String.format(footerContext, integrationConfiguration.getFileSizeLimitMb()))
                ))
        );

        return blocks;
    }

    public SlackOperations getSlackRequestHandler() {
        return SlackActions.OPEN_ONLYOFFICE_FILE;
    }
}
