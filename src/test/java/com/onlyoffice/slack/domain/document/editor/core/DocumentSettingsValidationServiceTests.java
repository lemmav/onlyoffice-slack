package com.onlyoffice.slack.domain.document.editor.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onlyoffice.slack.shared.configuration.client.OkHttpClientPoolService;
import com.onlyoffice.slack.shared.transfer.command.DocumentServerCommand;
import com.onlyoffice.slack.shared.transfer.request.SubmitSettingsRequest;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentSettingsValidationServiceTests {
  @Mock OkHttpClientPoolService httpClientPoolService;
  @Mock DocumentJwtManagerService documentJwtManagerService;
  @InjectMocks DocumentSettingsValidationServiceImpl documentSettingsValidationService;

  @Test
  void whenValidateConnectionWithValidRequest_thenNoExceptionThrown() throws IOException {
    var request = mock(SubmitSettingsRequest.class);

    when(request.getAddress()).thenReturn("https://example.com");
    when(request.getHeader()).thenReturn("header");
    when(request.getSecret()).thenReturn("secret");
    when(request.isDemoEnabled()).thenReturn(false);

    var client = mock(OkHttpClient.class);

    when(httpClientPoolService.getHttpClient()).thenReturn(client);

    var call = mock(Call.class);

    when(client.newCall(any(Request.class))).thenReturn(call);

    var response = mock(Response.class);
    var responseBody = mock(ResponseBody.class);

    when(response.isSuccessful()).thenReturn(true);
    when(response.body()).thenReturn(responseBody);
    when(responseBody.bytes()).thenReturn("{\"error\":0}".getBytes());
    when(call.execute()).thenReturn(response);

    doReturn("token")
        .when(documentJwtManagerService)
        .createToken(any(DocumentServerCommand.class), anyString());
    assertDoesNotThrow(() -> documentSettingsValidationService.validateConnection(request));
  }

  @Test
  void whenValidateConnectionWithInvalidAddress_thenThrowException() {
    var request = mock(SubmitSettingsRequest.class);

    when(request.getAddress()).thenReturn("http://invalid.com");
    assertThrows(
        Exception.class, () -> documentSettingsValidationService.validateConnection(request));
  }

  @Test
  void whenValidateConnectionWithDemoMode_thenSkipValidation() {
    var request = mock(SubmitSettingsRequest.class);

    when(request.isDemoEnabled()).thenReturn(true);
    when(request.isValidConfiguration()).thenReturn(false);
    assertDoesNotThrow(() -> documentSettingsValidationService.validateConnection(request));
  }
}
