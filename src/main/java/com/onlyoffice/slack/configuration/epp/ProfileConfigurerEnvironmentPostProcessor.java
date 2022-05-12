package com.onlyoffice.slack.configuration.epp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

public class ProfileConfigurerEnvironmentPostProcessor implements EnvironmentPostProcessor {
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        if (env.getActiveProfiles().length == 0)
            env.setActiveProfiles("prod", "production");
    }
}
