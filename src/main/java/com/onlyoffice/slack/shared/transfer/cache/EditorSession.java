package com.onlyoffice.slack.shared.transfer.cache;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditorSession implements Serializable {
  @NotBlank private String teamId;
  @NotBlank private String userId;
  @NotBlank private String userName;
  @NotBlank private String fileId;
  @NotBlank private String fileName;
  @NotBlank private String channelId;
  @NotBlank private String messageTs;
}
