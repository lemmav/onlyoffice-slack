package com.onlyoffice.slack;

import com.onlyoffice.slack.configuration.registry.KeycloakConfiguration;
import com.onlyoffice.slack.configuration.slack.SlackConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
@EnableConfigurationProperties({
		KeycloakConfiguration.class,
		SlackConfiguration.class
})
public class SlackApplication {
	public static void main(String[] args) {
		SpringApplication.run(SlackApplication.class, args);
	}
}
