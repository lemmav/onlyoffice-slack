package com.onlyoffice.slack.handler.action.slack;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.button;
import static com.slack.api.model.view.Views.*;

import com.onlyoffice.slack.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.configuration.slack.SlackConfigurationProperties;
import com.onlyoffice.slack.configuration.slack.SlackMessageConfigurationProperties;
import com.slack.api.bolt.context.builtin.MessageShortcutContext;
import com.slack.api.bolt.handler.builtin.MessageShortcutHandler;
import com.slack.api.bolt.request.builtin.MessageShortcutRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.views.ViewsOpenRequest;
import com.slack.api.model.File;
import com.slack.api.model.Message;
import com.slack.api.model.block.LayoutBlock;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackOpenFileManagerMessageShortcutRequestHandler implements MessageShortcutHandler {
  private final SlackMessageConfigurationProperties slackMessageConfigurationProperties;
  private final ServerConfigurationProperties serverConfigurationProperties;
  private final SlackConfigurationProperties slackConfigurationProperties;

  private final SlackPrivateMetadataBuilder slackPrivateMetadataBuilder;
  private final SlackFileActionBuilder slackFileActionBuilder;
  private final MessageSource messageSource;

  private boolean hasFiles(final Message message) {
    return message != null && message.getFiles() != null && !message.getFiles().isEmpty();
  }

  private String buildFileInfo(final File file) {
    return messageSource.getMessage(
        slackMessageConfigurationProperties.getMessageManagerModalFileInfo(),
        new Object[] {
          file.getName(),
          file.getPrettyType(),
          file.getSize(),
          file.isPublic()
              ? messageSource.getMessage(
                  slackMessageConfigurationProperties.getMessageManagerModalFileStatusPublic(),
                  null,
                  Locale.ENGLISH)
              : messageSource.getMessage(
                  slackMessageConfigurationProperties.getMessageManagerModalFileStatusPrivate(),
                  null,
                  Locale.ENGLISH),
          file.getId()
        },
        Locale.ENGLISH);
  }

  private String buildEditorUrl(final UUID sessionId) {
    return slackConfigurationProperties
        .getEditorPathPattern()
        .formatted(serverConfigurationProperties.getBaseAddress(), sessionId);
  }

  private LayoutBlock buildFileActionsBlock(final File file) {
    var sessionId = UUID.randomUUID();
    var buttonElement =
        button(
            b ->
                b.text(
                        plainText(
                            messageSource.getMessage(
                                slackMessageConfigurationProperties
                                    .getMessageManagerModalOpenButton(),
                                null,
                                Locale.ENGLISH)))
                    .value(slackFileActionBuilder.build(sessionId.toString(), file.getId()))
                    .url(buildEditorUrl(sessionId))
                    .actionId(slackConfigurationProperties.getOpenFileActionId())
                    .style("primary"));
    return actions(actions -> actions.elements(List.of(buttonElement)));
  }

  private void addFileBlock(final List<LayoutBlock> blocks, final File file) {
    blocks.add(divider());
    blocks.add(section(s -> s.text(markdownText(buildFileInfo(file)))));
    blocks.add(buildFileActionsBlock(file));
  }

  private List<LayoutBlock> buildFileBlocks(final Message message) {
    var blocks = new ArrayList<LayoutBlock>();
    blocks.add(
        header(
            h ->
                h.text(
                    plainText(
                        messageSource.getMessage(
                            slackMessageConfigurationProperties.getMessageManagerModalHeader(),
                            null,
                            Locale.ENGLISH)))));

    if (hasFiles(message)) {
      log.info("Found {} files in message", message.getFiles().size());
      message.getFiles().forEach(file -> addFileBlock(blocks, file));
    } else {
      log.info("No files found in message");
      blocks.add(
          section(
              s ->
                  s.text(
                      plainText(
                          messageSource.getMessage(
                              slackMessageConfigurationProperties
                                  .getMessageManagerModalNoFilesFound(),
                              null,
                              Locale.ENGLISH)))));
    }

    return blocks;
  }

  private void openFilesModal(
      final MessageShortcutContext ctx,
      final MessageShortcutRequest request,
      final List<LayoutBlock> blocks)
      throws IOException, SlackApiException {
    var message = request.getPayload().getMessage();
    ctx.client()
        .viewsOpen(
            ViewsOpenRequest.builder()
                .triggerId(request.getPayload().getTriggerId())
                .view(
                    view(
                        view ->
                            view.type("modal")
                                .notifyOnClose(false)
                                .title(
                                    viewTitle(
                                        title ->
                                            title
                                                .type("plain_text")
                                                .text(
                                                    messageSource.getMessage(
                                                        slackMessageConfigurationProperties
                                                            .getMessageManagerModalTitle(),
                                                        null,
                                                        Locale.ENGLISH))))
                                .close(
                                    viewClose(
                                        close ->
                                            close
                                                .type("plain_text")
                                                .text(
                                                    messageSource.getMessage(
                                                        slackMessageConfigurationProperties
                                                            .getMessageManagerModalCloseButton(),
                                                        null,
                                                        Locale.ENGLISH))))
                                .blocks(blocks)
                                .privateMetadata(
                                    slackPrivateMetadataBuilder.build(
                                        ctx.getChannelId(), message.getTs()))))
                .build());
  }

  @Override
  public Response apply(MessageShortcutRequest request, MessageShortcutContext ctx)
      throws IOException, SlackApiException {
    openFilesModal(ctx, request, buildFileBlocks(request.getPayload().getMessage()));
    return ctx.ack();
  }
}
