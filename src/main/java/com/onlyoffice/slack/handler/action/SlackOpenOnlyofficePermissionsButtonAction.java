package com.onlyoffice.slack.handler.action;

import com.onlyoffice.slack.SlackActions;
import com.onlyoffice.slack.SlackOperations;
import com.onlyoffice.slack.handler.SlackHandler;
import com.onlyoffice.slack.model.slack.Caller;
import com.onlyoffice.slack.model.slack.permission.FilePermissionRequest;
import com.onlyoffice.slack.model.slack.permission.FilePermissionResponse;
import com.onlyoffice.slack.service.slack.SlackFilePermissionsService;
import com.onlyoffice.slack.service.slack.SlackLocaleService;
import com.slack.api.bolt.App;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.MultiUsersSelectElement;
import com.slack.api.model.block.element.StaticSelectElement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.view.Views.*;

@Component
@AllArgsConstructor
@Slf4j
public class SlackOpenOnlyofficePermissionsButtonAction implements SlackHandler {
    public static final String usersBlock = "users_block";
    public static final String permissionBlock = "permission_block";

    private final SlackLocaleService slackLocaleService;
    private final MessageSource messageSource;
    private final SlackFilePermissionsService filePermissionsService;

    @Autowired
    public void register(App app) {
        app.blockAction(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            String value = req.getPayload().getActions().get(0).getValue();
            if (value == null || value.isBlank() || value.split(";").length != 6) {
                log.warn("invalid file info string");
                return ctx.ack();
            }

            String[] fileInfo = value.split(";");
            FilePermissionResponse permissions = filePermissionsService.getPermissionsAttachment(FilePermissionRequest
                    .builder()
                        .user(req.getContext().getRequestUserId())
                        .team(req.getContext().getTeamId())
                        .file(fileInfo[2])
                        .channel(fileInfo[3])
                        .threadTs(fileInfo[4])
                        .messageTs(fileInfo[5])
                    .build()
            );

            Locale locale = slackLocaleService.getLocale(Caller
                    .builder()
                            .id(ctx.getRequestUserId())
                            .name(ctx.getRequestUserId())
                            .wid(ctx.getTeamId())
                            .isRoot(false)
                            .token(ctx.getBotToken())
                    .build()
            );

            String permissionsTitle = messageSource.getMessage("file.modal.permissions.title", null, locale);
            String saveButton = messageSource.getMessage("file.modal.save", null, locale);
            String sectionText = messageSource.getMessage("file.modal.permissions.main.header", null, locale);
            String permissionPlaceholder = messageSource.getMessage("file.modal.permissions.main.permission.placeholder", null, locale);
            String permissionRead = messageSource.getMessage("file.modal.permissions.main.permission.read", null, locale);
            String permissionEdit = messageSource.getMessage("file.modal.permissions.main.permission.edit", null, locale);
            String selectUsers = messageSource.getMessage("file.modal.permissions.main.permission.select", null, locale);
            String selectPlaceholder = messageSource.getMessage("file.modal.permissions.main.permission.select.placeholder", null, locale);

            ctx.client().viewsPush(r -> r
                    .triggerId(ctx.getTriggerId())
                    .token(ctx.getBotToken())
                    .view(view(view -> view
                            .type("modal")
                            .notifyOnClose(true)
                            .clearOnClose(true)
                            .title(viewTitle(title -> title.type("plain_text").text(permissionsTitle)))
                            .submit(viewSubmit(submit -> submit.type("plain_text").text(saveButton)))
                            .callbackId(getSlackRequestHandler().getEntrypoint())
                            .privateMetadata(value)
                            .blocks(asBlocks(
                                    divider(),
                                    header(h -> h.text(plainText(fileInfo[1]))),
                                    section(s -> s
                                            .blockId(permissionBlock)
                                            .text(markdownText(sectionText))
                                            .accessory(StaticSelectElement.builder()
                                                    .actionId(SlackActions.GENERIC_ACTION.getEntrypoint())
                                                    .placeholder(plainText(permissionPlaceholder))
                                                    .initialOption(OptionObject
                                                            .builder()
                                                            .text(plainText(permissions.getDefaultPermission().substring(0,1).toUpperCase() +
                                                                    permissions.getDefaultPermission().substring(1)))
                                                            .value(permissions.getDefaultPermission())
                                                            .build()
                                                    )
                                                    .options(List.of(
                                                            OptionObject
                                                                    .builder()
                                                                    .text(plainText(permissionRead))
                                                                    .value("read")
                                                                    .build(),
                                                            OptionObject
                                                                    .builder()
                                                                    .text(plainText(permissionEdit))
                                                                    .value("edit")
                                                                    .build()
                                                    ))
                                                    .build())
                                    ),
                                    input(i -> i
                                            .blockId(usersBlock)
                                            .element(MultiUsersSelectElement
                                            .builder()
                                                    .actionId(SlackActions.GENERIC_ACTION.getEntrypoint())
                                                    .initialUsers(permissions.getSharedUsers())
                                                    .placeholder(plainText(selectPlaceholder))
                                            .build())
                                            .label(plainText(selectUsers))
                                            .optional(true)
                                    )
                            ))))
                    .token(ctx.getBotToken())
            );

            return ctx.ack();
        });
    }

    public SlackOperations getSlackRequestHandler() {
        return SlackActions.ONLYOFFICE_FILE_PERMISSIONS;
    }
}
