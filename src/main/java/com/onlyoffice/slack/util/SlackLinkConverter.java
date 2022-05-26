package com.onlyoffice.slack.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SlackLinkConverter {
    private final static String downloadPattern = "https://files.slack.com/files-pri/%s-%s/download/%s?pub_secret=%s";

    public String transformDownloadUrl(String downloadUrl, String fileName) throws RuntimeException {
        log.debug("transforming a download url: " + downloadUrl);
        int idsIndex = downloadUrl.lastIndexOf("/") + 1;
        String[] parts = downloadUrl.substring(idsIndex).split("-");
        if (parts.length != 3)
            throw new RuntimeException("could not find all the url parts required. Expected to get teamID, fileID, pubSecret");
        String url = String.format(downloadPattern, parts[0], parts[1], fileName, parts[2]);
        log.debug("successfully transformed a download url");
        return url;
    }

    public String removeSlash(String url) {
        log.debug("removing slash: {}", url);
        StringBuilder builder = new StringBuilder(url);
        if (builder.lastIndexOf("/") == builder.length() - 1)
            builder.deleteCharAt(url.length() - 1);
        return builder.toString();
    }
}
