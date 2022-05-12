package com.onlyoffice.slack.handler.submission;

import com.onlyoffice.slack.SlackActions;
import com.onlyoffice.slack.SlackOperations;
import com.onlyoffice.slack.handler.SlackHandler;
import com.onlyoffice.slack.model.slack.Caller;
import com.onlyoffice.slack.service.slack.SlackOtpGeneratorService;
import com.slack.api.bolt.App;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackFilePermissionsClose implements SlackHandler {
    private final SlackOtpGeneratorService otpGeneratorService;

    @Autowired
    public void register(App app) {
        app.viewClosed(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            log.debug("Closing file permissions");
            String[] fileInfo = req.getPayload().getView().getPrivateMetadata().split(";");

            otpGeneratorService.removeScheduledOtp(fileInfo[0], Caller
                    .builder()
                            .wid(ctx.getTeamId())
                            .id(ctx.getRequestUserId())
                            .name(ctx.getRequestUserId())
                    .build());

            return ctx.ack();
        });
    }

    public SlackOperations getSlackRequestHandler() {
        return SlackActions.ONLYOFFICE_FILE_PERMISSIONS;
    }
}
