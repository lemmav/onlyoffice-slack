package com.onlyoffice.slack.domain.slack.installation;

import com.slack.api.bolt.model.Installer;
import com.slack.api.bolt.service.InstallationService;

public interface RotatingInstallationService extends InstallationService {
  Installer findInstallerWithRotation(String enterpriseId, String teamId, String userId);
}
