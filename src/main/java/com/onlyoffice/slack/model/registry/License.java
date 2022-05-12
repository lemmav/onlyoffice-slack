package com.onlyoffice.slack.model.registry;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class License {
    private String serverUrl;
    private String serverHeader;
    private String serverSecret;
}
