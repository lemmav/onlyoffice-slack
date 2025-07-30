package com.onlyoffice.slack.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class SlackInstallationController {
  @GetMapping(value = "/slack/oauth/completion")
  public String completion() {
    return "completion";
  }

  @GetMapping(value = "/slack/oauth/cancellation")
  public String cancellation() {
    return "cancellation";
  }
}
