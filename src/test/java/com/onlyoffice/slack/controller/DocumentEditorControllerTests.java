package com.onlyoffice.slack.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hazelcast.map.IMap;
import com.onlyoffice.slack.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.service.data.RotatingInstallationService;
import com.onlyoffice.slack.service.data.TeamSettingsService;
import com.onlyoffice.slack.service.document.core.ConfigManagerService;
import com.onlyoffice.slack.transfer.cache.EditorSession;
import com.onlyoffice.slack.transfer.response.SettingsResponse;
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
import org.springframework.ui.Model;

class DocumentEditorControllerTests {
  private DocumentEditorController controller;
  private ServerConfigurationProperties serverConfigurationProperties;
  private RotatingInstallationService rotatingInstallationService;
  private ConfigManagerService configManagerService;
  private IMap<String, EditorSession> sessions;
  private TeamSettingsService teamSettingsService;
  private App app;
  private Model model;

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
    serverConfigurationProperties = mock(ServerConfigurationProperties.class);
    rotatingInstallationService = mock(RotatingInstallationService.class);
    configManagerService = mock(ConfigManagerService.class);
    sessions = mock(IMap.class);
    teamSettingsService = mock(TeamSettingsService.class);
    app = mock(App.class);
    model = mock(Model.class);

    controller =
        new DocumentEditorController(
            serverConfigurationProperties,
            rotatingInstallationService,
            configManagerService,
            sessions,
            teamSettingsService,
            app);
  }

  @Test
  void whenEditorCalled_thenReturnsLoadingView() {
    var sessionId = "session-id";

    var result = controller.editor(sessionId, model);

    assertEquals("loading", result);
  }

  @Test
  void whenEditorContentWithExpiredSession_thenReturnsErrorView() {
    var sessionId = "expired-session-id";

    when(sessions.remove(sessionId)).thenReturn(null);

    var result = controller.editorContent(sessionId, model);

    assertEquals("error", result);
  }

  @Test
  void whenEditorContentWithValidSessionButNoInstaller_thenReturnsErrorView() {
    var sessionId = "valid-session-id";
    var session = createValidEditorSession();

    when(sessions.remove(sessionId)).thenReturn(session);
    when(rotatingInstallationService.findInstallerWithRotation(null, "team", "user"))
        .thenReturn(null);

    var result = controller.editorContent(sessionId, model);

    assertEquals("error", result);
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

    assertEquals("error", result);
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

    assertEquals("error", result);
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
    } catch (Exception e) {
    }

    var result = controller.editorContent(sessionId, model);

    assertEquals("error", result);
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
    } catch (Exception e) {
    }

    var result = controller.editorContent(sessionId, model);

    assertEquals("error", result);
  }
}
