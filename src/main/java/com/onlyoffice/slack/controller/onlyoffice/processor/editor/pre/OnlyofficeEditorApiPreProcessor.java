package com.onlyoffice.slack.controller.onlyoffice.processor.editor.pre;

import com.google.common.collect.ImmutableMap;
import com.onlyoffice.slack.configuration.general.IntegrationConfiguration;
import com.onlyoffice.slack.exception.UnableToPerformSlackOperationException;
import com.onlyoffice.slack.model.onlyoffice.OnlyofficeCallbackToken;
import com.onlyoffice.slack.model.onlyoffice.OnlyofficeEditorToken;
import com.onlyoffice.slack.model.registry.Workspace;
import com.onlyoffice.slack.model.slack.Caller;
import com.onlyoffice.slack.model.slack.ScheduledOtp;
import com.onlyoffice.slack.model.slack.permission.FilePermissionRequest;
import com.onlyoffice.slack.service.registry.SlackOnlyofficeRegistryInstallationService;
import com.onlyoffice.slack.service.slack.SlackFilePermissionsService;
import com.onlyoffice.slack.service.slack.SlackOtpGeneratorService;
import core.model.config.Config;
import core.model.config.document.Permissions;
import core.processor.preprocessor.OnlyofficeEditorPreProcessor;
import core.security.OnlyofficeJwtSecurity;
import core.util.OnlyofficeFile;
import exception.OnlyofficeInvalidParameterRuntimeException;
import exception.OnlyofficeJwtSigningRuntimeException;
import exception.OnlyofficeProcessBeforeRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnlyofficeEditorApiPreProcessor extends OnlyofficeEditorPreProcessor<OnlyofficeEditorToken> {
    private static final String postfixLocation = "/web-apps/apps/api/documents/api.js";

    private final IntegrationConfiguration integrationConfiguration;
    private final SlackOnlyofficeRegistryInstallationService installationService;
    private final OnlyofficeJwtSecurity jwtSecurity;
    private final SlackOtpGeneratorService otpGenerator;
    private final SlackFilePermissionsService filePermissionsService;
    private final OnlyofficeFile onlyofficeFileUtil;

    public OnlyofficeEditorToken validateSchema(Map<String, Object> customData, ImmutableMap<String, Object> schema) {
        log.debug("validating editor token's schema");
        if (!customData.containsKey("editorToken")) return null;
        try {
            OnlyofficeEditorToken token = (OnlyofficeEditorToken) customData.get("editorToken");
            if (token == null) return null;
            return token;
        } catch (ClassCastException e) {
            log.error("could not cast to editor token type: {}", e.getMessage());
            return null;
        }
    }

    public void processBefore(Config config, OnlyofficeEditorToken token) throws OnlyofficeProcessBeforeRuntimeException, OnlyofficeInvalidParameterRuntimeException {
        try {
            Workspace workspace = installationService.findWorkspace(token.getWorkspace());

            if (workspace == null || workspace.getServerSecret() == null || workspace.getServerSecret().isBlank())
                throw new UnableToPerformSlackOperationException("Expected to find a workspace secret. Got null");

            try {
                boolean editAllowed = filePermissionsService.hasEditPermissions(
                        FilePermissionRequest
                                .builder()
                                .channel(token.getChannel())
                                .file(token.getFile())
                                .threadTs(token.getThreadTs())
                                .messageTs(token.getMessageTs())
                                .team(token.getWorkspace())
                                .user(token.getUser())
                                .build()
                ) && onlyofficeFileUtil.isEditable(token.getFileName());

                config.getDocument().setPermissions(Permissions
                        .builder()
                                .edit(editAllowed)
                        .build()
                );

                String callbackToken;

                if (!editAllowed) {
                    callbackToken = jwtSecurity.sign(
                            OnlyofficeCallbackToken
                                    .builder()
                                    .owner(token.getOwner())
                                    .user(token.getUser())
                                    .workspace(token.getWorkspace())
                                    .file(token.getFile())
                                    .fileName(token.getFileName())
                                    .channel(token.getChannel())
                                    .ts(token.getThreadTs())
                                    .otpCode(UUID.randomUUID().toString())
                                    .otpChannel(UUID.randomUUID().toString())
                                    .permalinkUrl(token.getPermalinkUrl())
                                    .otpAt(1)
                                    .build(),
                            integrationConfiguration.getCallbackSecret(),
                            Date.valueOf(LocalDate.now().plusDays(1))
                    ).get();
                } else {
                    ScheduledOtp callbackOtp = otpGenerator.generateScheduledOtp(Caller
                            .builder()
                            .id(token.getUser())
                            .name(token.getUserName())
                            .wid(token.getWorkspace())
                            .build()
                    );

                    callbackToken = jwtSecurity.sign(
                            OnlyofficeCallbackToken
                                    .builder()
                                    .owner(token.getOwner())
                                    .user(token.getUser())
                                    .workspace(token.getWorkspace())
                                    .file(token.getFile())
                                    .fileName(token.getFileName())
                                    .channel(token.getChannel())
                                    .ts(token.getThreadTs())
                                    .otpCode(callbackOtp.getCode())
                                    .otpChannel(callbackOtp.getChannel())
                                    .otpAt(callbackOtp.getAt())
                                    .permalinkUrl(token.getPermalinkUrl())
                                    .build(),
                            integrationConfiguration.getCallbackSecret(),
                            Date.valueOf(LocalDate.now().plusDays(1))
                    ).get();
                }

                config.getEditorConfig().setCallbackUrl(String.format("%s?token=%s",
                        integrationConfiguration.getCallbackUrl(), callbackToken)
                );
                config.setSecret(workspace.getServerSecret());

                Map<String, Object> custom = config.getCustom();
                custom.put("apijs", workspace.getServerUrl() + postfixLocation);
            } catch (OnlyofficeJwtSigningRuntimeException e) {
                throw new OnlyofficeProcessBeforeRuntimeException(e.getMessage(), e);
            }
        } catch (ClassCastException | UnableToPerformSlackOperationException e) {
            throw new OnlyofficeProcessBeforeRuntimeException("Expected to find an instance of OnlyofficeEditorToken (custom -> editorToken). Got unknown", e);
        }
    }

    public String preprocessorName() {
        return "onlyoffice.preprocessor.slack.editor.api";
    }
}
