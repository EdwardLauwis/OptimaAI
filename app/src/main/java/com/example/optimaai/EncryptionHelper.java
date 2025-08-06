package com.example.optimaai;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper {
    private static final String SECRET_KEY_BASE = "YourSuperSecretKeyForOptimaAI123"; // Base key
    private static final String INIT_VECTOR_BASE = "YourInitVector16"; // Base IV

    // Generate 16-byte key and IV using SHA-256 hashing
    private static byte[] getKey() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(SECRET_KEY_BASE.getBytes(StandardCharsets.UTF_8));
            return java.util.Arrays.copyOf(key, 16); // Use first 16 bytes for AES-128
        } catch (Exception e) {
            Log.e("EncryptionHelper", "Key generation failed: " + e.getMessage());
            return null;
        }
    }

    private static byte[] getIv() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] iv = sha.digest(INIT_VECTOR_BASE.getBytes(StandardCharsets.UTF_8));
            return java.util.Arrays.copyOf(iv, 16); // Use first 16 bytes
        } catch (Exception e) {
            Log.e("EncryptionHelper", "IV generation failed: " + e.getMessage());
            return null;
        }
    }

    public static String encrypt(String value) {
        try {
            byte[] key = getKey();
            byte[] iv = getIv();
            if (key == null || iv == null) return null;

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception ex) {
            Log.e("EncryptionHelper", "Encryption failed: " + ex.getMessage());
            return null;
        }
    }

    public static String decrypt(String encrypted) {
        try {
            byte[] key = getKey();
            byte[] iv = getIv();
            if (key == null || iv == null) return null;

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);

            byte[] original = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT));
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Log.e("EncryptionHelper", "Decryption failed: " + ex.getMessage());
            return null;
        }
    }
}