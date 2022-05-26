package com.onlyoffice.slack.service.slack;

import com.onlyoffice.slack.model.slack.Caller;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.users.UsersInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackLocaleService {
    private final App app;

    public Locale getLocale(Caller caller) {
        log.debug("trying to get locale for: {}", caller.getId());
        if (!caller.validate() || caller.getToken() == null || caller.getToken().isBlank()) {
            log.debug("invalid caller instance. Getting english locale");
            return Locale.ENGLISH;
        }
        try {
            UsersInfoResponse response = app.client().usersInfo(UsersInfoRequest
                    .builder()
                            .token(caller.getToken())
                            .user(caller.getId())
                            .includeLocale(true)
                    .build()
            );

            if (!response.isOk())
                throw new IOException(response.getError());

            return LocaleUtils.toLocale(response.getUser().getLocale().replace("-", "_"));
        } catch (IOException | SlackApiException e) {
            log.warn("got an exception: {}. Getting english locale", e.getMessage());
            return Locale.ENGLISH;
        }
    }
}
