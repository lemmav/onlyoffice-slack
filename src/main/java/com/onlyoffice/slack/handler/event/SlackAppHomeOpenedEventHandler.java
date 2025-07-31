package com.onlyoffice.slack.handler.event;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;
import static com.slack.api.model.view.Views.view;

import com.onlyoffice.slack.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.configuration.slack.SlackConfigurationProperties;
import com.onlyoffice.slack.configuration.slack.SlackMessageConfigurationProperties;
import com.onlyoffice.slack.exception.SettingsConfigurationException;
import com.onlyoffice.slack.service.data.TeamSettingsService;
import com.onlyoffice.slack.transfer.response.SettingsResponse;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.event.AppHomeOpenedEvent;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackAppHomeOpenedEventHandler implements BoltEventHandler<AppHomeOpenedEvent> {
  private final SlackMessageConfigurationProperties slackMessageConfigurationProperties;
  private final ServerConfigurationProperties serverConfigurationProperties;
  private final SlackConfigurationProperties slackConfigurationProperties;

  private final TeamSettingsService settingsService;
  private final MessageSource messageSource;

  private LayoutBlock buildDemoModeSection(final SettingsResponse settings) {
    var demoDeadline =
        settings
            .getDemoStartedDate()
            .plusDays(serverConfigurationProperties.getDemo().getDurationDays());
    var formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a");
    var message =
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageHomeDemoActive(),
            new Object[] {demoDeadline.format(formatter)},
            Locale.ENGLISH);
    return section(s -> s.text(plainText(message)));
  }

  private List<LayoutBlock> buildSettingsInputs(final SettingsResponse settings) {
    var inputs = new ArrayList<LayoutBlock>();
    inputs.add(
        header(
            h ->
                h.text(
                    plainText(
                        messageSource.getMessage(
                            slackMessageConfigurationProperties.getMessageHomeSettingsTitle(),
                            null,
                            Locale.ENGLISH)))));
    inputs.add(divider());
    inputs.add(
        input(
            i ->
                i.blockId("https_address")
                    .label(
                        plainText(
                            messageSource.getMessage(
                                slackMessageConfigurationProperties
                                    .getMessageHomeInputHttpsAddressLabel(),
                                null,
                                Locale.ENGLISH)))
                    .element(
                        plainTextInput(
                            pti ->
                                pti.actionId("https_address_input")
                                    .placeholder(
                                        plainText(
                                            messageSource.getMessage(
                                                slackMessageConfigurationProperties
                                                    .getMessageHomeInputHttpsAddressPlaceholder(),
                                                null,
                                                Locale.ENGLISH)))
                                    .initialValue(
                                        settings == null ? "" : settings.getAddress())))));
    inputs.add(
        input(
            i ->
                i.blockId("secret")
                    .label(
                        plainText(
                            messageSource.getMessage(
                                slackMessageConfigurationProperties
                                    .getMessageHomeInputSecretLabel(),
                                null,
                                Locale.ENGLISH)))
                    .element(
                        plainTextInput(
                            pti ->
                                pti.actionId("secret_input")
                                    .initialValue(settings == null ? "" : settings.getSecret())))));
    inputs.add(
        context(
            ctx ->
                ctx.elements(
                    List.of(
                        markdownText(
                            messageSource.getMessage(
                                slackMessageConfigurationProperties.getMessageHomeSecretHelp(),
                                null,
                                Locale.ENGLISH))))));
    inputs.add(
        input(
            i ->
                i.blockId("header")
                    .label(
                        plainText(
                            messageSource.getMessage(
                                slackMessageConfigurationProperties
                                    .getMessageHomeInputHeaderLabel(),
                                null,
                                Locale.ENGLISH)))
                    .element(
                        plainTextInput(
                            pti ->
                                pti.actionId("header_input")
                                    .initialValue(settings == null ? "" : settings.getHeader())))));
    inputs.add(
        context(
            ctx ->
                ctx.elements(
                    List.of(
                        markdownText(
                            messageSource.getMessage(
                                slackMessageConfigurationProperties.getMessageHomeHeaderHelp(),
                                null,
                                Locale.ENGLISH))))));
    inputs.add(
        input(
            i ->
                i.blockId("demo_enabled")
                    .label(
                        plainText(
                            messageSource.getMessage(
                                slackMessageConfigurationProperties
                                    .getMessageHomeInputDemoSettingsLabel(),
                                null,
                                Locale.ENGLISH)))
                    .element(
                        checkboxes(
                            c -> {
                              var enableDemoText =
                                  messageSource.getMessage(
                                      slackMessageConfigurationProperties
                                          .getMessageHomeCheckboxEnableDemo(),
                                      null,
                                      Locale.ENGLISH);
                              var checkboxBuilder =
                                  c.actionId("demo_enabled_checkbox")
                                      .options(
                                          List.of(
                                              option(
                                                  o ->
                                                      o.text(plainText(enableDemoText))
                                                          .value("demo_enabled"))));
                              if (settings != null && settings.isDemoEnabled()) {
                                checkboxBuilder.initialOptions(
                                    List.of(
                                        option(
                                            o ->
                                                o.text(plainText(enableDemoText))
                                                    .value("demo_enabled"))));
                              }
                              return checkboxBuilder;
                            }))));
    inputs.add(
        context(
            ctx ->
                ctx.elements(
                    List.of(
                        markdownText(
                            messageSource.getMessage(
                                slackMessageConfigurationProperties.getMessageHomeDemoHelp(),
                                null,
                                Locale.ENGLISH))))));

    return inputs;
  }

  private LayoutBlock buildSaveButtonActions() {
    return actions(
        actions ->
            actions.elements(
                asElements(
                    button(
                        b ->
                            b.text(
                                    plainText(
                                        messageSource.getMessage(
                                            slackMessageConfigurationProperties
                                                .getMessageHomeButtonSaveSettings(),
                                            null,
                                            Locale.ENGLISH)))
                                .actionId("submit_settings")
                                .style("primary")))));
  }

  private List<LayoutBlock> buildWelcomeBlocks() {
    var blocks = new ArrayList<LayoutBlock>();
    blocks.add(
        header(
            h ->
                h.text(
                    plainText(
                        messageSource.getMessage(
                            slackMessageConfigurationProperties.getMessageHomeWelcomeTitle(),
                            null,
                            Locale.ENGLISH)))));
    blocks.add(divider());
    blocks.add(
        section(
            s ->
                s.text(
                    markdownText(
                        messageSource.getMessage(
                            slackMessageConfigurationProperties.getMessageHomeWelcomeDescription(),
                            null,
                            Locale.ENGLISH)))));
    blocks.add(
        actions(
            actions ->
                actions.elements(
                    asElements(
                        button(
                            b ->
                                b.text(
                                        plainText(
                                            messageSource.getMessage(
                                                    slackMessageConfigurationProperties
                                                        .getMessageHomeReadMoreEmoji(),
                                                    null,
                                                    Locale.ENGLISH)
                                                + " "
                                                + messageSource.getMessage(
                                                    slackMessageConfigurationProperties
                                                        .getMessageHomeReadMore(),
                                                    null,
                                                    Locale.ENGLISH)))
                                    .actionId(slackConfigurationProperties.getReadMoreActionId())
                                    .url(slackConfigurationProperties.getWelcomeReadMoreUrl())),
                        button(
                            b ->
                                b.text(
                                        plainText(
                                            messageSource.getMessage(
                                                    slackMessageConfigurationProperties
                                                        .getMessageHomeSuggestFeatureEmoji(),
                                                    null,
                                                    Locale.ENGLISH)
                                                + " "
                                                + messageSource.getMessage(
                                                    slackMessageConfigurationProperties
                                                        .getMessageHomeSuggestFeature(),
                                                    null,
                                                    Locale.ENGLISH)))
                                    .actionId(
                                        slackConfigurationProperties.getSuggestFeatureActionId())
                                    .url(
                                        slackConfigurationProperties
                                            .getWelcomeSuggestFeatureUrl()))))));
    return blocks;
  }

  private List<LayoutBlock> buildCloudSection() {
    var blocks = new ArrayList<LayoutBlock>();
    blocks.add(
        header(
            h ->
                h.text(
                    plainText(
                        messageSource.getMessage(
                            slackMessageConfigurationProperties.getMessageHomeCloudTitle(),
                            null,
                            Locale.ENGLISH)))));
    blocks.add(divider());
    blocks.add(
        section(
            s ->
                s.text(
                        markdownText(
                            messageSource.getMessage(
                                slackMessageConfigurationProperties
                                    .getMessageHomeCloudDescription(),
                                null,
                                Locale.ENGLISH)))
                    .accessory(
                        button(
                            b ->
                                b.text(
                                        plainText(
                                            messageSource.getMessage(
                                                slackMessageConfigurationProperties
                                                    .getMessageHomeCloudButton(),
                                                null,
                                                Locale.ENGLISH)))
                                    .actionId("get_cloud")
                                    .url(slackConfigurationProperties.getGetCloudUrl())))));
    return blocks;
  }

  private List<LayoutBlock> buildHomeTabBlocks(final SettingsResponse settings) {
    var blocks = new ArrayList<>(buildWelcomeBlocks());

    if (settings != null && settings.isDemoEnabled() && settings.getDemoStartedDate() != null)
      blocks.add(buildDemoModeSection(settings));

    blocks.addAll(buildSettingsInputs(settings));
    blocks.add(buildSaveButtonActions());
    blocks.addAll(buildCloudSection());

    return blocks;
  }

  private void updateHomeTab(final AppHomeOpenedEvent event, final EventContext ctx)
      throws IOException, SlackApiException {
    var settings = SettingsResponse.builder().build();

    try {
      settings = settingsService.findSettings(ctx.getTeamId());
    } catch (SettingsConfigurationException e) {
      try {
        settings = settingsService.alwaysFindSettings(ctx.getTeamId());
      } catch (Exception fallbackException) {
        settings = SettingsResponse.builder().build();
      }
    }

    var blocks = buildHomeTabBlocks(settings);
    var view = view(v -> v.type("home").blocks(asBlocks(blocks.toArray(new LayoutBlock[0]))));
    var response = ctx.client().viewsPublish(r -> r.userId(event.getUser()).view(view));

    if (!response.isOk()) {
      var errorMessage =
          messageSource.getMessage(
              slackMessageConfigurationProperties.getMessageHomeErrorRenderView(),
              new Object[] {response.getError()},
              Locale.ENGLISH);
      log.error(errorMessage);
    }
  }

  @Override
  public Response apply(EventsApiPayload<AppHomeOpenedEvent> payload, EventContext ctx) {
    try {
      updateHomeTab(payload.getEvent(), ctx);
      return ctx.ack();
    } catch (IOException | SlackApiException e) {
      log.error("Error updating home tab", e);
      return ctx.ack();
    }
  }
}
