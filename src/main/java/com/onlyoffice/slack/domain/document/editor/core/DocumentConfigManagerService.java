package com.onlyoffice.slack.domain.document.editor.core;

import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.slack.shared.transfer.command.BuildConfigCommand;
import jakarta.validation.Valid;

public interface DocumentConfigManagerService {
  Config createConfig(@Valid final BuildConfigCommand command);
}
