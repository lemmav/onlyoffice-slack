package com.onlyoffice.slack.domain.slack.event.action;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.button;
import static com.slack.api.model.view.Views.*;

import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.shared.configuration.SlackConfigurationProperties;
import com.onlyoffice.slack.shared.configuration.message.MessageSourceSlackConfiguration;
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
class SlackOpenFileManagerMessageShortcutRequestHandler implements MessageShortcutHandler {
  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

  private final MessageSourceSlackConfiguration messageSourceSlackConfiguration;
  private final ServerConfigurationProperties serverConfigurationProperties;
  private final SlackConfigurationProperties slackConfigurationProperties;

  private final SlackPrivateMetadataBuilder slackPrivateMetadataBuilder;
  private final SlackFileActionBuilder slackFileActionBuilder;
  private final MessageSource messageSource;

  private String buildEditorUrl(final String sessionId) {
    return slackConfigurationProperties
        .getEditorPathPattern()
        .formatted(serverConfigurationProperties.getBaseAddress(), sessionId);
  }

  private boolean hasFiles(final Message message) {
    return message != null && message.getFiles() != null && !message.getFiles().isEmpty();
  }

  private void addFileBlock(final String userId, final List<LayoutBlock> blocks, final File file) {
    blocks.add(divider());
    if (file.getSize() > MAX_FILE_SIZE) {
      blocks.add(
          section(
              s ->
                  s.text(
                      plainText(
                          file.getName()
                              + "  —  "
                              + (file.getSize() / 1024)
                              + " KB (too large)"))));
      return;
    }

    var sessionId =
        slackFileActionBuilder.build(userId, UUID.randomUUID().toString(), file.getId());
    var openButton =
        button(
            b ->
                b.text(
                        plainText(
                            messageSource.getMessage(
                                messageSourceSlackConfiguration.getMessageManagerModalOpenButton(),
                                null,
                                Locale.ENGLISH)))
                    .value(sessionId)
                    .url(buildEditorUrl(sessionId))
                    .actionId(slackConfigurationProperties.getOpenFileActionId())
                    .style("primary"));
    blocks.add(
        section(
            s ->
                s.text(plainText(file.getName() + "  —  " + (file.getSize() / 1024) + " KB"))
                    .accessory(openButton)));
  }

  private List<LayoutBlock> buildFileBlocks(final String userId, final Message message) {
    var blocks = new ArrayList<LayoutBlock>();
    blocks.add(
        header(
            h ->
                h.text(
                    plainText(
                        messageSource.getMessage(
                            messageSourceSlackConfiguration.getMessageManagerModalHeader(),
                            null,
                            Locale.ENGLISH)))));

    if (hasFiles(message)) {
      log.info("Found {} files in message", message.getFiles().size());
      message.getFiles().forEach(file -> addFileBlock(userId, blocks, file));
    } else {
      log.info("No files found in message");
      blocks.add(
          section(
              s ->
                  s.text(
                      plainText(
                          messageSource.getMessage(
                              messageSourceSlackConfiguration.getMessageManagerModalNoFilesFound(),
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
                                                        messageSourceSlackConfiguration
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
                                                        messageSourceSlackConfiguration
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
    openFilesModal(
        ctx, request, buildFileBlocks(ctx.getRequestUserId(), request.getPayload().getMessage()));
    return ctx.ack();
  }
}
