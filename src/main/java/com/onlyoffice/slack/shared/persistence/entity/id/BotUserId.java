package com.onlyoffice.slack.shared.persistence.entity.id;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotUserId implements Serializable {
  private String teamId;
  private String botId;
}
