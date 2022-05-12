package com.onlyoffice.slack.handler.submission;

import com.onlyoffice.slack.SlackActions;
import com.onlyoffice.slack.handler.SlackHandler;
import com.onlyoffice.slack.handler.action.SlackOpenOnlyofficeSettingsAction;
import com.onlyoffice.slack.model.registry.License;
import com.onlyoffice.slack.SlackOperations;
import com.onlyoffice.slack.service.registry.SlackOnlyofficeRegistryInstallationService;
import com.onlyoffice.slack.util.SlackLinkConverter;
import com.slack.api.bolt.App;
import com.slack.api.model.view.ViewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackSettingsSubmission implements SlackHandler {
    private final SlackOnlyofficeRegistryInstallationService installationService;
    private final SlackLinkConverter linkConverter;

    @Autowired
    public void register(App app) {
        app.viewSubmission(getSlackRequestHandler().getEntrypoint(), (req, ctx) -> {
            log.debug("Submitting new settings");
            Map<String, Map<String, ViewState.Value>> values = req.getPayload()
                    .getView().getState().getValues();

            License license = License
                    .builder()
                    .serverUrl(
                            linkConverter.removeSlash(
                                    values
                                            .get(SlackOpenOnlyofficeSettingsAction.urlKey)
                                            .get(SlackOpenOnlyofficeSettingsAction.urlKey)
                                            .getValue()
                            )
                    )
                    .serverSecret(
                            values
                                    .get(SlackOpenOnlyofficeSettingsAction.secretKey)
                                    .get(SlackOpenOnlyofficeSettingsAction.secretKey)
                                    .getValue()
                    )
                    .serverHeader(
                            values
                                    .get(SlackOpenOnlyofficeSettingsAction.headerKey)
                                    .get(SlackOpenOnlyofficeSettingsAction.headerKey)
                                    .getValue()
                    )
                    .build();

            //TODO: Display errors according to Slack guidelines
            if (!installationService.saveLicense(ctx.getTeamId(), license))
                return ctx.ackWithErrors(Map.of("error", "Could not update workspace license information. Please try again later"));

            return ctx.ack();
        });
    }

    public SlackOperations getSlackRequestHandler() {
        return SlackActions.OPEN_SETTINGS;
    }
}
