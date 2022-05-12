package com.onlyoffice.slack.model.registry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace implements Serializable {
    private String id;
    private String serverUrl;
    private String serverHeader;
    private String serverSecret;
}
