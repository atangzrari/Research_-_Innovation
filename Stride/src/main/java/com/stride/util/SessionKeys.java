package com.stride.util;

/** Fixed names for the attributes Stride keeps in the HTTP session. */
public final class SessionKeys {

    /** The signed-in {@link com.stride.model.User}. */
    public static final String USER = "strideUser";

    /** The anti-CSRF token for this session. */
    public static final String CSRF = "strideCsrfToken";

    /** Messages to show once on the next page (after a redirect). */
    public static final String FLASH = "strideFlash";

    /** Pending single sign-on values (state, nonce, PKCE verifier). */
    public static final String SSO = "strideSso";

    /** The study assistant conversation. */
    public static final String CHAT = "strideChat";

    private SessionKeys() {
    }
}
