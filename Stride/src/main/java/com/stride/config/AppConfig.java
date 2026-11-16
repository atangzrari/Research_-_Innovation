package com.stride.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Settings from stride.properties, overridable by "stride.*" system properties or
 * STRIDE_* environment variables, so secrets never need to be written into the source tree.
 */
public final class AppConfig {

    private final Properties values = new Properties();

    private AppConfig(Properties base, Map<String, String> env, Properties system) {
        values.putAll(base);
        for (String key : base.stringPropertyNames()) {
            String envKey = "STRIDE_" + key.toUpperCase(Locale.ROOT).replace('.', '_');
            if (env.containsKey(envKey)) {
                values.setProperty(key, env.get(envKey));
            }
        }
        for (String name : system.stringPropertyNames()) {
            if (name.startsWith("stride.")) {
                values.setProperty(name.substring("stride.".length()), system.getProperty(name));
            }
        }
    }

    /** Loads stride.properties from the class path and applies overrides. */
    public static AppConfig load() {
        Properties base = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream("/stride.properties")) {
            if (in != null) {
                base.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read stride.properties", e);
        }
        return new AppConfig(base, System.getenv(), System.getProperties());
    }

    /** For tests: the file's defaults with the given keys replaced. */
    public static AppConfig withOverrides(Map<String, String> overrides) {
        AppConfig config = load();
        overrides.forEach(config.values::setProperty);
        return config;
    }

    public String get(String key, String fallback) {
        String v = values.getProperty(key);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        return v.trim().replace("${user.home}", System.getProperty("user.home"));
    }

    public String get(String key) {
        return get(key, "");
    }

    public int getInt(String key, int fallback) {
        String v = get(key, null);
        if (v == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public double getDouble(String key, double fallback) {
        String v = get(key, null);
        if (v == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean getBool(String key, boolean fallback) {
        String v = get(key, null);
        return v == null ? fallback : Boolean.parseBoolean(v);
    }

    public LocalTime getTime(String key, LocalTime fallback) {
        String v = get(key, null);
        return v == null ? fallback : LocalTime.parse(v);
    }

    public List<String> getList(String key) {
        String v = get(key, null);
        if (v == null) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String part : v.split(",")) {
            if (!part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }
}
