package com.onlyoffice.slack.configuration.slack;

import com.slack.api.bolt.service.builtin.oauth.view.OAuthInstallPageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SlackCustomInstallationRenderer implements OAuthInstallPageRenderer {
    private final SpringTemplateEngine templateEngine;

    public String render(String installationUrl) {
        Context myContext = new Context();
        myContext.setVariable("installationUrl", installationUrl);
        return templateEngine.process("install", myContext);
    }
}
