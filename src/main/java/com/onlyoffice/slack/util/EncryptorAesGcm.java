package com.onlyoffice.slack.util;

import com.onlyoffice.slack.configuration.general.IntegrationConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class EncryptorAesGcm {
    private final int GCM_KEY_SIZE = 256;
    private final int TAG_LENGTH_BIT = 128;
    private final int GCM_IV_LENGTH = 12;
    private final int GCM_SALT_LENGTH = 16;
    private final String ALGORITHM = "AES/GCM/NoPadding";

    private final IntegrationConfiguration integrationConfiguration;

    private byte[] getRandomNonce(int bytes) {
        log.debug("Generating nonce");
        byte[] nonce = new byte[bytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    private SecretKey getKey(byte[] nonce) throws Exception {
        log.debug("Getting a secret key from nonce");
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(integrationConfiguration.getAesSecret().toCharArray(), nonce, 65536, GCM_KEY_SIZE);
        SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        return key;
    }

    public String encrypt(String plainText) throws Exception {
        log.debug("Encrypting: {}", plainText);
        byte[] salt = getRandomNonce(GCM_SALT_LENGTH);
        byte[] iv = getRandomNonce(GCM_IV_LENGTH);

        SecretKey key = getKey(salt);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] result = ByteBuffer.allocate(iv.length + salt.length + cipherText.length)
                .put(iv)
                .put(salt)
                .put(cipherText)
                .array();

        log.debug("Successfully encrypted: {}", plainText);
        return Base64.getEncoder().encodeToString(result);
    }

    public String decrypt(String cipherText) throws Exception {
        log.debug("Decrypting: {}", cipherText);
        byte[] decoded = Base64.getDecoder().decode(cipherText.getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = ByteBuffer.wrap(decoded);

        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        byte[] salt = new byte[GCM_SALT_LENGTH];
        buffer.get(salt);

        byte[] text = new byte[buffer.remaining()];
        buffer.get(text);

        SecretKey key = getKey(salt);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] plainText = cipher.doFinal(text);

        log.debug("Successfully decrypted: {}", cipherText);
        return new String(plainText, StandardCharsets.UTF_8);
    }
}
