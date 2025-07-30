package com.onlyoffice.slack.service.cryptography;

import com.onlyoffice.slack.exception.DecryptionException;
import com.onlyoffice.slack.exception.EncryptionException;
import jakarta.validation.constraints.NotBlank;

public interface EncryptionService {
  String encrypt(@NotBlank final String plainText) throws EncryptionException;

  String decrypt(@NotBlank final String cipherText) throws DecryptionException;
}
