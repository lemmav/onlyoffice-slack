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
public class SlackFileModalClose implements SlackHandler {
    private final SlackOtpGeneratorService otpGenerator;

    @Autowired
    public void register(App app) {
        app.viewClosed(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            log.debug("Removing a scheduled otp");
            otpGenerator.removeScheduledOtp(
                    req.getPayload().getView().getPrivateMetadata(),
                    Caller
                            .builder()
                            .id(ctx.getRequestUserId())
                            .name(ctx.getRequestUserId())
                            .wid(ctx.getTeamId())
                            .build()
            );
            return ctx.ack();
        });
    }

    public SlackOperations getSlackRequestHandler() {
        return SlackActions.CLOSE_ONLYOFFICE_FILE_MODAL;
    }
}
