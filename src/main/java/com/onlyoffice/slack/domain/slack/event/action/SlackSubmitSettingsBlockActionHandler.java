package com.onlyoffice.slack.domain.slack.event.action;

import com.onlyoffice.slack.domain.document.editor.core.DocumentSettingsValidationService;
import com.onlyoffice.slack.domain.slack.event.registry.SlackBlockActionHandlerRegistrar;
import com.onlyoffice.slack.domain.slack.settings.SettingsService;
import com.onlyoffice.slack.shared.configuration.SlackConfigurationProperties;
import com.onlyoffice.slack.shared.transfer.request.SubmitSettingsRequest;
import com.onlyoffice.slack.shared.utils.HttpUtils;
import com.slack.api.bolt.handler.builtin.BlockActionHandler;
import com.slack.api.model.view.ViewState;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class SlackSubmitSettingsBlockActionHandler implements SlackBlockActionHandlerRegistrar {
  private final SlackConfigurationProperties slackConfigurationProperties;

  private final DocumentSettingsValidationService documentSettingsValidationService;
  private final SettingsService settingsService;

  private final HttpUtils httpUtils;

  private String extractValue(
      final Map<String, Map<String, ViewState.Value>> values,
      final String blockId,
      final String actionId) {
    try {
      return Optional.ofNullable(values.get(blockId))
          .map(blockValues -> blockValues.get(actionId))
          .map(ViewState.Value::getValue)
          .map(String::trim)
          .filter(value -> !value.isEmpty())
          .orElse("");
    } catch (Exception e) {
      log.warn(
          "Error extracting value for block '{}' action '{}': {}",
          blockId,
          actionId,
          e.getMessage());
      return null;
    }
  }

  private boolean extractDemoEnabled(final Map<String, Map<String, ViewState.Value>> values) {
    return Optional.ofNullable(values.get("demo_enabled"))
        .map(block -> block.get("demo_enabled_checkbox"))
        .map(ViewState.Value::getSelectedOptions)
        .map(options -> !options.isEmpty())
        .orElse(false);
  }

  private SubmitSettingsRequest buildSettingsRequest(
      final String httpsAddress, final String secret, final String header, boolean demoEnabled) {
    return SubmitSettingsRequest.builder()
        .address(httpsAddress)
        .secret(secret)
        .header(header)
        .demoEnabled(demoEnabled)
        .build();
  }

  private boolean shouldValidateConnection(final SubmitSettingsRequest request) {
    return !request.isDemoEnabled() || request.isCredentialsComplete();
  }

  @Override
  public List<String> getIds() {
    return List.of(slackConfigurationProperties.getSubmitSettingsActionId());
  }

  @Override
  public BlockActionHandler getAction() {
    return (req, ctx) -> {
      var values = req.getPayload().getView().getState().getValues();
      var httpsAddress = extractValue(values, "https_address", "https_address_input");
      var secret = extractValue(values, "secret", "secret_input");
      var header = extractValue(values, "header", "header_input");
      var demoEnabled = extractDemoEnabled(values);

      try {
        var request =
            buildSettingsRequest(
                httpUtils.normalizeAddress(httpsAddress), secret, header, demoEnabled);

        if (!request.isValidConfiguration()) {
          log.warn(
              "Invalid configuration provided: address={}, secret={}, header={}, demoEnabled={}",
              httpsAddress,
              secret != null ? "***" : null,
              header,
              demoEnabled);
          return ctx.ack();
        }

        if (shouldValidateConnection(request))
          documentSettingsValidationService.validateConnection(request);

        settingsService.saveSettings(ctx, request);
      } catch (Exception e) {
        log.error("Error updating settings", e);
        return ctx.ack();
      }

      return ctx.ack();
    };
  }
}
