package com.onlyoffice.slack.service.slack;

import com.onlyoffice.slack.service.registry.SlackOnlyofficeRegistryInstallationService;
import com.slack.api.bolt.App;
import com.slack.api.bolt.model.Installer;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.search.SearchMessagesRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.search.SearchMessagesResponse;
import com.slack.api.model.Attachment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlyofficeDocKeyGeneratorService {
    private static final String textPattern = "Generated a new document key for %s";
    private static final String fallbackPattern = "ONLYOFFICE:%s";
    private static final String slackBot = "USLACKBOT";

    private final App app;
    private final SlackOnlyofficeRegistryInstallationService installationService;

    public String findGeneratedDocKey(String ownerToken, String fileId) throws IOException, SlackApiException {
        log.debug("Trying to find an already generated document key: {}", fileId);

        SearchMessagesResponse searchMessagesResponse = app.client().searchMessages(SearchMessagesRequest
                .builder()
                .token(ownerToken)
                .teamId(slackBot)
                .query(String.format(fallbackPattern, fileId))
                .sortDir("desc")
                .sort("timestamp")
                .count(1)
                .build());

        if (!searchMessagesResponse.isOk())
            throw new IOException(searchMessagesResponse.getError());

        if (searchMessagesResponse.getMessages().getMatches().size() == 1)
            return searchMessagesResponse.getMessages().getMatches()
                    .get(0).getAttachments().get(0).getFooter();

        log.debug("Could not find any document key: {}", fileId);
        return null;
    }

//    public void updateDocKey(String wid, String uid, String fileId) throws IOException, SlackApiException {
//        Installer installer = installationService.findInstaller(null, wid, uid);
//        if (installer == null) return;
//        publishDocKey(installer.getInstallerUserAccessToken(), fileId, UUID.randomUUID().toString());
//    }

    public void publishDocKey(String ownerToken, String fileId, String docKey) throws IOException, SlackApiException {
        log.debug("Publishing a new document key {} for {}", docKey, fileId);

        ChatPostMessageResponse postMessageResponse = app.client().chatPostMessage(ChatPostMessageRequest
                .builder()
                .channel(slackBot)
                .token(ownerToken)
                .asUser(true)
                .attachments(List.of(Attachment
                        .builder()
                        .color("#c2c4c4")
                        .text(String.format(textPattern, fileId))
                        .footer(docKey)
                        .fallback(String.format(fallbackPattern, fileId))
                        .build()))
                .build());
        if (!postMessageResponse.isOk())
            throw new IOException(postMessageResponse.getError());
    }
}
