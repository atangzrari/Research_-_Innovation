package com.stride.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Salted PBKDF2-HMAC-SHA256 password hashing using only the JDK.
 * Stored format: pbkdf2$iterations$salt$hash (salt and hash Base64). Because the count is stored
 * with each hash, raising it later does not invalidate older hashes.
 */
public final class PasswordHasher {

    public static final int DEFAULT_ITERATIONS = 600_000;   // OWASP guidance for PBKDF2-HMAC-SHA256
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile int iterations = DEFAULT_ITERATIONS;

    private PasswordHasher() {
    }

    /** Sets the round count for new hashes (from security.pbkdf2.iterations). */
    public static void setIterations(int rounds) {
        if (rounds < 1) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        iterations = rounds;
    }

    public static int getIterations() {
        return iterations;
    }

    public static String hash(String password) {
        return hash(password, iterations);
    }

    public static String hash(String password, int rounds) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = derive(password, salt, rounds);
        return "pbkdf2$" + rounds + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(key);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        try {
            int rounds = Integer.parseInt(parts[1]);
            if (rounds < 1) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, salt, rounds);
            return MessageDigest.isEqual(expected, actual);   // constant-time
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** True if the stored hash used fewer rounds than the current setting (re-hash at next sign-in). */
    public static boolean needsRehash(String stored) {
        if (stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        try {
            return parts.length == 4 && Integer.parseInt(parts[1]) < iterations;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int rounds) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available", e);
        }
    }
}
