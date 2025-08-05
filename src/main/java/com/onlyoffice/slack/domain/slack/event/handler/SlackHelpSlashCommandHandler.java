package com.onlyoffice.slack.domain.slack.event.handler;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;

import com.onlyoffice.slack.domain.slack.event.registry.SlackSlashCommandHandlerRegistrar;
import com.onlyoffice.slack.shared.configuration.SlackConfigurationProperties;
import com.onlyoffice.slack.shared.configuration.message.MessageSourceSlackConfiguration;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class SlackHelpSlashCommandHandler implements SlackSlashCommandHandlerRegistrar {
  private final MessageSourceSlackConfiguration messageConfig;
  private final SlackConfigurationProperties slackProperties;

  private final MessageSource messageSource;

  @Override
  public String getSlash() {
    return "/help";
  }

  @Override
  public SlashCommandHandler getHandler() {
    return (req, ctx) -> {
      try {
        MDC.put("team_id", ctx.getTeamId());
        MDC.put("channel_id", req.getPayload().getChannelId());
        MDC.put("user_id", req.getPayload().getUserId());

        log.info("Sending ephemeral message via /help command");

        var userId = req.getPayload().getUserId();
        var blocks =
            List.of(
                section(
                    section ->
                        section.text(
                            markdownText(
                                messageSource.getMessage(
                                    messageConfig.getMessageHelpGreeting(),
                                    new Object[] {userId},
                                    Locale.getDefault())))),
                section(
                    section ->
                        section.text(
                            markdownText(
                                messageSource.getMessage(
                                    messageConfig.getMessageHelpInstructions(),
                                    null,
                                    Locale.getDefault())))),
                divider(),
                section(
                    section ->
                        section
                            .text(
                                markdownText(
                                    messageSource.getMessage(
                                        messageConfig.getMessageHelpLearnMore(),
                                        null,
                                        Locale.getDefault())))
                            .accessory(
                                button(
                                    button ->
                                        button
                                            .text(
                                                plainText(
                                                    messageSource.getMessage(
                                                        messageConfig
                                                            .getMessageHelpLearnMoreButton(),
                                                        null,
                                                        Locale.getDefault())))
                                            .style("primary")
                                            .actionId(slackProperties.getLearnMoreActionId())
                                            .url(slackProperties.getGetCloudUrl())))),
                section(
                    section ->
                        section
                            .text(
                                markdownText(
                                    messageSource.getMessage(
                                        messageConfig.getMessageHelpFeedback(),
                                        null,
                                        Locale.getDefault())))
                            .accessory(
                                button(
                                    button ->
                                        button
                                            .text(
                                                plainText(
                                                    messageSource.getMessage(
                                                        messageConfig
                                                            .getMessageHelpFeedbackButton(),
                                                        null,
                                                        Locale.getDefault())))
                                            .actionId(slackProperties.getShareFeedbackActionId())
                                            .url(slackProperties.getWelcomeSuggestFeatureUrl())))));

        return ctx.ack(res -> res.blocks(blocks).responseType("ephemeral"));
      } finally {
        MDC.clear();
      }
    };
  }
}
