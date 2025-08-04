package com.onlyoffice.slack.shared.transfer.command;

import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.slack.api.model.File;
import com.slack.api.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildConfigCommand {
  @NotBlank private String channelId;
  @NotBlank private String messageTs;
  @NotBlank private String signingSecret;
  @NotNull private User user;
  @NotNull private File file;
  @NotNull private Mode mode;
  @NotNull private Type type;
}
