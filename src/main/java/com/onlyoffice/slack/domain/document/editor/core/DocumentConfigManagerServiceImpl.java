package com.onlyoffice.slack.domain.document.editor.core;

import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.EditorConfig;
import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.shared.transfer.command.BuildConfigCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class DocumentConfigManagerServiceImpl implements DocumentConfigManagerService {
  private final ServerConfigurationProperties serverConfigurationProperties;

  private final DocumentJwtManagerService documentJwtManagerService;
  private final DocumentFileManagerService documentManagerService;

  @Override
  public Config createConfig(@Valid final BuildConfigCommand command) {
    var user = command.getUser();
    var file = command.getFile();
    var config =
        Config.builder()
            .width("100%")
            .height("100%")
            .type(command.getType())
            .documentType(documentManagerService.getDocumentType(file))
            .document(
                documentManagerService.getDocument(
                    user.getTeamId(),
                    user.getId(),
                    file,
                    command.getChannelId(),
                    command.getMessageTs()))
            .editorConfig(
                EditorConfig.builder()
                    .mode(command.getMode())
                    .user(
                        User.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .image(user.getProfile().getImage32())
                            .build())
                    .callbackUrl(
                        "%s/callback?file=%s"
                            .formatted(
                                serverConfigurationProperties.getBaseAddress(), file.getId()))
                    .build())
            .build();

    config.setToken(documentJwtManagerService.createToken(config, command.getSigningSecret()));

    return config;
  }
}
