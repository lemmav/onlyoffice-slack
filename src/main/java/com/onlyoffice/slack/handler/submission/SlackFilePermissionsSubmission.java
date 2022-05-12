package com.onlyoffice.slack.handler.submission;

import com.onlyoffice.slack.SlackActions;
import com.onlyoffice.slack.handler.SlackHandler;
import com.onlyoffice.slack.handler.action.SlackOpenOnlyofficePermissionsButtonAction;
import com.onlyoffice.slack.model.slack.permission.UpdateFilePermissionRequest;
import com.onlyoffice.slack.SlackOperations;
import com.onlyoffice.slack.service.slack.SlackFilePermissionsService;
import com.slack.api.bolt.App;
import com.slack.api.model.view.ViewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackFilePermissionsSubmission implements SlackHandler {
    private final SlackFilePermissionsService filePermissionsService;

    @Autowired
    public void register(App app) {
        app.viewSubmission(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            log.debug("Submitting file permissions");
            String[] fileInfo = req.getPayload().getView().getPrivateMetadata().split(";");

            List<String> users = req.getPayload().getView().getState().getValues()
                    .get(SlackOpenOnlyofficePermissionsButtonAction.usersBlock)
                    .get(SlackActions.GENERIC_ACTION.getEntrypoint())
                    .getSelectedUsers();

            ViewState.SelectedOption permission = req.getPayload().getView().getState().getValues()
                    .get(SlackOpenOnlyofficePermissionsButtonAction.permissionBlock)
                    .get(SlackActions.GENERIC_ACTION.getEntrypoint())
                    .getSelectedOption();

            filePermissionsService.updatePermissions(UpdateFilePermissionRequest
                    .builder()
                            .user(ctx.getRequestUserId())
                            .file(fileInfo[2])
                            .channel(fileInfo[3])
                            .defaultPermission(permission == null ? "read" : permission.getValue())
                            .sharedUsers(users)
                            .threadTs(fileInfo[4])
                            .messageTs(fileInfo[5])
                            .team(ctx.getTeamId())
                    .build()
            );

            return ctx.ack();
        });
    }

    public SlackOperations getSlackRequestHandler() {
        return SlackActions.ONLYOFFICE_FILE_PERMISSIONS;
    }
}
