package com.onlyoffice.slack.controller.onlyoffice.processor.callback;

import com.onlyoffice.slack.model.onlyoffice.OnlyofficeCallbackToken;
import com.onlyoffice.slack.service.slack.OnlyofficeDocKeyGeneratorService;
import com.onlyoffice.slack.service.registry.SlackOnlyofficeRegistryInstallationService;
import com.slack.api.bolt.App;
import com.slack.api.bolt.model.Installer;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.files.FilesUploadRequest;
import com.slack.api.methods.response.files.FilesUploadResponse;
import core.model.callback.Callback;
import core.uploader.OnlyofficeUploader;
import core.uploader.OnlyofficeUploaderType;
import exception.OnlyofficeRegistryHandlerRuntimeException;
import exception.OnlyofficeUploaderRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnlyofficeCallbackUploaderDocKeyProcessor implements OnlyofficeUploader<Callback> {
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final App app;
    private final SlackOnlyofficeRegistryInstallationService installationService;
    private final OnlyofficeDocKeyGeneratorService docKeyGeneratorService;

    public void upload(Callback callback, InputStream inputStream) throws OnlyofficeUploaderRuntimeException {
        if (!callback.getCustom().containsKey("callbackToken"))
            throw new OnlyofficeUploaderRuntimeException("Expected to find an OnlyofficeCallbackToken (custom -> callbackToken) instance. Got null");

        log.debug("trying to upload a file");
        OnlyofficeCallbackToken token = (OnlyofficeCallbackToken) callback.getCustom().get("callbackToken");
        Installer user = installationService.findInstaller(null, token.getWorkspace(), token.getUser());
        Installer owner = installationService.findInstaller(null, token.getWorkspace(), token.getOwner());
        if (user == null || owner == null)
            throw new OnlyofficeUploaderRuntimeException("Expected to fetch owner and user installations. Got null");

        try {
            File file = File.createTempFile(callback.getKey(), token.getFileName());
            try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
                int read;
                byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            }

            FilesUploadRequest request = FilesUploadRequest
                    .builder()
                    .token(user.getInstallerUserAccessToken())
                    .channels(List.of(token.getChannel()))
                    .threadTs(token.getTs())
                    .file(file)
                    .filename(token.getFileName())
                    .title(token.getFileName())
                    .initialComment(String.format("Previous version: %s",token.getPermalinkUrl()))
                    .build();
            FilesUploadResponse response = app.client().filesUpload(request);

            file.delete();
            if (!response.isOk())
                throw new OnlyofficeRegistryHandlerRuntimeException("Could not upload a new file");

            if (callback.getStatus() == 2) docKeyGeneratorService
                    .publishDocKey(owner.getInstallerUserAccessToken(), token.getFile(), UUID.randomUUID().toString());

        } catch (SlackApiException | IOException e) {
            throw new OnlyofficeUploaderRuntimeException(e.getMessage());
        }
    }

    @Override
    public OnlyofficeUploaderType getUploaderType() {
        return OnlyofficeUploaderType.FILE;
    }
}
