package com.stride;

import com.stride.config.App;
import com.stride.config.AppConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Builds a Stride app on a private in-memory database with sample data, for tests. */
public final class TestApp {

    /** The moment the sample data and the report screenshots use: Thursday 29 October 2026, 15:27. */
    public static final String THURSDAY_1527 = "2026-10-29T15:27";

    private TestApp() {
    }

    public static Map<String, String> overrides(String clock) {
        Map<String, String> o = new HashMap<>();
        o.put("db.url", "jdbc:h2:mem:t" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1");
        o.put("db.user", "sa");
        o.put("db.password", "");
        o.put("db.seed", "true");
        // Fewer rounds keep the test run fast; production uses 600,000 (stride.properties).
        o.put("security.pbkdf2.iterations", "1000");
        o.put("clock.fixed", clock);
        return o;
    }

    public static App start() {
        return start(THURSDAY_1527);
    }

    public static App start(String clock) {
        return new App(AppConfig.withOverrides(overrides(clock))).start();
    }

    public static App start(Map<String, String> extra) {
        Map<String, String> o = overrides(THURSDAY_1527);
        o.putAll(extra);
        return new App(AppConfig.withOverrides(o)).start();
    }
}
