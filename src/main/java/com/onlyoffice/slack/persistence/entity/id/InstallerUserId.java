package com.onlyoffice.slack.persistence.entity.id;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallerUserId implements Serializable {
  private String teamId;
  private String installerUserId;
}
