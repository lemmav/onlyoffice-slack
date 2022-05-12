package com.onlyoffice.slack.model.slack;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class ScheduledOtp implements SlackAppModel {
    private String code;
    private String channel;
    private Integer at;
    public boolean validate() {
        if (code == null || code.isBlank()) return false;
        if (channel == null || channel.isBlank()) return false;
        if (at <= 0) return false;
        return true;
    }
}
