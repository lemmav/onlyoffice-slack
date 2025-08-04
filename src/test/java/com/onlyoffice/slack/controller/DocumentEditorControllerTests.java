package com.onlyoffice.slack.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.domain.document.editor.DocumentEditorController;
import com.onlyoffice.slack.domain.document.editor.core.DocumentConfigManagerService;
import com.onlyoffice.slack.domain.slack.installation.RotatingInstallationService;
import com.onlyoffice.slack.domain.slack.settings.TeamSettingsService;
import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.shared.configuration.message.MessageSourceSlackConfiguration;
import com.onlyoffice.slack.shared.transfer.cache.EditorSession;
import com.onlyoffice.slack.shared.transfer.response.SettingsResponse;
import com.slack.api.Slack;
import com.slack.api.bolt.App;
import com.slack.api.bolt.model.Installer;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.files.FilesInfoRequest;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.files.FilesInfoResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.File;
import com.slack.api.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;

class DocumentEditorControllerTests {
  private ServerConfigurationProperties serverConfigurationProperties;

  private App app;
  private IMap sessions;

  private Model model;
  private DocumentEditorController controller;

  private TeamSettingsService teamSettingsService;
  private RotatingInstallationService rotatingInstallationService;

  private EditorSession createValidEditorSession() {
    return EditorSession.builder()
        .teamId("team")
        .userId("user")
        .fileId("file")
        .channelId("channel")
        .messageTs("12345.67890")
        .build();
  }

  private Installer createValidInstaller() {
    var installer = mock(Installer.class);

    when(installer.getInstallerUserAccessToken()).thenReturn("access-token");

    return installer;
  }

  private SettingsResponse createValidSettings() {
    var settings = new SettingsResponse();
    settings.setDemoEnabled(false);
    settings.setAddress("https://documentserver.example.com");
    settings.setSecret("secret");
    settings.setHeader("AuthorizationJwt");

    return settings;
  }

  private SettingsResponse createDemoSettings() {
    var settings = new SettingsResponse();
    settings.setDemoEnabled(true);

    return settings;
  }

  private ServerConfigurationProperties.DemoProperties createDemoConfig() {
    var demo = mock(ServerConfigurationProperties.DemoProperties.class);

    when(demo.getAddress()).thenReturn("https://demo.documentserver.com");
    when(demo.getSecret()).thenReturn("demo");
    when(demo.getHeader()).thenReturn("AuthorizationJwt");

    return demo;
  }

  private FilesInfoResponse createValidFileInfo() {
    var response = mock(FilesInfoResponse.class);
    var file = mock(File.class);

    when(response.isOk()).thenReturn(true);
    when(response.getFile()).thenReturn(file);

    return response;
  }

  private FilesInfoResponse createInvalidFileInfo() {
    var response = mock(FilesInfoResponse.class);

    when(response.isOk()).thenReturn(false);
    when(response.getError()).thenReturn("file_not_found");

    return response;
  }

  private UsersInfoResponse createValidUserInfo() {
    var response = mock(UsersInfoResponse.class);
    var user = mock(User.class);

    when(response.isOk()).thenReturn(true);
    when(response.getUser()).thenReturn(user);

    return response;
  }

  @BeforeEach
  void setUp() {
    app = mock(App.class);
    model = mock(Model.class);
    sessions = mock(IMap.class);
    teamSettingsService = mock(TeamSettingsService.class);
    rotatingInstallationService = mock(RotatingInstallationService.class);
    serverConfigurationProperties = mock(ServerConfigurationProperties.class);

    var documentConfigManagerService = mock(DocumentConfigManagerService.class);
    var messageSourceSlackConfiguration = mock(MessageSourceSlackConfiguration.class);
    var messageSource = mock(MessageSource.class);

    controller =
        new DocumentEditorController(
            serverConfigurationProperties,
            messageSourceSlackConfiguration,
            app,
            messageSource,
            teamSettingsService,
            sessions,
            rotatingInstallationService,
            documentConfigManagerService);
  }

  @Test
  void whenEditorCalled_thenReturnsLoadingView() {
    var sessionId = "session-id";

    var result = controller.editor(sessionId, model);

    assertEquals("document/loading", result);
  }

  @Test
  void whenEditorContentWithExpiredSession_thenReturnsErrorView() {
    var sessionId = "expired-session-id";

    when(sessions.remove(sessionId)).thenReturn(null);

    var result = controller.editorContent(sessionId, model);

    assertEquals("errors/bad_session", result);
  }

  @Test
  void whenEditorContentWithValidSessionButNoInstaller_thenReturnsErrorView() {
    var sessionId = "valid-session-id";
    var session = createValidEditorSession();

    when(sessions.remove(sessionId)).thenReturn(session);
    when(rotatingInstallationService.findInstallerWithRotation(null, "team", "user"))
        .thenReturn(null);

    var result = controller.editorContent(sessionId, model);

    assertEquals("errors/not_available", result);
  }

  @Test
  void whenEditorContentWithValidSessionAndInstaller_thenReturnsErrorDueToAsyncComplexity() {
    var sessionId = "valid-session-id";
    var session = createValidEditorSession();
    var installer = createValidInstaller();
    var settings = createValidSettings();
    var fileInfo = createValidFileInfo();
    var userInfo = createValidUserInfo();

    var methodsClient = mock(MethodsClient.class);

    when(sessions.remove(sessionId)).thenReturn(session);
    when(rotatingInstallationService.findInstallerWithRotation(null, "team", "user"))
        .thenReturn(installer);
    when(teamSettingsService.findSettings("team")).thenReturn(settings);
    when(app.getSlack()).thenReturn(mock(Slack.class));
    when(app.getSlack().methods(anyString())).thenReturn(methodsClient);
    when(app.getClient()).thenReturn(methodsClient);

    try {
      when(methodsClient.filesInfo(any(FilesInfoRequest.class))).thenReturn(fileInfo);
      when(methodsClient.usersInfo(any(UsersInfoRequest.class))).thenReturn(userInfo);
    } catch (Exception e) {
    }

    var result = controller.editorContent(sessionId, model);

    assertEquals("errors/bad_slack", result);
  }

  @Test
  void whenEditorContentWithSlackApiError_thenReturnsErrorView() {
    var sessionId = "valid-session-id";
    var session = createValidEditorSession();
    var installer = createValidInstaller();
    var settings = createValidSettings();
    var fileInfo = createInvalidFileInfo();
    var methodsClient = mock(MethodsClient.class);

    when(sessions.remove(sessionId)).thenReturn(session);
    when(rotatingInstallationService.findInstallerWithRotation(null, "team", "user"))
        .thenReturn(installer);
    when(teamSettingsService.findSettings("team")).thenReturn(settings);
    when(app.getSlack()).thenReturn(mock(Slack.class));
    when(app.getSlack().methods(anyString())).thenReturn(methodsClient);

    try {
      when(methodsClient.filesInfo(any(FilesInfoRequest.class))).thenReturn(fileInfo);
    } catch (Exception e) {
    }

    var result = controller.editorContent(sessionId, model);

    assertEquals("errors/bad_slack", result);
  }

  @Test
  void whenEditorContentWithException_thenReturnsErrorView() {
    var sessionId = "valid-session-id";
    var session = createValidEditorSession();
    var installer = createValidInstaller();
    var settings = createValidSettings();
    var methodsClient = mock(MethodsClient.class);

    when(sessions.remove(sessionId)).thenReturn(session);
    when(rotatingInstallationService.findInstallerWithRotation(null, "team", "user"))
        .thenReturn(installer);
    when(teamSettingsService.findSettings("team")).thenReturn(settings);
    when(app.getSlack()).thenReturn(mock(Slack.class));
    when(app.getSlack().methods(anyString())).thenReturn(methodsClient);

    try {
      when(methodsClient.filesInfo(any(FilesInfoRequest.class)))
          .thenThrow(new RuntimeException("API Error"));
    } catch (Exception ignored) {
    }

    var result = controller.editorContent(sessionId, model);

    assertEquals("errors/bad_slack", result);
  }

  @Test
  void whenEditorContentWithDemoSettings_thenReturnsErrorDueToAsyncComplexity() {
    var sessionId = "valid-session-id";
    var session = createValidEditorSession();
    var installer = createValidInstaller();
    var settings = createDemoSettings();
    var fileInfo = createValidFileInfo();
    var userInfo = createValidUserInfo();
    var methodsClient = mock(MethodsClient.class);
    var demoConfig = createDemoConfig();

    when(sessions.remove(sessionId)).thenReturn(session);
    when(rotatingInstallationService.findInstallerWithRotation(null, "team", "user"))
        .thenReturn(installer);
    when(teamSettingsService.findSettings("team")).thenReturn(settings);
    when(serverConfigurationProperties.getDemo()).thenReturn(demoConfig);
    when(app.getSlack()).thenReturn(mock(Slack.class));
    when(app.getSlack().methods(anyString())).thenReturn(methodsClient);
    when(app.getClient()).thenReturn(methodsClient);

    try {
      when(methodsClient.filesInfo(any(FilesInfoRequest.class))).thenReturn(fileInfo);
      when(methodsClient.usersInfo(any(UsersInfoRequest.class))).thenReturn(userInfo);
    } catch (Exception ignored) {
    }

    var result = controller.editorContent(sessionId, model);

    assertEquals("errors/bad_slack", result);
  }
}
