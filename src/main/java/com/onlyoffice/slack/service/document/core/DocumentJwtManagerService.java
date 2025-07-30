package com.onlyoffice.slack.service.document.core;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public interface DocumentJwtManagerService {
  String createToken(@NotNull Object object, @NotBlank String key);

  String createToken(@NotNull Map<String, ?> payloadMap, @NotBlank String key);

  Map<String, Object> verifyToken(@NotBlank String token, @NotBlank String key);
}
