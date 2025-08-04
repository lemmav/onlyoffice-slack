package com.onlyoffice.slack.shared.utils;

import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.shared.exception.DecryptionException;
import com.onlyoffice.slack.shared.exception.EncryptionException;
import jakarta.validation.constraints.NotBlank;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AesEncryptionService implements EncryptionService {
  private final ServerConfigurationProperties properties;

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final String FACTORY_INSTANCE = "PBKDF2WithHmacSHA256";
  private static final int TAG_LENGTH_BIT = 128;
  private static final int IV_LENGTH_BYTE = 12;
  private static final int SALT_LENGTH_BYTE = 32;
  private static final String ALGORITHM_TYPE = "AES";
  private static final int KEY_LENGTH = 256;
  private static final int ITERATION_COUNT = 3000; // Min 1000
  private static final Charset UTF_8 = StandardCharsets.UTF_8;

  private byte[] getRandomNonce(final int length) {
    var nonce = new byte[length];
    new SecureRandom().nextBytes(nonce);
    return nonce;
  }

  private SecretKey getSecretKey(final String password, final byte[] salt)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    var spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
    var factory = SecretKeyFactory.getInstance(FACTORY_INSTANCE);
    return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ALGORITHM_TYPE);
  }

  private Cipher initCipher(final int mode, final SecretKey secretKey, final byte[] iv)
      throws InvalidKeyException,
          InvalidAlgorithmParameterException,
          NoSuchPaddingException,
          NoSuchAlgorithmException {
    var cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(mode, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
    return cipher;
  }

  @Override
  public String encrypt(@NotBlank final String plainText) throws EncryptionException {
    try {
      var salt = getRandomNonce(SALT_LENGTH_BYTE);
      var secretKey = getSecretKey(properties.getCryptography().getSecret(), salt);
      var iv = getRandomNonce(IV_LENGTH_BYTE);
      var cipher = initCipher(Cipher.ENCRYPT_MODE, secretKey, iv);
      var encryptedMessageByte = cipher.doFinal(plainText.getBytes(UTF_8));
      var cipherByte =
          ByteBuffer.allocate(iv.length + salt.length + encryptedMessageByte.length)
              .put(iv)
              .put(salt)
              .put(encryptedMessageByte)
              .array();

      return Base64.getEncoder().encodeToString(cipherByte);
    } catch (Exception e) {
      throw new EncryptionException(e);
    }
  }

  @Override
  public String decrypt(@NotBlank final String cipherText) throws DecryptionException {
    try {
      var decodedCipherByte = Base64.getDecoder().decode(cipherText.getBytes(UTF_8));
      var byteBuffer = ByteBuffer.wrap(decodedCipherByte);

      var iv = new byte[IV_LENGTH_BYTE];
      byteBuffer.get(iv);

      var salt = new byte[SALT_LENGTH_BYTE];
      byteBuffer.get(salt);

      var encryptedByte = new byte[byteBuffer.remaining()];
      byteBuffer.get(encryptedByte);

      var secretKey = getSecretKey(properties.getCryptography().getSecret(), salt);
      var cipher = initCipher(Cipher.DECRYPT_MODE, secretKey, iv);

      var decryptedMessageByte = cipher.doFinal(encryptedByte);

      return new String(decryptedMessageByte, UTF_8);
    } catch (Exception e) {
      throw new DecryptionException(e);
    }
  }
}
