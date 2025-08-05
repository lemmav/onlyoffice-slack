package com.onlyoffice.slack.domain.slack.event.action;

import com.onlyoffice.slack.domain.slack.event.registry.SlackBlockActionHandlerRegistrar;
import com.onlyoffice.slack.shared.configuration.SlackConfigurationProperties;
import com.slack.api.bolt.handler.builtin.BlockActionHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class SlackAutoAckBlockActionHandler implements SlackBlockActionHandlerRegistrar {
  private final SlackConfigurationProperties slackConfigurationProperties;

  @Override
  public List<String> getIds() {
    return List.of(
        slackConfigurationProperties.getGetCloudActionId(),
        slackConfigurationProperties.getReadMoreActionId(),
        slackConfigurationProperties.getSuggestFeatureActionId(),
        slackConfigurationProperties.getLearnMoreActionId(),
        slackConfigurationProperties.getShareFeedbackActionId());
  }

  @Override
  public BlockActionHandler getAction() {
    return (req, ctx) -> ctx.ack();
  }
}
