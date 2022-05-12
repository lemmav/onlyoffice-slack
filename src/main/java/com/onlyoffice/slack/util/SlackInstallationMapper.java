package com.onlyoffice.slack.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.slack.exception.UnableToPerformSlackOperationException;
import com.slack.api.bolt.model.Bot;
import com.slack.api.bolt.model.Installer;
import com.slack.api.bolt.model.builtin.DefaultBot;
import com.slack.api.bolt.model.builtin.DefaultInstaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Base64;

@Component
@Slf4j
public class SlackInstallationMapper {
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String toBase64(Installer installer) throws UnableToPerformSlackOperationException {
        try {
            log.debug("Converting installer to base64");
            return Base64.getEncoder().encodeToString(mapper.writeValueAsBytes(installer));
        } catch (Exception e) {
            throw new UnableToPerformSlackOperationException("Installer to base64 mapping error");
        }
    }

    public String toBase64(Bot bot) throws UnableToPerformSlackOperationException {
        try {
            log.debug("Converting bot to base64");
            return Base64.getEncoder().encodeToString(mapper.writeValueAsBytes(bot));
        } catch (Exception e) {
            throw new UnableToPerformSlackOperationException("Bot to base64 mapping error");
        }
    }

    public Installer toInstaller(String b64) throws UnableToPerformSlackOperationException {
        try {
            log.debug("Converting base64 to installer");
            return mapper.readValue(Base64.getDecoder().decode(b64), DefaultInstaller.class);
        } catch (Exception e) {
            throw new UnableToPerformSlackOperationException("Base64 to installer mapping error");
        }
    }

    public Installer toInstaller(DefaultBot bot) throws UnableToPerformSlackOperationException {
        try {
            log.debug("Converting bot to installer");
            return mapper.convertValue(bot, DefaultInstaller.class);
        } catch (Exception e) {
            throw new UnableToPerformSlackOperationException("Bot to installer mapping error");
        }
    }

    public Bot toBot(String b64) throws UnableToPerformSlackOperationException {
        try {
            log.debug("Converting base64 to bot");
            return mapper.readValue(Base64.getDecoder().decode(b64), DefaultBot.class);
        } catch (Exception e) {
            throw new UnableToPerformSlackOperationException("Base64 to bot mapping error");
        }
    }

    public Bot toBot(Installer installer) throws UnableToPerformSlackOperationException {
        try {
            log.debug("Converting installer to bot");
            DefaultInstaller defaultInstaller = (DefaultInstaller) installer;
            return mapper.convertValue(defaultInstaller, DefaultBot.class);
        } catch (Exception e) {
            throw new UnableToPerformSlackOperationException("Installer to bot mapping error");
        }
    }
}
