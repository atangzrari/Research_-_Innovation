package com.stride.unit;

import com.stride.config.AppConfig;
import com.stride.security.OidcClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Single sign-on (RQ27) against a small fake identity provider. */
public class OidcClientTest {

    private HttpServer idp;
    private String nonce = "";
    private String receivedVerifier;

    @Before
    public void up() throws IOException {
        idp = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        idp.createContext("/token", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            for (String kv : body.split("&")) if (kv.startsWith("code_verifier=")) receivedVerifier = kv.substring(14);
            String claims = "{\"nonce\":\"" + nonce + "\",\"aud\":\"stride\",\"iss\":\"fake\"}";
            String jwt = "e30." + Base64.getUrlEncoder().withoutPadding().encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + ".sig";
            send(ex, "{\"access_token\":\"tok\",\"id_token\":\"" + jwt + "\",\"token_type\":\"Bearer\"}");
        });
        idp.createContext("/userinfo", ex -> {
            boolean ok = "Bearer tok".equals(ex.getRequestHeaders().getFirst("Authorization"));
            send(ex, ok ? "{\"email\":\"Student@Stride.test\",\"email_verified\":true}" : "{}");
        });
        idp.start();
    }

    private static void send(com.sun.net.httpserver.HttpExchange ex, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    @After
    public void down() {
        idp.stop(0);
    }

    private OidcClient client() {
        String base = "http://127.0.0.1:" + idp.getAddress().getPort();
        Map<String, String> o = new HashMap<>();
        o.put("sso.enabled", "true");
        o.put("sso.authorize.url", base + "/authorize");
        o.put("sso.token.url", base + "/token");
        o.put("sso.userinfo.url", base + "/userinfo");
        o.put("sso.client.id", "stride");
        o.put("sso.redirect.uri", "http://localhost:8080/sso/callback");
        return new OidcClient(AppConfig.withOverrides(o));
    }

    @Test
    public void disabledByDefault() {
        assertFalse(new OidcClient(AppConfig.load()).isEnabled());
    }

    @Test
    public void redirectCarriesPkceStateAndNonce() {
        OidcClient c = client();
        OidcClient.Pending p = c.newPending();
        String url = c.authorizeRedirect(p);
        assertTrue(url.contains("code_challenge_method=S256"));
        assertTrue(url.contains("state=" + p.state));
        assertTrue(url.contains("nonce=" + p.nonce));
        assertFalse(url.contains(p.verifier));
        assertTrue(URI.create(url).getQuery().contains("response_type=code"));
    }

    @Test
    public void completesWithVerifiedEmail() throws IOException {
        OidcClient c = client();
        OidcClient.Pending p = c.newPending();
        nonce = p.nonce;
        assertEquals("student@stride.test", c.completeSignIn(p, p.state, "abc"));
        assertEquals(p.verifier, receivedVerifier);
    }

    @Test
    public void rejectsWrongStateAndWrongNonce() {
        OidcClient c = client();
        OidcClient.Pending p = c.newPending();
        nonce = p.nonce;
        assertThrows(IOException.class, () -> c.completeSignIn(p, "forged", "abc"));
        nonce = "other";
        assertThrows(IOException.class, () -> c.completeSignIn(p, p.state, "abc"));
    }
}
