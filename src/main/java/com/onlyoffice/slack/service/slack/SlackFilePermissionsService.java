package com.onlyoffice.slack.service.slack;

import com.onlyoffice.slack.model.slack.permission.FilePermissionRequest;
import com.onlyoffice.slack.model.slack.permission.FilePermissionResponse;
import com.onlyoffice.slack.model.slack.permission.UpdateFilePermissionRequest;
import com.onlyoffice.slack.service.registry.SlackOnlyofficeRegistryInstallationService;
import com.slack.api.bolt.App;
import com.slack.api.bolt.model.Installer;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatUpdateRequest;
import com.slack.api.methods.request.conversations.ConversationsRepliesRequest;
import com.slack.api.methods.request.files.FilesInfoRequest;
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse;
import com.slack.api.methods.response.files.FilesInfoResponse;
import com.slack.api.model.Attachment;
import com.slack.api.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackFilePermissionsService {
    private static final String attachmentText = "ONLYOFFICE %s permissions";
    private static final String attachmentFooter = "%s:%s";
    private static final String color = "#c2c4c4";

    private final App app;
    private final SlackOnlyofficeRegistryInstallationService installationService;

    public void updatePermissions(UpdateFilePermissionRequest request) {
        if (request == null || !request.validate()) {
            log.warn("Invalid FilePermissionUpdateRequest instance: {}", request);
            return;
        }

        Installer owner = installationService
                .findInstaller(null, request.getTeam(), request.getUser());
        if (owner == null) return;

        try {
            FilesInfoResponse fileInfo = app.client().filesInfo(FilesInfoRequest
                    .builder()
                            .file(request.getFile())
                            .token(owner.getInstallerUserAccessToken())
                    .build()
            );

            if (!fileInfo.isOk()) {
                log.warn("Bad file info request: {}", fileInfo.getError());
                return;
            }

            ConversationsRepliesResponse messages = app.client().conversationsReplies(ConversationsRepliesRequest
                    .builder()
                        .channel(request.getChannel())
                        .token(owner.getInstallerUserAccessToken())
                        .ts(request.getThreadTs())
                        .latest(request.getMessageTs())
                        .oldest(request.getMessageTs())
                        .inclusive(true)
                        .limit(1)
                    .build()
            );

            if (!messages.isOk()) {
                log.warn("Bad message request: {}", messages.getError());
                return;
            }

            if (messages.getMessages().size() != 1 && messages.getMessages().size() != 2) {
                log.warn("Could not find {} message", request.getMessageTs());
                return;
            }

            Message message = messages.getMessages().size() == 1 ? messages
                    .getMessages().get(0) : messages.getMessages().get(1);

            List<Attachment> attachments = message.getAttachments();
            if (attachments == null) attachments = new ArrayList<>();
            attachments = attachments.stream().filter(a -> !a.getFooter()
                    .contains(fileInfo.getFile().getId()+":")).collect(Collectors.toList());

            if (!request.getDefaultPermission().equals("read") || request.getSharedUsers().size() != 0)
                attachments.add(Attachment
                        .builder()
                        .color(color)
                        .text(String.format(attachmentText, fileInfo.getFile().getName()))
                        .footer(String.format(attachmentFooter, fileInfo.getFile().getId(), request.getDefaultPermission()))
                        .fallback(request.getSharedUsers().size() < 1 ?
                                "empty" : String.join(",", request.getSharedUsers()))
                        .build());

            app.client().chatUpdate(ChatUpdateRequest
                    .builder()
                    .ts(request.getMessageTs())
                    .channel(request.getChannel())
                    .token(owner.getInstallerUserAccessToken())
                    .text(message.getText() != null && !message.getText().isBlank() ? message.getText() : " ")
                    .attachments(attachments)
                    .build()
            );
        } catch (IOException | SlackApiException e) {
            log.warn(e.getMessage());
        }
    }

    public boolean hasEditPermissions(FilePermissionRequest request) {
        if (!request.validate()) {
            log.warn("Invalid FilePermissionGetRequest instance: {}", request);
            return false;
        }

        Installer owner = installationService
                .findInstaller(null, request.getTeam(), request.getUser());

        if (owner == null) {
            log.warn("Could not fetch owner installation");
            return false;
        }

        try {
            ConversationsRepliesResponse messages = app.client().conversationsReplies(ConversationsRepliesRequest
                    .builder()
                    .channel(request.getChannel())
                    .token(owner.getInstallerUserAccessToken())
                    .ts(request.getThreadTs())
                    .latest(request.getMessageTs())
                    .oldest(request.getMessageTs())
                    .inclusive(true)
                    .limit(1)
                    .build()
            );

            if (!messages.isOk()) {
                log.warn("Bad message request: {}", messages.getError());
                return false;
            }

            if (messages.getMessages().size() != 1 && messages.getMessages().size() != 2) {
                log.warn("Could not find {} message", request.getMessageTs());
                return false;
            }

            Message message = messages.getMessages().size() == 1 ? messages
                    .getMessages().get(0) : messages.getMessages().get(1);

            List<Attachment> attachments = message.getAttachments();
            if (attachments == null) attachments = new ArrayList<>();
            attachments = attachments.stream().filter(a -> a.getFooter()
                    .contains(request.getFile()+":")).collect(Collectors.toList());

            if (attachments.size() < 1) return false;

            boolean hasPermission = attachments.get(0).getFooter()
                    .contains(String.format("%s:edit", request.getFile())) || attachments
                    .get(0).getFallback().contains(request.getUser());

            if (hasPermission) return true;

            return false;
        } catch (IOException | SlackApiException e) {
            log.warn(e.getMessage());
            return false;
        }
    }

    public FilePermissionResponse getPermissionsAttachment(FilePermissionRequest request) {
        if (!request.validate()) {
            log.warn("Invalid FilePermissionGetRequest instance: {}", request);
            return FilePermissionResponse.builder().build();
        }

        Installer owner = installationService
                .findInstaller(null, request.getTeam(), request.getUser());

        FilePermissionResponse defaultPermissions = FilePermissionResponse.builder().build();
        if (owner == null) {
            log.warn("Could not fetch owner installation");
            return defaultPermissions;
        }

        try {
            ConversationsRepliesResponse messages = app.client().conversationsReplies(ConversationsRepliesRequest
                    .builder()
                    .channel(request.getChannel())
                    .token(owner.getInstallerUserAccessToken())
                    .ts(request.getThreadTs())
                    .latest(request.getMessageTs())
                    .oldest(request.getMessageTs())
                    .inclusive(true)
                    .limit(1)
                    .build()
            );

            if (!messages.isOk()) {
                log.warn("Bad message request: {}", messages.getError());
                return defaultPermissions;
            }

            if (messages.getMessages().size() != 1 && messages.getMessages().size() != 2) {
                log.warn("Could not find {} message", request.getMessageTs());
                return defaultPermissions;
            }

            Message message = messages.getMessages().size() == 1 ? messages
                    .getMessages().get(0) : messages.getMessages().get(1);

            List<Attachment> attachments = message.getAttachments();
            if (attachments == null) return FilePermissionResponse.builder().build();
            Attachment attachment = attachments.stream().filter(a -> a.getFooter()
                    .contains(request.getFile()+":")).findFirst().orElseGet(() -> new Attachment());

            if (attachment.getId() == null) return defaultPermissions;

            List<String> sharedUsers = attachment.getFallback()
                    .equals("empty") ? List.of() : Arrays.stream(attachment.getFallback().split(",")).toList();

            return attachment == null ? defaultPermissions : FilePermissionResponse
                    .builder()
                        .defaultPermission(attachment.getFooter().split(":")[1])
                        .sharedUsers(sharedUsers)
                    .build();
        } catch (IOException | SlackApiException e) {
            log.warn(e.getMessage());
            return defaultPermissions;
        }
    }
}
