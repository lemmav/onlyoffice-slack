package com.onlyoffice.slack.handler.action.slack;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.configuration.slack.SlackConfigurationProperties;
import com.onlyoffice.slack.registry.SlackBlockActionHandlerRegistrar;
import com.onlyoffice.slack.transfer.cache.EditorSession;
import com.onlyoffice.slack.util.SafeOptional;
import com.slack.api.app_backend.interactive_components.payload.BlockActionPayload;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.handler.builtin.BlockActionHandler;
import com.slack.api.methods.request.files.FilesInfoRequest;
import com.slack.api.methods.response.files.FilesInfoResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackOpenFileBlockActionHandler implements SlackBlockActionHandlerRegistrar {
  private final SlackConfigurationProperties slackConfigurationProperties;

  private final SlackSplittingFileActionExtractor slackSplittingFileActionExtractor;
  private final SlackPrivateMetadataExtractor slackPrivateMetadataExtractor;
  private final IMap<String, EditorSession> sessions;

  private Optional<FilesInfoResponse> findFile(final ActionContext ctx, final String fileId) {
    log.info("Retrieving file info for file");
    return SafeOptional.of(
        () ->
            ctx.getSlack()
                .methods()
                .filesInfo(
                    FilesInfoRequest.builder()
                        .file(fileId)
                        .token(ctx.getRequestUserToken())
                        .build()));
  }

  private void handleAction(
      final ActionContext ctx,
      final BlockActionPayload.Action action,
      final BlockActionPayload.User user,
      final String channelId,
      final String messageTs) {
    var sessionId =
        slackSplittingFileActionExtractor.extract(
            action.getValue(), SlackFileActionExtractor.Type.SESSION);
    var fileId =
        slackSplittingFileActionExtractor.extract(
            action.getValue(), SlackFileActionExtractor.Type.FILE);

    if (sessionId == null || sessionId.isBlank()) {
      log.warn("Invalid action value format: {}. Session id is missing", action.getValue());
      return;
    }

    if (fileId == null || fileId.isBlank()) {
      log.warn("Invalid action value format: {}. File id is missing", action.getValue());
      return;
    }

    try {
      MDC.put("session_id", sessionId);
      MDC.put("file_id", fileId);
      log.info("Processing open file action for session: {} and file: {}", sessionId, fileId);

      findFile(ctx, fileId)
          .ifPresentOrElse(
              (fileInfo) -> {
                log.info("Creating editor session for file: {}", fileInfo.getFile().getName());
                sessions.put(
                    sessionId,
                    EditorSession.builder()
                        .teamId(ctx.getTeamId())
                        .userId(user.getId())
                        .userName(user.getName())
                        .fileId(fileInfo.getFile().getId())
                        .fileName(fileInfo.getFile().getName())
                        .channelId(channelId)
                        .messageTs(messageTs)
                        .build());
                log.info("Editor session created successfully");
              },
              () -> log.warn("File not found or inaccessible for file ID: {}", fileId));
    } finally {
      MDC.clear();
    }
  }

  @Override
  public String getId() {
    return slackConfigurationProperties.getOpenFileActionId();
  }

  @Override
  public BlockActionHandler getAction() {
    return (req, ctx) -> {
      MDC.put("team_id", ctx.getTeamId());
      MDC.put("handler_id", getId());

      try {
        log.info("Processing block action request for current handler");

        var user = req.getPayload().getUser();
        var view = req.getPayload().getView();
        var privateMetadata = view != null ? view.getPrivateMetadata() : null;

        var channelId = "";
        var messageTs = "";

        if (privateMetadata != null) {
          channelId =
              slackPrivateMetadataExtractor.extract(
                  privateMetadata, SlackPrivateMetadataExtractor.Type.CHANNEL);
          messageTs =
              slackPrivateMetadataExtractor.extract(
                  privateMetadata, SlackPrivateMetadataExtractor.Type.MESSAGETS);
        }

        var maybeAction = req.getPayload().getActions().stream().findFirst();

        if (maybeAction.isPresent()) {
          MDC.put("channel_id", channelId);
          MDC.put("user_id", user.getId());
          MDC.put("message_ts", messageTs);
          log.info("Found action in payload, delegating to handleAction");

          handleAction(ctx, maybeAction.get(), user, channelId, messageTs);
        } else {
          log.warn("No action found in payload");
        }

        log.info("Block action request processed successfully");
        return ctx.ack();
      } finally {
        MDC.clear();
      }
    };
  }
}
