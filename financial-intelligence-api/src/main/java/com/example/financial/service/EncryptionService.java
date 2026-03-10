package com.example.financial.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption service for Plaid access tokens.
 * <p>
 * • Uses AES‑256‑GCM with a random 12‑byte IV (nonce) per encryption.
 * • Stores the encrypted value as Base64(IV || ciphertext).
 * • Supports decryption of legacy tokens that were encrypted with AES‑CBC and a
 * static zero IV.
 * • The encryption key is taken from the environment variable
 * {@code PLAID_ENCRYPTION_KEY}
 * and must decode to exactly 32 bytes (256‑bit).
 * </p>
 */
@Service
public class EncryptionService {

    /** Base64‑encoded 256‑bit key from environment */
    @Value("${PLAID_ENCRYPTION_KEY}")
    private String base64Key;

    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String LEGACY_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes – recommended for GCM
    private static final int LEGACY_IV_LENGTH = 16; // bytes for CBC

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    private void init() {
        if (base64Key == null) {
            throw new IllegalStateException("PLAID_ENCRYPTION_KEY environment variable is not set");
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("PLAID_ENCRYPTION_KEY must decode to 32 bytes for AES‑256");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypt plain text using AES‑256‑GCM.
     * Returns Base64(IV || ciphertext).
     */
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes());
            // concatenate IV and ciphertext
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    /**
     * Decrypt a value that may be in the new GCM format or the legacy CBC format.
     * New format: Base64(IV || ciphertext) where IV is 12 bytes.
     * Legacy format: Base64(ciphertext) encrypted with AES‑CBC and a static zero
     * IV.
     */
    public String decrypt(String encrypted) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            // Heuristic: if length > IV_LENGTH we assume new format (IV + ciphertext)
            if (decoded.length > IV_LENGTH) {
                // New GCM format
                byte[] iv = new byte[IV_LENGTH];
                byte[] cipherText = new byte[decoded.length - IV_LENGTH];
                System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
                System.arraycopy(decoded, IV_LENGTH, cipherText, 0, cipherText.length);
                Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
                byte[] plain = cipher.doFinal(cipherText);
                return new String(plain);
            } else {
                // Legacy CBC format – static zero IV
                byte[] zeroIv = new byte[LEGACY_IV_LENGTH]; // all zeros
                Cipher cipher = Cipher.getInstance(LEGACY_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, new javax.crypto.spec.IvParameterSpec(zeroIv));
                byte[] plain = cipher.doFinal(decoded);
                return new String(plain);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
}
