package com.onlyoffice.slack.shared.transfer.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentServerVersion {
  private int error;
  private String version;
}
