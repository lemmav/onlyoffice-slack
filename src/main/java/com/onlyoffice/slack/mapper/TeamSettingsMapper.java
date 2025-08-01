package com.onlyoffice.slack.mapper;

import com.onlyoffice.slack.persistence.entity.TeamSettings;
import com.onlyoffice.slack.transfer.response.SettingsResponse;
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
