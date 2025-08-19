package com.example.optimaai;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5PADDING";
    private static final String SECRET_KEY_BASE = "YourSuperSecretKeyForOptimaAI123";

    private static SecretKeySpec secretKeySpec;

    static {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(SECRET_KEY_BASE.getBytes(StandardCharsets.UTF_8));
            key = Arrays.copyOf(key, 16);
            secretKeySpec = new SecretKeySpec(key, ALGORITHM);
        } catch (Exception e) {
            Log.e("EncryptionHelper", "Failed to initialize SecretKeySpec", e);
        }
    }

    public static String encrypt(String value) {
        if (secretKeySpec == null) {
            Log.e("EncryptionHelper", "Encryption failed: SecretKeySpec not initialized.");
            return null;
        }
        try {
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] encryptedValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encryptedValue.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedValue, 0, combined, iv.length, encryptedValue.length);

            return Base64.encodeToString(combined, Base64.DEFAULT);

        } catch (Exception ex) {
            Log.e("EncryptionHelper", "Encryption failed", ex);
            return null;
        }
    }

    public static String decrypt(String encrypted) {
        if (secretKeySpec == null) {
            Log.e("EncryptionHelper", "Decryption failed: SecretKeySpec not initialized.");
            return null;
        }
        try {
            byte[] combined = Base64.decode(encrypted, Base64.DEFAULT);
            byte[] iv = Arrays.copyOfRange(combined, 0, 16);
            byte[] encryptedValue = Arrays.copyOfRange(combined, 16, combined.length);

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] decryptedValue = cipher.doFinal(encryptedValue);
            return new String(decryptedValue, StandardCharsets.UTF_8);

        } catch (Exception ex) {
            Log.e("EncryptionHelper", "Decryption Failed", ex);
            return null;
        }
    }
}