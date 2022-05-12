package com.onlyoffice.slack.model.slack;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.function.Predicate;

@Builder
@Getter
@ToString
public class Caller implements SlackAppModel {
    private String id;
    private String wid;
    private String name;
    private String token;
    @Builder.Default
    private boolean isRoot = false;

    public boolean validate() {
        Predicate<String> isValidString = (s) -> s != null && !s.isBlank();
        if (!isValidString.test(id) || !isValidString.test(name) || !isValidString.test(wid))
            return false;
        return true;
    }
}
