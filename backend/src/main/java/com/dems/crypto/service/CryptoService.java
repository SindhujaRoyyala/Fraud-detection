package com.dems.crypto.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class CryptoService {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final String SHA_256 = "SHA-256";

    private final byte[] aesKeyBytes;
    private final byte[] saltBytes;
    private final SecureRandom secureRandom;

    public CryptoService(
            @Value("${app.security.aes.secret-key}") String aesSecretKey,
            @Value("${app.security.aes.salt}") String aesSalt) {
        this.aesKeyBytes = deriveKey(aesSecretKey, aesSalt);
        this.saltBytes = aesSalt.getBytes(StandardCharsets.UTF_8);
        this.secureRandom = new SecureRandom();
    }

    private byte[] deriveKey(String secret, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(hash, 32);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive AES key", e);
        }
    }

    public String encryptAes256Gcm(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            SecretKey secretKey = new SecretKeySpec(aesKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, combined, GCM_IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    public String decryptAes256Gcm(String encryptedBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            SecretKey secretKey = new SecretKeySpec(aesKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    public String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }

    public String sha256Hash(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hashBytes = digest.digest(input);
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }

    public String sha256HashStream(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 stream hashing failed", e);
        }
    }

    public String computeAuditEntryHash(Long blockNumber, String event, UUID userId,
                                         String description, String metadata,
                                         Instant createdAt, String previousHash) {
        StringBuilder sb = new StringBuilder();
        sb.append(blockNumber).append("|");
        sb.append(event).append("|");
        sb.append(userId != null ? userId.toString() : "").append("|");
        sb.append(description != null ? description : "").append("|");
        sb.append(metadata != null ? metadata : "").append("|");
        sb.append(createdAt.toString()).append("|");
        sb.append(previousHash != null ? previousHash : "GENESIS");
        return sha256Hash(sb.toString());
    }

    public String generateSecureRandomToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return UUID.nameUUIDFromBytes(tokenBytes) + "-" + bytesToHex(Arrays.copyOfRange(tokenBytes, 0, 8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
