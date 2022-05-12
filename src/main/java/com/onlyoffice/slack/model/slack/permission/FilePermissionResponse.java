package com.onlyoffice.slack.model.slack.permission;

import com.onlyoffice.slack.model.slack.SlackAppModel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@ToString
public class FilePermissionResponse implements SlackAppModel {
    @Builder.Default
    private List<String> sharedUsers = new ArrayList<>();
    @Builder.Default
    private String defaultPermission = "read";

    public boolean validate() {
        return true;
    }
}
