package com.stride.security;

import com.stride.config.AppConfig;
import com.stride.util.Json;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Single sign-on with the college identity provider (RQ27): the OpenID Connect authorisation-code
 * flow with PKCE (RFC 7636), as recommended for OAuth 2.0 clients (RFC 8252). Stride never sees the
 * student's college password; it receives a verified e-mail address and matches it to an existing,
 * active Stride account. Off unless sso.enabled=true.
 */
public final class OidcClient {

    /** The values kept in the session between the redirect and the callback. */
    public static final class Pending implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public final String state;
        public final String nonce;
        public final String verifier;

        Pending(String state, String nonce, String verifier) {
            this.state = state;
            this.nonce = nonce;
            this.verifier = verifier;
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final boolean enabled;
    private final String label;
    private final String authorizeUrl;
    private final String tokenUrl;
    private final String userinfoUrl;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;
    private final HttpClient http;

    public OidcClient(AppConfig config) {
        this.authorizeUrl = config.get("sso.authorize.url");
        this.tokenUrl = config.get("sso.token.url");
        this.userinfoUrl = config.get("sso.userinfo.url");
        this.clientId = config.get("sso.client.id");
        this.clientSecret = config.get("sso.client.secret");
        this.redirectUri = config.get("sso.redirect.uri");
        this.scope = config.get("sso.scope", "openid email profile");
        this.label = config.get("sso.button.label", "Sign in with your college account");
        this.enabled = config.getBool("sso.enabled", false) && !authorizeUrl.isEmpty() && !tokenUrl.isEmpty()
                && !userinfoUrl.isEmpty() && !clientId.isEmpty();
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getLabel() {
        return label;
    }

    public Pending newPending() {
        return new Pending(random(24), random(24), random(48));
    }

    /** Where to send the browser to sign in. */
    public String authorizeRedirect(Pending p) {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("response_type", "code");
        q.put("client_id", clientId);
        q.put("redirect_uri", redirectUri);
        q.put("scope", scope);
        q.put("state", p.state);
        q.put("nonce", p.nonce);
        q.put("code_challenge", challenge(p.verifier));
        q.put("code_challenge_method", "S256");
        return authorizeUrl + (authorizeUrl.contains("?") ? "&" : "?") + form(q);
    }

    /**
     * Swaps the code for tokens, checks the ID token's nonce and audience, then asks the
     * userinfo endpoint for the e-mail address. Returns the verified, lower-case e-mail.
     */
    public String completeSignIn(Pending p, String state, String code) throws IOException {
        if (p == null || state == null || !MessageDigest.isEqual(p.state.getBytes(StandardCharsets.UTF_8),
                state.getBytes(StandardCharsets.UTF_8))) {
            throw new IOException("The sign-in response did not match this browser (state mismatch).");
        }
        if (code == null || code.isBlank()) {
            throw new IOException("The identity provider did not return a code.");
        }
        Map<String, String> body = new LinkedHashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("redirect_uri", redirectUri);
        body.put("client_id", clientId);
        body.put("code_verifier", p.verifier);
        if (!clientSecret.isEmpty()) {
            body.put("client_secret", clientSecret);
        }
        Map<String, Object> tokens = send(HttpRequest.newBuilder(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form(body))).build());
        Object access = tokens.get("access_token");
        if (!(access instanceof String)) {
            throw new IOException("The identity provider did not return an access token.");
        }
        Object idToken = tokens.get("id_token");
        if (idToken instanceof String) {
            // The ID token came straight from the token endpoint over TLS, so its signature check
            // may rely on TLS (OIDC Core 3.1.3.7). Its claims are still checked.
            Map<String, Object> claims = claims((String) idToken);
            if (!p.nonce.equals(claims.get("nonce"))) {
                throw new IOException("The ID token's nonce does not match.");
            }
            Object aud = claims.get("aud");
            boolean audOk = clientId.equals(aud) || (aud instanceof java.util.List && ((java.util.List<?>) aud).contains(clientId));
            if (!audOk) {
                throw new IOException("The ID token was issued for another application.");
            }
        }
        Map<String, Object> info = send(HttpRequest.newBuilder(URI.create(userinfoUrl))
                .header("Authorization", "Bearer " + access).header("Accept", "application/json").GET().build());
        Object email = info.get("email");
        if (email == null) {
            email = info.get("preferred_username");
        }
        Object verified = info.get("email_verified");
        if (!(email instanceof String) || Boolean.FALSE.equals(verified)) {
            throw new IOException("The identity provider did not give a verified e-mail address.");
        }
        return ((String) email).trim().toLowerCase(java.util.Locale.ROOT);
    }

    private Map<String, Object> send(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> r = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) {
                throw new IOException("Identity provider answered HTTP " + r.statusCode());
            }
            return Json.parseObject(r.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        } catch (IllegalArgumentException e) {
            throw new IOException("Identity provider sent JSON Stride could not read", e);
        }
    }

    static Map<String, Object> claims(String jwt) throws IOException {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            throw new IOException("Malformed ID token");
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return Json.parseObject(payload);
    }

    static String challenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String random(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String form(Map<String, String> values) {
        StringJoiner j = new StringJoiner("&");
        values.forEach((k, v) -> j.add(URLEncoder.encode(k, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(v, StandardCharsets.UTF_8)));
        return j.toString();
    }
}
