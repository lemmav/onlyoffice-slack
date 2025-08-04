package com.onlyoffice.slack.shared.configuration.message;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MessageSourceConfiguration implements WebMvcConfigurer {

  @Bean
  @Primary
  public MessageSource messageSource() {
    var messageSource = new ResourceBundleMessageSource();
    messageSource.setBasenames("lang/messages");
    messageSource.setDefaultLocale(Locale.ENGLISH);
    messageSource.setDefaultEncoding("UTF-8");
    return messageSource;
  }
}
