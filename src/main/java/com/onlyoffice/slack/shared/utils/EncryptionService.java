package com.onlyoffice.slack.shared.utils;

import com.onlyoffice.slack.shared.exception.DecryptionException;
import com.onlyoffice.slack.shared.exception.EncryptionException;
import jakarta.validation.constraints.NotBlank;

public interface EncryptionService {
  String encrypt(@NotBlank final String plainText) throws EncryptionException;

  String decrypt(@NotBlank final String cipherText) throws DecryptionException;
}
