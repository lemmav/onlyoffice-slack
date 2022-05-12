package com.onlyoffice.slack.controller.onlyoffice.processor.editor.pre;

import com.google.common.collect.ImmutableMap;
import com.onlyoffice.slack.model.onlyoffice.OnlyofficeEditorToken;
import com.onlyoffice.slack.model.slack.Caller;
import com.onlyoffice.slack.service.registry.SlackOnlyofficeRegistryInstallationService;
import com.onlyoffice.slack.service.slack.OnlyofficeDocKeyGeneratorService;
import com.onlyoffice.slack.service.slack.SlackOtpGeneratorService;
import com.slack.api.bolt.model.Installer;
import com.slack.api.methods.SlackApiException;
import core.model.config.Config;
import core.model.config.document.Permissions;
import core.processor.preprocessor.OnlyofficeEditorPreProcessor;
import exception.OnlyofficeInvalidParameterRuntimeException;
import exception.OnlyofficeProcessBeforeRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnlyofficeEditorDocKeyPreProcessor extends OnlyofficeEditorPreProcessor<OnlyofficeEditorToken> {
    private final SlackOnlyofficeRegistryInstallationService installationService;
    private final SlackOtpGeneratorService otpGenerator;
    private final OnlyofficeDocKeyGeneratorService docKeyGeneratorService;

    public OnlyofficeEditorToken validateSchema(Map<String, Object> customData, ImmutableMap<String, Object> schema) {
        if (!customData.containsKey("editorToken")) return null;
        try {
            OnlyofficeEditorToken token = (OnlyofficeEditorToken) customData.get("editorToken");
            if (token == null) return null;
            return token;
        } catch (ClassCastException e) {
            return null;
        }
    }

    public void processBefore(Config config, OnlyofficeEditorToken token) throws OnlyofficeProcessBeforeRuntimeException, OnlyofficeInvalidParameterRuntimeException {
        Installer owner = installationService.findInstaller(null, token.getWorkspace(), token.getOwner());
        Installer user = installationService.findInstaller(null, token.getWorkspace(), token.getUser());

        if (user == null || owner == null)
            throw new OnlyofficeProcessBeforeRuntimeException("Expected to find owner and user. Got null");

        log.debug("Received an OTP: {}", token.getOtpCode());
        boolean isValid = otpGenerator.removeScheduledOtp(token.getOtpCode(), Caller
                .builder()
                .id(token.getUser())
                .name(token.getUserName())
                .wid(token.getWorkspace())
                .build()
        );

        if (!isValid)
            throw new OnlyofficeProcessBeforeRuntimeException("OTP validation exception. This scheduled message does not exist");

        try {
            String publishedDocKey = docKeyGeneratorService
                    .findGeneratedDocKey(owner.getInstallerUserAccessToken(), token.getFile());

            if (publishedDocKey != null && !publishedDocKey.isBlank()) {
                config.getDocument().setKey(publishedDocKey);
                return;
            }

            String newDocKey = UUID.randomUUID().toString();
            docKeyGeneratorService
                    .publishDocKey(owner.getInstallerUserAccessToken(), token.getFile(), newDocKey);
            config.getDocument().setKey(newDocKey);

        } catch (IOException | SlackApiException e) {
            config.getDocument().setPermissions(Permissions.builder().edit(false).build());
            config.getDocument().setKey(UUID.randomUUID().toString());
        }
    }

    public String preprocessorName() {
        return "onlyoffice.preprocessor.slack.editor.docKey";
    }
}
