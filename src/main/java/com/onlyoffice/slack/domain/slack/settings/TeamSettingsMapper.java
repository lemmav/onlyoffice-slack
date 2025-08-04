package com.onlyoffice.slack.domain.slack.settings;

import com.onlyoffice.slack.shared.persistence.entity.TeamSettings;
import com.onlyoffice.slack.shared.transfer.response.SettingsResponse;
import com.onlyoffice.slack.shared.utils.Mapper;
import org.springframework.stereotype.Component;

@Component
public class TeamSettingsMapper implements Mapper<TeamSettings, SettingsResponse> {

  @Override
  public SettingsResponse map(TeamSettings source) {
    if (source == null) {
      return SettingsResponse.builder().build();
    }

    return SettingsResponse.builder()
        .address(source.getAddress())
        .header(source.getHeader())
        .secret(source.getSecret())
        .demoEnabled(source.getDemoEnabled())
        .demoStartedDate(source.getDemoStartedDate())
        .build();
  }
}
