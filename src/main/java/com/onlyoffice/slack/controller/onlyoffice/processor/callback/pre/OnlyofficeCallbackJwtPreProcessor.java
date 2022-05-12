package com.onlyoffice.slack.controller.onlyoffice.processor.callback.pre;

import com.google.common.collect.ImmutableMap;
import com.onlyoffice.slack.model.onlyoffice.OnlyofficeCallbackToken;
import com.onlyoffice.slack.model.registry.Workspace;
import com.onlyoffice.slack.service.registry.SlackOnlyofficeRegistryInstallationService;
import core.model.callback.Callback;
import core.processor.preprocessor.OnlyofficeCallbackPreProcessor;
import exception.OnlyofficeInvalidParameterRuntimeException;
import exception.OnlyofficeProcessBeforeRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OnlyofficeCallbackJwtPreProcessor extends OnlyofficeCallbackPreProcessor<CallbackJwtSchema> {
    private static final String tokenPrefix = "Bearer ";
    private static final String tokenReplacer = "";

    private final SlackOnlyofficeRegistryInstallationService installationService;

    public CallbackJwtSchema validateSchema(Map<String, Object> customData, ImmutableMap<String, Object> schema) {
        try {
            OnlyofficeCallbackToken token = (OnlyofficeCallbackToken) customData.get("callbackToken");
            if (token == null)
                throw new ClassCastException("Expected to get an OnlyofficeCalbackToken instance. Got null");

            Workspace workspace = this.installationService.findWorkspace(token.getWorkspace());
            if (workspace == null || workspace.getServerSecret() == null || workspace.getServerSecret().isBlank())
                throw new OnlyofficeProcessBeforeRuntimeException("Expected to get an instance of Workspace. Got null");

            String headerToken = schema.get(workspace.getServerHeader().toLowerCase())
                    .toString().replace(tokenPrefix, tokenReplacer);

            return new CallbackJwtSchema(workspace.getServerSecret(), headerToken);
        } catch (ClassCastException e) {
            throw new OnlyofficeProcessBeforeRuntimeException(e.getMessage());
        }
    }

    public void processBefore(Callback callback, CallbackJwtSchema callbackJwtSchema) throws OnlyofficeProcessBeforeRuntimeException, OnlyofficeInvalidParameterRuntimeException {
        if (callback.getToken() == null || callback.getToken().isBlank()) {
            callback.setToken(callbackJwtSchema.getToken());
        }
        callback.setSecret(callbackJwtSchema.getSecret());
    }

    public String preprocessorName() {
        return "onlyoffice.preprocessor.slack.callback.validator";
    }
}
