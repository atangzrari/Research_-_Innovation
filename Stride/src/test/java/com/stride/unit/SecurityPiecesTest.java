package com.stride.unit;

import com.stride.config.AppConfig;
import com.stride.report.Report;
import com.stride.security.NetworkMatcher;
import com.stride.security.RotatingCode;
import com.stride.security.SignInGuard;
import com.stride.service.AccountService;
import com.stride.service.CampusLocation;
import com.stride.service.ConfidenceScorer;
import com.stride.util.Json;
import org.junit.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SecurityPiecesTest {

    private static final Instant T = Instant.parse("2026-10-29T13:27:10Z");

    @Test
    public void codeIsSixCharactersFromTheReadableAlphabet() {
        String c = RotatingCode.code("secret", 123);
        assertEquals(6, c.length());
        for (char ch : c.toCharArray()) assertTrue(RotatingCode.ALPHABET.indexOf(ch) >= 0);
    }

    @Test
    public void codeChangesEveryWindowAndPreviousIsAccepted() {
        long w = RotatingCode.window(T, 30);
        assertNotEquals(RotatingCode.code("s", w), RotatingCode.code("s", w + 1));
        assertTrue(RotatingCode.codeMatches("s", RotatingCode.code("s", w), T, 30));
        assertTrue(RotatingCode.codeMatches("s", RotatingCode.code("s", w - 1), T, 30));
        assertFalse(RotatingCode.codeMatches("s", RotatingCode.code("s", w - 2), T, 30));
        assertFalse(RotatingCode.codeMatches("other", RotatingCode.code("s", w), T, 30));
    }

    @Test
    public void secondsLeftCountsDown() {
        assertEquals(20, RotatingCode.secondsLeft(T, 30));
    }

    @Test
    public void networkRanges() {
        NetworkMatcher m = new NetworkMatcher(List.of("10.0.0.0/8", "192.168.1.0/24", "::1/128", "172.16.0.0/12"));
        assertTrue(m.contains("10.20.4.11"));
        assertTrue(m.contains("192.168.1.200"));
        assertFalse(m.contains("192.168.2.1"));
        assertTrue(m.contains("172.31.255.1"));
        assertFalse(m.contains("172.32.0.1"));
        assertTrue(m.contains("::1"));
        assertTrue(m.contains("::ffff:10.1.2.3"));
        assertFalse(m.contains("41.75.3.9"));
        assertFalse(m.contains("not-an-ip"));
        assertFalse(m.contains(null));
    }

    @Test
    public void confidenceScores() {
        ConfidenceScorer s = new ConfidenceScorer(true, 60);
        assertEquals(90, s.score(ConfidenceScorer.Token.QR, true, false, ConfidenceScorer.Location.NOT_USED).getValue());
        assertEquals(80, s.score(ConfidenceScorer.Token.CODE, true, false, ConfidenceScorer.Location.NOT_USED).getValue());
        ConfidenceScorer.Score away = s.score(ConfidenceScorer.Token.CODE, false, false, ConfidenceScorer.Location.NOT_USED);
        assertEquals(45, away.getValue());
        assertTrue(away.isFlagged());
        assertTrue(s.score(ConfidenceScorer.Token.QR, true, true, ConfidenceScorer.Location.NOT_USED).isFlagged());
        assertEquals(100, s.score(ConfidenceScorer.Token.QR, true, false, ConfidenceScorer.Location.INSIDE).getValue());
    }

    @Test
    public void locationFactor() {
        CampusLocation off = new CampusLocation(null, null, 400);
        assertEquals(ConfidenceScorer.Location.NOT_USED, off.classify("1", "1", "5"));
        CampusLocation on = new CampusLocation(-24.65, 25.93, 400);
        assertEquals(ConfidenceScorer.Location.INSIDE, on.classify("-24.651", "25.931", "20"));
        assertEquals(ConfidenceScorer.Location.OUTSIDE, on.classify("-24.70", "25.93", "20"));
        assertEquals(ConfidenceScorer.Location.NOT_USED, on.classify("x", "y", ""));
    }

    @Test
    public void signInGuardLocksByAddressAcrossManyEmails() {
        Clock c = Clock.fixed(T, ZoneOffset.UTC);
        SignInGuard g = new SignInGuard(5, 20, 300, Duration.ofMinutes(5), c);
        for (int i = 0; i < 20; i++) g.failure("user" + i + "@x", "9.9.9.9");
        assertFalse(g.check("fresh@x", "9.9.9.9").isZero());
        assertTrue(g.check("fresh@x", "8.8.8.8").isZero());
    }

    @Test
    public void signInGuardGlobalLimit() {
        SignInGuard g = new SignInGuard(5, 20, 3, Duration.ofMinutes(5), Clock.fixed(T, ZoneOffset.UTC));
        assertTrue(g.check("a", "1").isZero());
        assertTrue(g.check("b", "2").isZero());
        assertTrue(g.check("c", "3").isZero());
        assertFalse(g.check("d", "4").isZero());
    }

    @Test
    public void passwordPolicy() {
        assertTrue(AccountService.passwordProblem("short", "a@b.c") != null);
        assertTrue(AccountService.passwordProblem("password123", "a@b.c") != null);
        assertTrue(AccountService.passwordProblem("thabo.long.pass", "thabo@x.bw") != null);
        assertNull(AccountService.passwordProblem("blue kettle on friday", "thabo@x.bw"));
    }

    @Test
    public void csvQuotesAndDefusesFormulas() {
        Report r = new Report("x", "x", "x", List.of("a", "b"));
        r.row("=HYPERLINK(\"evil\")", "plain, with comma");
        r.row("-12", "88%");
        String csv = r.toCsv();
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"evil\"\")\""));
        assertTrue(csv.contains("\"plain, with comma\""));
        assertTrue(csv.contains("-12,88%"));
    }

    @Test
    public void jsonRoundTrip() {
        Map<String, Object> m = Json.parseObject("{\"a\":\"x\\\"y\",\"n\":3,\"b\":true,\"l\":[1,\"z\"],\"o\":{\"k\":null}}");
        assertEquals("x\"y", m.get("a"));
        assertEquals(3L, m.get("n"));
        assertEquals(Boolean.TRUE, m.get("b"));
        assertTrue(Json.write(Map.of("code", "<a>")).contains("\\u003ca\\u003e"));
    }

    @Test
    public void configOverrides() {
        AppConfig c = AppConfig.withOverrides(Map.of("attendance.code.seconds", "15"));
        assertEquals(15, c.getInt("attendance.code.seconds", 30));
        assertEquals("Africa/Gaborone", c.get("campus.zone"));
        assertEquals(600000, c.getInt("security.pbkdf2.iterations", 0));
    }
}
