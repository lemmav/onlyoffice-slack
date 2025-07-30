package com.onlyoffice.slack.persistence.converter;

import com.onlyoffice.slack.service.cryptography.AesEncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Converter
@RequiredArgsConstructor
public class EncryptionAttributeConverter implements AttributeConverter<String, String> {
  private final AesEncryptionService encryptionService;

  @Override
  public String convertToDatabaseColumn(String token) {
    if (token == null || token.trim().isEmpty()) return token;

    return encryptionService.encrypt(token);
  }

  @Override
  public String convertToEntityAttribute(String encToken) {
    if (encToken == null || encToken.trim().isEmpty()) return encToken;

    return encryptionService.decrypt(encToken);
  }
}
