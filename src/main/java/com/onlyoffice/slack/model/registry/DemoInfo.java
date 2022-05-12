package com.onlyoffice.slack.model.registry;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DemoInfo {
    private boolean hasDemo;
    private boolean isExpired;
}
