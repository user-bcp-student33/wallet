package com.bcp.wallet.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

import com.bcp.wallet.backend.config.WalletProperties;

@Component
public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FORMAT_ID = "pbkdf2_sha256";
    private static final String FIELD_SEPARATOR = "$";
    private static final String FIELD_SEPARATOR_REGEX = "\\$";
    private static final int STORED_FIELD_COUNT = 4;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int BITS_PER_BYTE = 8;

    private final SecureRandom random = new SecureRandom();
    private final int iterations;

    public PasswordHasher(WalletProperties properties) {
        this.iterations = properties.security().pbkdf2Iterations();
    }

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(password, salt, iterations, HASH_BITS);
        return String.join(FIELD_SEPARATOR, FORMAT_ID, String.valueOf(iterations), encode(salt), encode(hash));
    }

    public boolean matches(String password, String stored) {
        String[] fields = stored.split(FIELD_SEPARATOR_REGEX);
        if (fields.length != STORED_FIELD_COUNT || !FORMAT_ID.equals(fields[0])) {
            return false;
        }
        try {
            byte[] expected = Base64.getDecoder().decode(fields[3]);
            byte[] actual = derive(password, Base64.getDecoder().decode(fields[2]),
                    Integer.parseInt(fields[1]), expected.length * BITS_PER_BYTE);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException malformedStoredHash) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations, int bits) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, bits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String encode(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes), StandardCharsets.US_ASCII);
    }
}
