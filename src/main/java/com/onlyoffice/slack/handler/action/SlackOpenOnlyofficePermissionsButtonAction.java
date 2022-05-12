package com.onlyoffice.slack.handler.action;

import com.onlyoffice.slack.SlackActions;
import com.onlyoffice.slack.SlackOperations;
import com.onlyoffice.slack.handler.SlackHandler;
import com.onlyoffice.slack.model.slack.permission.FilePermissionRequest;
import com.onlyoffice.slack.model.slack.permission.FilePermissionResponse;
import com.onlyoffice.slack.service.slack.SlackFilePermissionsService;
import com.slack.api.bolt.App;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.MultiUsersSelectElement;
import com.slack.api.model.block.element.StaticSelectElement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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

    private final SlackFilePermissionsService filePermissionsService;

    @Autowired
    public void register(App app) {
        app.blockAction(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            String value = req.getPayload().getActions().get(0).getValue();
            if (value == null || value.isBlank() || value.split(";").length != 6) {
                log.warn("Invalid file info string");
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

            ctx.client().viewsPush(r -> r
                    .triggerId(ctx.getTriggerId())
                    .token(ctx.getBotToken())
                    .view(view(view -> view
                            .type("modal")
                            .notifyOnClose(true)
                            .clearOnClose(true)
                            .title(viewTitle(title -> title.type("plain_text").text("ONLYOFFICE Permissions")))
                            .submit(viewSubmit(submit -> submit.type("plain_text").text("Save")))
                            .callbackId(getSlackRequestHandler().getEntrypoint())
                            .privateMetadata(value)
                            .blocks(asBlocks(
                                    divider(),
                                    header(h -> h.text(plainText(fileInfo[1]))),
                                    input(i -> i
                                            .blockId(usersBlock)
                                            .element(MultiUsersSelectElement
                                            .builder()
                                                    .actionId(SlackActions.GENERIC_ACTION.getEntrypoint())
                                                    .initialUsers(permissions.getSharedUsers())
                                                    .placeholder(plainText("Select users"))
                                            .build())
                                            .label(plainText("Share this file with"))
                                            .optional(true)
                                    ),
                                    section(s -> s
                                            .blockId(permissionBlock)
                                            .text(markdownText("Default access rights for chat members"))
                                            .accessory(StaticSelectElement.builder()
                                                    .actionId(SlackActions.GENERIC_ACTION.getEntrypoint())
                                                    .placeholder(plainText("Choose"))
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
                                                                    .text(plainText("Read"))
                                                                    .value("read")
                                                                    .build(),
                                                            OptionObject
                                                                    .builder()
                                                                    .text(plainText("Edit"))
                                                                    .value("edit")
                                                                    .build()
                                                    ))
                                                    .build())
                                    ),
                                    divider()
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
