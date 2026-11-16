package com.stride.web;

import com.stride.DevServer;
import com.stride.TestApp;
import com.stride.config.App;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Servlet-level tests (defect D8): the real filters, controllers and JSP views on an embedded
 * Tomcat 11, driven over HTTP the way a browser would.
 */
public class WebIntegrationTest {

    private static DevServer server;
    private static String base;
    private static App app;

    @BeforeClass
    public static void start() throws Exception {
        TestApp.overrides(TestApp.THURSDAY_1527).forEach((k, v) -> System.setProperty("stride." + k, v));
        server = new DevServer().start(0);
        base = "http://localhost:" + server.port();
        app = (App) server.context().getServletContext().getAttribute(App.CONTEXT_KEY);
    }

    @AfterClass
    public static void stop() throws Exception {
        server.stop();
        for (String k : TestApp.overrides(TestApp.THURSDAY_1527).keySet()) System.clearProperty("stride." + k);
    }

    /** A browser: keeps cookies, does not follow redirects. */
    static final class Browser {
        final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        String csrf;

        HttpResponse<String> get(String path) throws IOException, InterruptedException {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            Matcher m = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(r.body());
            if (m.find()) csrf = m.group(1);
            return r;
        }

        HttpResponse<String> post(String path, Map<String, String> form) throws IOException, InterruptedException {
            StringJoiner j = new StringJoiner("&");
            form.forEach((k, v) -> j.add(URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)));
            return http.send(HttpRequest.newBuilder(URI.create(base + path))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(j.toString())).build(), HttpResponse.BodyHandlers.ofString());
        }

        Browser signIn(String email, String password) throws Exception {
            get("/login");
            HttpResponse<String> r = post("/login", Map.of("_csrf", csrf, "email", email, "password", password));
            assertEquals(r.body(), 302, r.statusCode());
            return this;
        }
    }

    @Test
    public void signInPageHasSecurityHeaders() throws Exception {
        HttpResponse<String> r = new Browser().get("/login");
        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Security-Policy").orElse("").contains("frame-ancestors 'none'"));
        assertEquals("nosniff", r.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals("DENY", r.headers().firstValue("X-Frame-Options").orElse(""));
        assertTrue(r.body().contains("Every college site, one sign-in away."));
        assertFalse("demo passwords must not be shown", r.body().contains("student123"));
    }

    @Test
    public void protectedPagesRedirectToSignIn() throws Exception {
        for (String p : List.of("/dashboard", "/admin/users", "/lecturer/sessions", "/report?id=rp01", "/")) {
            HttpResponse<String> r = new Browser().get(p);
            assertEquals(p, 302, r.statusCode());
            assertTrue(p, r.headers().firstValue("Location").orElse("").endsWith("/login"));
        }
    }

    @Test
    public void wrongPasswordGivesOneMessage() throws Exception {
        Browser b = new Browser();
        b.get("/login");
        HttpResponse<String> r = b.post("/login", Map.of("_csrf", b.csrf, "email", "student@stride.test", "password", "bad"));
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("That email and password don"));
        b.get("/login");
        HttpResponse<String> r2 = b.post("/login", Map.of("_csrf", b.csrf, "email", "ghost@stride.test", "password", "bad"));
        assertTrue(r2.body().contains("That email and password don"));
    }

    @Test
    public void postWithoutCsrfTokenIsForbidden() throws Exception {
        Browser b = new Browser().signIn("student@stride.test", "student123");
        assertEquals(403, b.post("/logout", Map.of()).statusCode());
        assertEquals(403, b.post("/checkin", Map.of("code", "ABCDEF")).statusCode());
        assertEquals(403, b.post("/login", Map.of("email", "x", "password", "y")).statusCode());
    }

    @Test
    public void rolesAreEnforcedOnEveryRequest() throws Exception {
        Browser s = new Browser().signIn("student@stride.test", "student123");
        for (String p : List.of("/admin/users", "/lecturer/sessions", "/reports", "/report?id=rp01", "/notices/new")) {
            assertEquals(p, 403, s.get(p).statusCode());
        }
        Browser l = new Browser().signIn("lecturer@stride.test", "lecturer123");
        assertEquals(403, l.get("/admin/users").statusCode());
        assertEquals(403, l.get("/checkin").statusCode());
        assertEquals(200, l.get("/reports").statusCode());
        Browser a = new Browser().signIn("admin@stride.test", "admin123");
        assertEquals(403, a.get("/lecturer/sessions").statusCode());
        assertEquals(200, a.get("/admin/audit").statusCode());
    }

    @Test
    public void everyPageRendersForItsRole() throws Exception {
        Map<String, List<String>> pages = Map.of(
                "student@stride.test/student123", List.of("/dashboard", "/classes", "/timetable", "/attendance", "/notices",
                        "/sites", "/sites?q=books", "/help", "/map?room=Lab+2", "/modules", "/module?code=CSE305", "/account",
                        "/checkin", "/planner", "/assistant"),
                "lecturer@stride.test/lecturer123", List.of("/dashboard", "/lecturer/sessions", "/timetable", "/module?code=CSE302",
                        "/notices/new", "/reports", "/report?id=rp01", "/report?id=rp02", "/report?id=rp03", "/report?id=rp04",
                        "/report?id=rp05"),
                "admin@stride.test/admin123", List.of("/dashboard", "/admin/users", "/admin/user?id=1", "/admin/modules",
                        "/admin/module?id=1", "/admin/timetable", "/admin/rooms", "/admin/sites", "/admin/audit", "/notices/new"));
        for (Map.Entry<String, List<String>> e : pages.entrySet()) {
            String[] who = e.getKey().split("/");
            Browser b = new Browser().signIn(who[0], who[1]);
            for (String p : e.getValue()) {
                HttpResponse<String> r = b.get(p);
                assertEquals(who[0] + " " + p, 200, r.statusCode());
                assertTrue(p, r.body().contains("</html>"));
            }
        }
    }

    @Test
    public void homePageWordsAreSpaced() throws Exception {
        String html = new Browser().signIn("student@stride.test", "student123").get("/dashboard").body();
        assertTrue("defect D1", html.contains("You have 3 classes today."));
        assertTrue("defect D1", html.contains("On now: "));
    }

    @Test
    public void lecturerOpensSessionAndStudentChecksInWithTheLiveCode() throws Exception {
        Browser lect = new Browser().signIn("lecturer@stride.test", "lecturer123");
        lect.get("/lecturer/sessions");
        int entry = app.schedule.classesToday(app.users.findByEmail("lecturer@stride.test").get(), app.now()).stream()
                .filter(c -> c.getModuleCode().equals("CSE302")).findFirst().get().getEntryId();
        HttpResponse<String> opened = lect.post("/lecturer/open", Map.of("_csrf", lect.csrf, "entry", String.valueOf(entry)));
        assertEquals(302, opened.statusCode());
        String sessionPath = opened.headers().firstValue("Location").get().replace(base, "");
        String id = sessionPath.substring(sessionPath.indexOf("id=") + 3);

        String live = lect.get("/lecturer/live?id=" + id).body();
        Matcher m = Pattern.compile("\"code\":\"([A-Z0-9]{6})\"").matcher(live);
        assertTrue(live, m.find());
        HttpResponse<String> qr = lect.get("/lecturer/qr?id=" + id);
        assertTrue(qr.headers().firstValue("Content-Type").orElse("").startsWith("image/svg+xml"));

        Browser stu = new Browser().signIn("student@stride.test", "student123");
        assertTrue(stu.get("/dashboard").body().contains("is taking attendance now"));
        stu.get("/checkin");
        HttpResponse<String> done = stu.post("/checkin", Map.of("_csrf", stu.csrf, "code", m.group(1)));
        assertEquals(done.body(), 200, done.statusCode());
        assertTrue(done.body().contains("checked in, but marked late"));

        HttpResponse<String> again = stu.post("/checkin", Map.of("_csrf", stu.csrf, "code", m.group(1)));
        assertEquals(400, again.statusCode());

        String roster = lect.get(sessionPath).body();
        Matcher sum = Pattern.compile("id=\"live-summary\"[^>]*>([^<]*)").matcher(roster);
        assertTrue(roster.length() + "", sum.find());
        assertTrue(sum.group(1), sum.group(1).contains("1 of 24 checked in"));
        HttpResponse<String> csv = lect.get("/lecturer/roster.csv?id=" + id);
        assertTrue(csv.body().startsWith("Student,Student number,Status"));
        assertTrue(csv.body().contains("Demo Student,CSE24-900,Late"));
    }

    @Test
    public void reportDownloadsAsCsv() throws Exception {
        HttpResponse<String> r = new Browser().signIn("admin@stride.test", "admin123").get("/report?id=rp01&format=csv");
        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Disposition").orElse("").contains("stride-rp01"));
        assertTrue(r.body().contains("CSE305"));
    }

    @Test
    public void assistantAnswersOverHttp() throws Exception {
        Browser b = new Browser().signIn("student@stride.test", "student123");
        b.get("/assistant");
        assertEquals(302, b.post("/assistant", Map.of("_csrf", b.csrf, "q", "What is my attendance in CSE305?")).statusCode());
        assertTrue(b.get("/assistant").body().contains("75%"));
    }

    @Test
    public void adminResetForcesPasswordChange() throws Exception {
        Browser a = new Browser().signIn("admin@stride.test", "admin123");
        int id = app.users.findByEmail("masego.rapula@stride.test").get().getUserId();
        a.get("/admin/user?id=" + id);
        a.post("/admin/users/reset", Map.of("_csrf", a.csrf, "id", String.valueOf(id)));
        String page = a.get("/admin/user?id=" + id).body();
        Matcher m = Pattern.compile("Temporary password \\(shown once\\): ([A-Za-z0-9-]+)").matcher(page);
        assertTrue(m.find());

        Browser s = new Browser();
        s.get("/login");
        HttpResponse<String> in = s.post("/login", Map.of("_csrf", s.csrf, "email", "masego.rapula@stride.test", "password", m.group(1)));
        assertTrue(in.headers().firstValue("Location").orElse("").endsWith("/account/password"));
        assertTrue(s.get("/dashboard").headers().firstValue("Location").orElse("").endsWith("/account/password"));
        s.get("/account/password");
        s.post("/account/password", Map.of("_csrf", s.csrf, "current", m.group(1), "next", "green tea at noon", "repeat", "green tea at noon"));
        assertEquals(200, s.get("/dashboard").statusCode());
    }

    @Test
    public void signOutEndsSession() throws Exception {
        Browser b = new Browser().signIn("student@stride.test", "student123");
        b.get("/dashboard");
        assertEquals(302, b.post("/logout", Map.of("_csrf", b.csrf)).statusCode());
        assertEquals(302, b.get("/dashboard").statusCode());
    }

    @Test
    public void unknownPageShowsFriendlyError() throws Exception {
        HttpResponse<String> r = new Browser().signIn("student@stride.test", "student123").get("/no-such-page");
        assertEquals(404, r.statusCode());
        assertTrue(r.body().contains("doesn’t exist"));
        assertFalse(r.body().contains("Exception"));
        assertNotNull(r);
    }
}
