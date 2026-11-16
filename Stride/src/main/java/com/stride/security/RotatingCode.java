package com.stride.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;

/**
 * The rotating attendance code (RQ15). Like an authenticator app: the code is an HMAC of the
 * session's secret and the current time window, so it changes every few seconds and a photo of
 * the screen expires before it can be passed on. The previous window is also accepted, so a
 * student typing as the code changes is not rejected.
 */
public final class RotatingCode {

    /** No 0/O, 1/I/L: easy to read off a projector and type on a phone. */
    public static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    public static final int CODE_LENGTH = 6;
    public static final int TOKEN_LENGTH = 20;

    private RotatingCode() {
    }

    public static long window(Instant now, int seconds) {
        return Math.floorDiv(now.getEpochSecond(), seconds);
    }

    /** Seconds until the code changes. */
    public static int secondsLeft(Instant now, int seconds) {
        return seconds - Math.floorMod(now.getEpochSecond(), seconds);
    }

    /** The short code students type. */
    public static String code(String secret, long window) {
        return encode(mac(secret, "code:" + window), CODE_LENGTH);
    }

    /** The longer token carried inside the QR image. */
    public static String qrToken(String secret, long window) {
        return encode(mac(secret, "qr:" + window), TOKEN_LENGTH);
    }

    /** Upper-cases and removes spaces and dashes, so "abc 23x" and "ABC-23X" both work. */
    public static String normalise(String typed) {
        if (typed == null) {
            return "";
        }
        return typed.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
    }

    /** Accepts the code for this window or the one before. */
    public static boolean codeMatches(String secret, String typed, Instant now, int seconds) {
        String given = normalise(typed);
        if (given.length() != CODE_LENGTH) {
            return false;
        }
        long w = window(now, seconds);
        return equal(code(secret, w), given) || equal(code(secret, w - 1), given);
    }

    public static boolean tokenMatches(String secret, String token, Instant now, int seconds) {
        if (token == null || token.length() != TOKEN_LENGTH) {
            return false;
        }
        long w = window(now, seconds);
        return equal(qrToken(secret, w), token) || equal(qrToken(secret, w - 1), token);
    }

    private static boolean equal(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] mac(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    private static String encode(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt((bytes[i] & 0xFF) % ALPHABET.length()));
        }
        return sb.toString();
    }
}
