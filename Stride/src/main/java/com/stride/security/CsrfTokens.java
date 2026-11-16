package com.stride.security;

import com.stride.util.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Anti-CSRF tokens (synchroniser-token pattern): a random value kept in the session and
 * echoed back in a hidden form field on every state-changing POST.
 */
public final class CsrfTokens {

    public static final String PARAMETER = "_csrf";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfTokens() {
    }

    /** The session's token, creating it on first use. */
    public static String token(HttpSession session) {
        synchronized (session) {
            String token = (String) session.getAttribute(SessionKeys.CSRF);
            if (token == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                session.setAttribute(SessionKeys.CSRF, token);
            }
            return token;
        }
    }

    /** True only if the request has a session and its _csrf field matches that session's token. */
    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        return matches((String) session.getAttribute(SessionKeys.CSRF), request.getParameter(PARAMETER));
    }

    static boolean matches(String expected, String given) {
        if (expected == null || given == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                given.getBytes(StandardCharsets.UTF_8));
    }
}
