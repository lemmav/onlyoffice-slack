package com.onlyoffice.slack.controller.slack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SlackInstallationResultController {
    @GetMapping(value = "/slack/oauth/completion")
    public String completion() {
        log.debug("successfully installed ONLYOFFICE integration");
        return "completion";
    }

    @GetMapping(value = "/slack/oauth/cancellation")
    public String cancellation() {
        log.debug("failed to install ONLYOFFICE integration");
        return "cancellation";
    }
}
