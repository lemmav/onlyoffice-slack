package com.onlyoffice.slack.controller.onlyoffice;

import com.google.common.collect.ImmutableMap;
import com.onlyoffice.slack.configuration.general.IntegrationConfiguration;
import com.onlyoffice.slack.model.onlyoffice.OnlyofficeCallbackToken;
import com.onlyoffice.slack.model.onlyoffice.OnlyofficeEditorToken;
import core.OnlyofficeIntegrationSDK;
import core.model.callback.Callback;
import core.model.config.Config;
import core.runner.implementation.CallbackRequest;
import core.runner.implementation.ConfigRequest;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OnlyofficeEditorController {
    private final IntegrationConfiguration integrationConfiguration;
    private final OnlyofficeIntegrationSDK integrationSDK;

    @RateLimiter(name = "callbackRateLimiter", fallbackMethod = "callbackFallback")
    @PostMapping(path = "/onlyoffice/callback")
    @ResponseBody
    public String callback(
            @RequestParam(value = "token") String token,
            @RequestHeader Map<String, String> headers,
            @RequestBody Callback callback
    ) {
        try {
            this.integrationSDK
                    .callbackRunner
                    .run(
                            CallbackRequest
                                    .builder()
                                        .callback(callback)
                                    .build()
                                    .addPreProcessor(
                                            "onlyoffice.preprocessor.default.callback",
                                            ImmutableMap.of(
                                                    "key", integrationConfiguration.getCallbackSecret(),
                                                    "token", token,
                                                    "mutator", OnlyofficeCallbackToken.builder().build()
                                            )
                                    )
                                    .addPreProcessor("onlyoffice.preprocessor.slack.callback.otp")
                                    .addPreProcessor(
                                            "onlyoffice.preprocessor.slack.callback.validator",
                                            ImmutableMap.copyOf(headers)
                                    )
                    );
            return "{\"error\": 0}";
        } catch (RuntimeException | IOException e) {
            return "{\"error\": 1}";
        }
    }

    public String callbackFallback(
            String token,
            Map<String, String> headers,
            Callback callback,
            RequestNotPermitted e
    ) {
        log.warn("process callback {} - {}", callback.getKey(), e.getMessage());
        return "{\"error\": 1}";
    }

    @RateLimiter(name = "editorRateLimiter", fallbackMethod = "editorFallback")
    @GetMapping(path = "/onlyoffice/editor")
    public String editor(
            @RequestParam(value = "token") String token,
            Model model
    ) {
        try {
            this.integrationSDK
                    .editorRunner
                    .run(
                            ConfigRequest
                                    .builder()
                                        .config(Config
                                                .builder()
                                                .build()
                                        )
                                    .build()
                                    .addPreProcessor("onlyoffice.preprocessor.default.editor", ImmutableMap.of(
                                            "key", integrationConfiguration.getEditorSecret(),
                                            "token", token,
                                            "mutator", OnlyofficeEditorToken.builder().build()
                                    ))
                                    .addPreProcessor("onlyoffice.preprocessor.slack.editor.docKey")
                                    .addPreProcessor("onlyoffice.preprocessor.slack.editor.api")
                                    .addPostProcessor("onlyoffice.postprocessor.slack.editor.model", ImmutableMap.of(
                                            "model", model
                                    ))
                    );
            return "editor";
        } catch (RuntimeException | IOException e) {
            return "error";
        }
    }

    public String editorFallback(
            String token,
            Model model,
            RequestNotPermitted e
    ) {
        log.warn("build editor: {}", e.getMessage());
        return "limit";
    }
}
