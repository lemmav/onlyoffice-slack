package com.onlyoffice.slack.configuration.slack;

import com.slack.api.bolt.service.builtin.oauth.view.OAuthInstallPageRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Configuration
@RequiredArgsConstructor
public class SlackCustomInstallationRenderer implements OAuthInstallPageRenderer {
  private final SpringTemplateEngine engine;

  public String render(final String installationUrl) {
    var ctx = new Context();
    ctx.setVariable("installationUrl", installationUrl);
    return engine.process("install", ctx);
  }
}
