package com.onlyoffice.slack.domain.document.editor;

import com.hazelcast.map.IMap;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.slack.domain.document.editor.core.DocumentConfigManagerService;
import com.onlyoffice.slack.domain.slack.installation.RotatingInstallationService;
import com.onlyoffice.slack.domain.slack.settings.TeamSettingsService;
import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.shared.configuration.message.MessageSourceSlackConfiguration;
import com.onlyoffice.slack.shared.transfer.cache.EditorSession;
import com.onlyoffice.slack.shared.transfer.command.BuildConfigCommand;
import com.onlyoffice.slack.shared.transfer.response.SettingsResponse;
import com.slack.api.bolt.App;
import com.slack.api.bolt.model.Installer;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.files.FilesInfoResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DocumentEditorController {
  private static final ExecutorService VIRTUAL_THREAD_EXECUTOR =
      Executors.newVirtualThreadPerTaskExecutor();

  private final ServerConfigurationProperties serverConfigurationProperties;
  private final MessageSourceSlackConfiguration messageSourceSlackConfiguration;

  private final App app;
  private final MessageSource messageSource;
  private final TeamSettingsService settingsService;
  private final IMap<String, EditorSession> sessions;
  private final RotatingInstallationService rotatingInstallationService;
  private final DocumentConfigManagerService documentConfigManagerService;

  @GetMapping(path = "/editor")
  public String editor(@RequestParam("session") final String sessionId, final Model model) {
    model.addAttribute("sid", sessionId);
    model.addAttribute(
        "title",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageLoadingTitle(), null, Locale.ENGLISH));
    model.addAttribute(
        "description",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageLoadingDescription(), null, Locale.ENGLISH));
    model.addAttribute(
        "error",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageLoadingError(), null, Locale.ENGLISH));
    model.addAttribute(
        "retry",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageLoadingRetry(), null, Locale.ENGLISH));
    model.addAttribute(
        "cancel",
        messageSource.getMessage(
            messageSourceSlackConfiguration.getMessageLoadingCancel(), null, Locale.ENGLISH));

    return "document/loading";
  }

  private Optional<EditorSession> retrieveAndRemoveSession(final String sessionId) {
    return Optional.ofNullable(sessions.remove(sessionId));
  }

  private Optional<Installer> findInstaller(final EditorSession session) {
    return Optional.ofNullable(
        rotatingInstallationService.findInstallerWithRotation(
            null, session.getTeamId(), session.getUserId()));
  }

  private void applyDemoSettings(final SettingsResponse settings) {
    settings.setAddress(serverConfigurationProperties.getDemo().getAddress());
    settings.setHeader(serverConfigurationProperties.getDemo().getHeader());
    settings.setSecret(serverConfigurationProperties.getDemo().getSecret());
  }

  private SettingsResponse getEffectiveSettings(final String teamId) {
    var settings = settingsService.findSettings(teamId);
    if (settings.isDemoEnabled()) applyDemoSettings(settings);

    return settings;
  }

  private Optional<FilesInfoResponse> fetchFileInfo(final String token, final String fileId) {
    try {
      var fileInfo = app.getSlack().methods(token).filesInfo(r -> r.file(fileId));
      if (!fileInfo.isOk()) {
        log.error("Failed to get file info from Slack API. Error: {}", fileInfo.getError());
        return Optional.empty();
      }

      return Optional.of(fileInfo);
    } catch (Exception e) {
      log.error("Slack API error while fetching file info: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private Optional<UsersInfoResponse> fetchUserInfo(final String token, final String userId) {
    try {
      var userInfo =
          app.getClient().usersInfo(UsersInfoRequest.builder().user(userId).token(token).build());
      if (!userInfo.isOk()) {
        log.error("Failed to get user info from Slack API. Error: {}", userInfo.getError());
        return Optional.empty();
      }
      return Optional.of(userInfo);
    } catch (Exception e) {
      log.error("Slack API error while fetching user info: {}", e.getMessage());
      return Optional.empty();
    }
  }

  @GetMapping(path = "/editor/content")
  public String editorContent(@RequestParam("session") final String sessionId, final Model model) {
    var storedSession = retrieveAndRemoveSession(sessionId);
    if (storedSession.isEmpty()) {
      model.addAttribute(
          "title",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSessionTitle(), null, Locale.ENGLISH));
      model.addAttribute(
          "text",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSessionText(), null, Locale.ENGLISH));
      model.addAttribute(
          "button",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSessionButton(), null, Locale.ENGLISH));

      return "errors/bad_session";
    }

    var session = storedSession.get();

    var maybeUser = findInstaller(session);
    if (maybeUser.isEmpty()) {
      model.addAttribute(
          "title",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorAvailableTitle(), null, Locale.ENGLISH));
      model.addAttribute(
          "text",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorAvailableText(), null, Locale.ENGLISH));
      model.addAttribute(
          "button",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorAvailableButton(), null, Locale.ENGLISH));

      return "errors/not_available";
    }

    var user = maybeUser.get();
    var settings = getEffectiveSettings(session.getTeamId());

    try {
      var fileInfoFuture =
          CompletableFuture.supplyAsync(
              () -> fetchFileInfo(user.getInstallerUserAccessToken(), session.getFileId()),
              VIRTUAL_THREAD_EXECUTOR);
      var userInfoFuture =
          CompletableFuture.supplyAsync(
              () -> fetchUserInfo(user.getInstallerUserAccessToken(), session.getUserId()),
              VIRTUAL_THREAD_EXECUTOR);

      var fileInfoOpt = fileInfoFuture.get();
      var userInfoOpt = userInfoFuture.get();

      if (fileInfoOpt.isEmpty() || userInfoOpt.isEmpty()) {
        model.addAttribute(
            "title",
            messageSource.getMessage(
                messageSourceSlackConfiguration.getErrorSlackApiTitle(), null, Locale.ENGLISH));
        model.addAttribute(
            "text",
            messageSource.getMessage(
                messageSourceSlackConfiguration.getErrorSlackApiText(), null, Locale.ENGLISH));
        model.addAttribute(
            "button",
            messageSource.getMessage(
                messageSourceSlackConfiguration.getErrorSlackApiButton(), null, Locale.ENGLISH));

        return "errors/bad_slack";
      }

      var config =
          documentConfigManagerService.createConfig(
              BuildConfigCommand.builder()
                  .channelId(session.getChannelId())
                  .messageTs(session.getMessageTs())
                  .signingSecret(settings.getSecret())
                  .user(userInfoOpt.get().getUser())
                  .file(fileInfoOpt.get().getFile())
                  .mode(Mode.EDIT)
                  .type(Type.DESKTOP)
                  .build());

      model.addAttribute("config", config);
      model.addAttribute(
          "apiUrl", String.format("%s/web-apps/apps/api/documents/api.js", settings.getAddress()));

      return "document/editor";
    } catch (InterruptedException | ExecutionException e) {
      model.addAttribute(
          "title",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSlackApiTitle(), null, Locale.ENGLISH));
      model.addAttribute(
          "text",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSlackApiText(), null, Locale.ENGLISH));
      model.addAttribute(
          "button",
          messageSource.getMessage(
              messageSourceSlackConfiguration.getErrorSlackApiButton(), null, Locale.ENGLISH));

      return "errors/bad_slack";
    }
  }
}
