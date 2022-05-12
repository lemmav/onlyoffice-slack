package com.onlyoffice.slack.controller.onlyoffice.processor.callback.pre;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class CallbackJwtSchema {
    private String secret;
    private String token;
}
