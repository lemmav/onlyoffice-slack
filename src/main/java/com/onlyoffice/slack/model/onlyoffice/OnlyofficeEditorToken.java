package com.onlyoffice.slack.model.onlyoffice;

import core.model.OnlyofficeModelMutator;
import core.model.User;
import core.model.config.Config;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OnlyofficeEditorToken implements OnlyofficeModelMutator<Config> {
    private String owner; // message owner
    private String user; // current user id
    private String userName; // current user name
    private String workspace; // current workspace (team)
    private String file; // file id to generate docKey for
    private String fileName; // file name to display in the editor
    private String url; // document url (expected to be public)
    private String channel; // message channel
    private String threadTs; // parent message timestamp
    private String messageTs; // message timestamp
    private String otpCode; // scheduled message id
    private Integer otpAt; // scheduled message timestamp
    private String otpChannel; // scheduled message channel

    public void mutate(Config config) {
        config.getDocument().setTitle(fileName);
        config.getDocument().setUrl(url);
        config.getEditorConfig().setUser(
                User
                        .builder()
                        .id(user)
                        .name(userName)
                        .build()
        );
        config.getCustom().put("editorToken", this);
    }
}
