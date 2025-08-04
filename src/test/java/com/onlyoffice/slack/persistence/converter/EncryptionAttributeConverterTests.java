package com.onlyoffice.slack.persistence.converter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onlyoffice.slack.shared.persistence.converter.EncryptionAttributeConverter;
import com.onlyoffice.slack.shared.utils.AesEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncryptionAttributeConverterTests {
  private AesEncryptionService encryptionService;
  private EncryptionAttributeConverter converter;

  @BeforeEach
  void setUp() {
    encryptionService = mock(AesEncryptionService.class);
    converter = new EncryptionAttributeConverter(encryptionService);
  }

  @Test
  void whenNonNullNonEmptyValueProvidedToConvertToDatabaseColumn_thenEncryptsValue() {
    var plain = "secret";
    var encrypted = "encrypted";

    when(encryptionService.encrypt(plain)).thenReturn(encrypted);
    assertEquals(encrypted, converter.convertToDatabaseColumn(plain));
    verify(encryptionService).encrypt(plain);
  }

  @Test
  void whenNullProvidedToConvertToDatabaseColumn_thenReturnsNull() {
    assertNull(converter.convertToDatabaseColumn(null));
    verifyNoInteractions(encryptionService);
  }

  @Test
  void whenEmptyStringProvidedToConvertToDatabaseColumn_thenReturnsEmptyString() {
    assertEquals("", converter.convertToDatabaseColumn(""));
    verifyNoInteractions(encryptionService);
  }

  @Test
  void whenNonNullNonEmptyValueProvidedToConvertToEntityAttribute_thenDecryptsValue() {
    var encrypted = "encrypted";
    var plain = "secret";

    when(encryptionService.decrypt(encrypted)).thenReturn(plain);
    assertEquals(plain, converter.convertToEntityAttribute(encrypted));
    verify(encryptionService).decrypt(encrypted);
  }

  @Test
  void whenNullProvidedToConvertToEntityAttribute_thenReturnsNull() {
    assertNull(converter.convertToEntityAttribute(null));
    verifyNoInteractions(encryptionService);
  }

  @Test
  void whenEmptyStringProvidedToConvertToEntityAttribute_thenReturnsEmptyString() {
    assertEquals("", converter.convertToEntityAttribute(""));
    verifyNoInteractions(encryptionService);
  }
}
