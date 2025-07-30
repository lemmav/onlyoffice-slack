package com.onlyoffice.slack.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.onlyoffice.slack.configuration.slack.SlackMessageConfigurationProperties;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;

@ExtendWith(MockitoExtension.class)
class SlackInstallationControllerTests {
  private SlackInstallationController controller;

  @Mock private SlackMessageConfigurationProperties slackMessageConfigurationProperties;
  @Mock private MessageSource messageSource;
  @Mock private Model model;

  @BeforeEach
  void setUp() {
    controller =
        new SlackInstallationController(slackMessageConfigurationProperties, messageSource);
  }

  @Test
  void whenCompletionCalled_thenReturnsCompletionViewAndModel() {
    when(slackMessageConfigurationProperties.getMessageCompletionTitle())
        .thenReturn("completion.title");
    when(slackMessageConfigurationProperties.getMessageCompletionText())
        .thenReturn("completion.text");
    when(slackMessageConfigurationProperties.getMessageCompletionDescription())
        .thenReturn("completion.description");
    when(slackMessageConfigurationProperties.getMessageCompletionButton())
        .thenReturn("completion.button");

    when(messageSource.getMessage(eq("completion.title"), any(), eq(Locale.ENGLISH)))
        .thenReturn("Title");
    when(messageSource.getMessage(eq("completion.text"), any(), eq(Locale.ENGLISH)))
        .thenReturn("Text");
    when(messageSource.getMessage(eq("completion.description"), any(), eq(Locale.ENGLISH)))
        .thenReturn("Description");
    when(messageSource.getMessage(eq("completion.button"), any(), eq(Locale.ENGLISH)))
        .thenReturn("Button");

    var result = controller.completion(model);

    assertEquals("completion", result);
    verify(model).addAttribute("completionTitle", "Title");
    verify(model).addAttribute("completionText", "Text");
    verify(model).addAttribute("completionDescription", "Description");
    verify(model).addAttribute("completionButton", "Button");
  }

  @Test
  void whenCancellationCalled_thenReturnsCancellationViewAndModel() {
    when(slackMessageConfigurationProperties.getMessageCancellationTitle())
        .thenReturn("cancellation.title");
    when(slackMessageConfigurationProperties.getMessageCancellationText())
        .thenReturn("cancellation.text");
    when(slackMessageConfigurationProperties.getMessageCancellationDescription())
        .thenReturn("cancellation.description");
    when(slackMessageConfigurationProperties.getMessageCancellationButton())
        .thenReturn("cancellation.button");

    when(messageSource.getMessage(eq("cancellation.title"), any(), eq(Locale.ENGLISH)))
        .thenReturn("Title");
    when(messageSource.getMessage(eq("cancellation.text"), any(), eq(Locale.ENGLISH)))
        .thenReturn("Text");
    when(messageSource.getMessage(eq("cancellation.description"), any(), eq(Locale.ENGLISH)))
        .thenReturn("Description");
    when(messageSource.getMessage(eq("cancellation.button"), any(), eq(Locale.ENGLISH)))
        .thenReturn("Button");

    var result = controller.cancellation(model);

    assertEquals("cancellation", result);
    verify(model).addAttribute("cancellationTitle", "Title");
    verify(model).addAttribute("cancellationText", "Text");
    verify(model).addAttribute("cancellationDescription", "Description");
    verify(model).addAttribute("cancellationButton", "Button");
  }
}
