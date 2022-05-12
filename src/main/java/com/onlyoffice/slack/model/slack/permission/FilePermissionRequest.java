package com.onlyoffice.slack.model.slack.permission;

import com.onlyoffice.slack.model.slack.SlackAppModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.function.Predicate;

@Builder
@AllArgsConstructor
@Getter
@ToString
public class FilePermissionRequest implements SlackAppModel {
    private String user;
    private String channel;
    private String team;
    private String threadTs;
    private String messageTs;
    private String file;

    private final static Predicate<String> isValidString = s -> s != null && !s.isBlank();

    public boolean validate() {
        if (!isValidString.test(user)) return false;
        if (!isValidString.test(channel) || !isValidString.test(team)) return false;
        if (!isValidString.test(messageTs) || !isValidString.test(threadTs)) return false;
        if (!isValidString.test(file)) return false;

        return true;
    }
}
