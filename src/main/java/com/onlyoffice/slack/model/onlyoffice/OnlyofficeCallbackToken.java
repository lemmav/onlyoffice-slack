package com.onlyoffice.slack.model.onlyoffice;

import core.model.OnlyofficeModelMutator;
import core.model.callback.Callback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OnlyofficeCallbackToken implements OnlyofficeModelMutator<Callback> {
    private String owner; // message owner
    private String user; // current user id
    private String workspace; // current workspace (team)
    private String file; // file id to generate docKey for
    private String fileName; // file name to display in the editor
    private String channel; // message channel
    private String ts; // parent message timestamp

    private String otpCode;
    private Integer otpAt;
    private String otpChannel;

    public void mutate(Callback callback) {
        callback.getCustom().put("callbackToken", this);
    }
}
