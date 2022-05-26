package com.onlyoffice.slack.service.slack;

import com.slack.api.bolt.App;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlyofficeDocKeyGeneratorService {
    private static final String textPattern = "ONLYOFFICE SYSTEM INFO: \n*File: %s*";
    private static final String footerPattern = "Key: *%s*";
    private static final String slackBot = "USLACKBOT";

    private final App app;

    public String findGeneratedDocKey(String ownerToken, String fileId) throws IOException, SlackApiException {
        log.debug("trying to find an already generated document key: {}", fileId);

        SearchMessagesResponse searchMessagesResponse = app.client().searchMessages(SearchMessagesRequest
                .builder()
                .token(ownerToken)
                .teamId(slackBot)
                .query(String.format(textPattern, fileId))
                .sortDir("desc")
                .sort("timestamp")
                .count(1)
                .build());

        if (!searchMessagesResponse.isOk())
            throw new IOException(searchMessagesResponse.getError());

        if (searchMessagesResponse.getMessages().getMatches().size() == 1)
            return searchMessagesResponse.getMessages().getMatches()
                    .get(0).getAttachments().get(0).getFallback();

        log.debug("could not find any document key: {}", fileId);
        return null;
    }

//    public void updateDocKey(String wid, String uid, String fileId) throws IOException, SlackApiException {
//        Installer installer = installationService.findInstaller(null, wid, uid);
//        if (installer == null) return;
//        publishDocKey(installer.getInstallerUserAccessToken(), fileId, UUID.randomUUID().toString());
//    }

    public void publishDocKey(String ownerToken, String fileId, String docKey) throws IOException, SlackApiException {
        log.debug("publishing a new document key {} for {}", docKey, fileId);

        ChatPostMessageResponse postMessageResponse = app.client().chatPostMessage(ChatPostMessageRequest
                .builder()
                .channel(slackBot)
                .token(ownerToken)
                .asUser(true)
                .attachments(List.of(Attachment
                        .builder()
                        .color("#c2c4c4")
                        .text(String.format(textPattern, fileId))
                        .footer(String.format(footerPattern, docKey))
                        .fallback(docKey)
                        .build()))
                .build());
        if (!postMessageResponse.isOk())
            throw new IOException(postMessageResponse.getError());
    }
}
