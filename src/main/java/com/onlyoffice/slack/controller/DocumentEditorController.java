package com.onlyoffice.slack.controller;

import com.hazelcast.map.IMap;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.slack.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.configuration.slack.SlackMessageConfigurationProperties;
import com.onlyoffice.slack.service.data.RotatingInstallationService;
import com.onlyoffice.slack.service.data.TeamSettingsService;
import com.onlyoffice.slack.service.document.core.ConfigManagerService;
import com.onlyoffice.slack.transfer.cache.EditorSession;
import com.onlyoffice.slack.transfer.command.BuildConfigCommand;
import com.onlyoffice.slack.transfer.response.SettingsResponse;
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

  private final RotatingInstallationService rotatingInstallationService;
  private final ConfigManagerService configManagerService;
  private final IMap<String, EditorSession> sessions;
  private final TeamSettingsService settingsService;
  private final App app;

  private final SlackMessageConfigurationProperties slackMessageConfigurationProperties;
  private final MessageSource messageSource;

  @GetMapping(path = "/editor")
  public String editor(@RequestParam("session") final String sessionId, final Model model) {
    model.addAttribute("sessionId", sessionId);
    model.addAttribute(
        "loadingTitle",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageLoadingTitle(), null, Locale.ENGLISH));
    model.addAttribute(
        "loadingDescription",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageLoadingDescription(),
            null,
            Locale.ENGLISH));
    model.addAttribute(
        "loadingError",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageLoadingError(), null, Locale.ENGLISH));
    model.addAttribute(
        "loadingRetry",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageLoadingRetry(), null, Locale.ENGLISH));
    model.addAttribute(
        "loadingCancel",
        messageSource.getMessage(
            slackMessageConfigurationProperties.getMessageLoadingCancel(), null, Locale.ENGLISH));
    return "loading";
  }

  @GetMapping(path = "/editor/content")
  public String editorContent(@RequestParam("session") final String sessionId, final Model model) {
    var storedSession = retrieveAndRemoveSession(sessionId);
    if (storedSession.isEmpty()) {
      model.addAttribute("errorTitle", "Session Expired");
      model.addAttribute(
          "errorMessage", "Your session has expired. Please open the file again from Slack.");
      model.addAttribute("buttonText", "Back to Slack");
      model.addAttribute("statusCode", 403);
      return "error";
    }

    var session = storedSession.get();

    var maybeUser = findInstaller(session);
    if (maybeUser.isEmpty()) {
      model.addAttribute("errorTitle", "App Not Installed");
      model.addAttribute(
          "errorMessage",
          "The ONLYOFFICE app needs to be installed or reinstalled for your workspace. Please contact your Slack administrator.");
      model.addAttribute("buttonText", "Back to Slack");
      model.addAttribute("statusCode", 403);
      return "error";
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
        model.addAttribute("errorTitle", "Slack API Error");
        model.addAttribute(
            "errorMessage",
            "Failed to fetch file or user info from Slack. Please try again later.");
        model.addAttribute("buttonText", "Back to Slack");
        model.addAttribute("statusCode", 500);
        return "error";
      }

      var config =
          configManagerService.createConfig(
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
          "documentServerApiUrl",
          String.format("%s/web-apps/apps/api/documents/api.js", settings.getAddress()));
      return "editor";
    } catch (InterruptedException | ExecutionException e) {
      model.addAttribute("errorTitle", "Internal Error");
      model.addAttribute(
          "errorMessage",
          "An error occurred while loading the document editor. Please try again later.");
      model.addAttribute("buttonText", "Back to Slack");
      model.addAttribute("statusCode", 500);
      return "error";
    }
  }

  private Optional<EditorSession> retrieveAndRemoveSession(final String sessionId) {
    return Optional.ofNullable(sessions.remove(sessionId));
  }

  private Optional<Installer> findInstaller(final EditorSession session) {
    return Optional.ofNullable(
        rotatingInstallationService.findInstallerWithRotation(
            null, session.getTeamId(), session.getUserId()));
  }

  private SettingsResponse getEffectiveSettings(final String teamId) {
    var settings = settingsService.findSettings(teamId);
    if (settings.isDemoEnabled()) applyDemoSettings(settings);

    return settings;
  }

  private void applyDemoSettings(final SettingsResponse settings) {
    settings.setAddress(serverConfigurationProperties.getDemo().getAddress());
    settings.setHeader(serverConfigurationProperties.getDemo().getHeader());
    settings.setSecret(serverConfigurationProperties.getDemo().getSecret());
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
}
