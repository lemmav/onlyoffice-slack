package com.onlyoffice.slack.service.document.core;

import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.slack.transfer.command.BuildConfigCommand;
import jakarta.validation.Valid;

public interface DocumentConfigManagerService {
  Config createConfig(@Valid final BuildConfigCommand command);
}
