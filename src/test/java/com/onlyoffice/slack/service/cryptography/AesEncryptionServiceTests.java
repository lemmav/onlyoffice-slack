package com.onlyoffice.slack.service.cryptography;

import static org.junit.jupiter.api.Assertions.*;

import com.onlyoffice.slack.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.configuration.ServerConfigurationProperties.CryptographyProperties;
import com.onlyoffice.slack.exception.DecryptionException;
import com.onlyoffice.slack.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AesEncryptionServiceTests {
  private static final String SECRET = "test";
  private static final String WRONG_SECRET = "wrong";
  private AesEncryptionService aesEncryptionService;

  @BeforeEach
  void setUp() {
    var properties = Mockito.mock(ServerConfigurationProperties.class);
    var cryptography = Mockito.mock(CryptographyProperties.class);

    Mockito.when(properties.getCryptography()).thenReturn(cryptography);
    Mockito.when(cryptography.getSecret()).thenReturn(SECRET);

    aesEncryptionService = new AesEncryptionService(properties);
  }

  @Test
  void whenEncryptAndDecryptWithSameKey_thenOriginalTextReturned()
      throws EncryptionException, DecryptionException {
    var plainText = "Test data";
    var cipherText = aesEncryptionService.encrypt(plainText);
    var decrypted = aesEncryptionService.decrypt(cipherText);
    assertEquals(plainText, decrypted);
  }

  @Test
  void whenDecryptWithWrongKey_thenThrowDecryptionException() throws EncryptionException {
    var plainText = "Test data";
    var cipherText = aesEncryptionService.encrypt(plainText);

    var wrongProperties = Mockito.mock(ServerConfigurationProperties.class);
    var wrongCryptography = Mockito.mock(CryptographyProperties.class);

    Mockito.when(wrongProperties.getCryptography()).thenReturn(wrongCryptography);
    Mockito.when(wrongCryptography.getSecret()).thenReturn(WRONG_SECRET);

    var wrongService = new AesEncryptionService(wrongProperties);

    assertThrows(DecryptionException.class, () -> wrongService.decrypt(cipherText));
  }

  @Test
  void whenEncryptAndDecryptEmptyString_thenEmptyStringReturned()
      throws EncryptionException, DecryptionException {
    var plainText = "";
    var cipherText = aesEncryptionService.encrypt(plainText);
    var decrypted = aesEncryptionService.decrypt(cipherText);
    assertEquals(plainText, decrypted);
  }
}
