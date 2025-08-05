package com.onlyoffice.slack.domain.document.editor.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.slack.domain.document.DocumentServerConfigurationProperties;
import com.onlyoffice.slack.shared.exception.domain.DocumentTokenValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Base64;
import java.util.Calendar;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
class DocumentJwtManagerServiceImpl implements DocumentJwtManagerService {
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final DocumentServerConfigurationProperties documentServerConfigurationProperties;

  @Override
  public String createToken(@NotNull Object object, @NotBlank String key) {
    var payloadMap = objectMapper.convertValue(object, new TypeReference<Map<String, ?>>() {});
    return createToken(payloadMap, key);
  }

  @Override
  public String createToken(@NotNull Map<String, ?> payloadMap, @NotBlank String key) {
    var algorithm = Algorithm.HMAC256(key);
    var calendar = Calendar.getInstance();
    var issuedAt = calendar.toInstant();
    calendar.add(
        Calendar.MINUTE, documentServerConfigurationProperties.getJwt().getKeepAliveMinutes());

    return JWT.create()
        .withIssuedAt(issuedAt)
        .withExpiresAt(calendar.toInstant())
        .withPayload(payloadMap)
        .sign(algorithm);
  }

  @Override
  public Map<String, Object> verifyToken(@NotBlank String token, @NotBlank String key) {
    try {
      var algorithm = Algorithm.HMAC256(key);
      var decoder = Base64.getUrlDecoder();

      var jwt =
          JWT.require(algorithm)
              .acceptLeeway(
                  documentServerConfigurationProperties.getJwt().getAcceptableLeewaySeconds())
              .build()
              .verify(token);

      var payloadJson = new String(decoder.decode(jwt.getPayload()));
      return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      throw new DocumentTokenValidationException("Failed to verify and parse JWT token", e);
    }
  }
}
