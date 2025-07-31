package com.onlyoffice.slack.handler.action.slack;

import com.onlyoffice.slack.configuration.slack.SlackConfigurationProperties;
import com.onlyoffice.slack.registry.SlackBlockActionHandlerRegistrar;
import com.slack.api.bolt.handler.builtin.BlockActionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackSuggestFeatureBlockActionHandler implements SlackBlockActionHandlerRegistrar {
  private final SlackConfigurationProperties slackConfigurationProperties;

  @Override
  public String getId() {
    return slackConfigurationProperties.getSuggestFeatureActionId();
  }

  @Override
  public BlockActionHandler getAction() {
    return (req, ctx) -> ctx.ack();
  }
}
