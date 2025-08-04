package com.onlyoffice.slack.shared.transfer.cache;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSessionKey {
  @NotBlank private String key;
  @NotBlank private String channelId;
  @NotBlank private String messageTs;
}
