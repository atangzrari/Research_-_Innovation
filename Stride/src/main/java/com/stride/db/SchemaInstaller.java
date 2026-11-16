package com.stride.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates the tables from db/schema.sql when the database is new. */
public final class SchemaInstaller {

    private SchemaInstaller() {
    }

    /** True if the users table already exists (a probe query; portable across databases). */
    public static boolean isInstalled(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeQuery("SELECT COUNT(*) FROM users WHERE 1 = 0").close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static void install(Connection c) throws SQLException {
        String script;
        try (InputStream in = SchemaInstaller.class.getResourceAsStream("/db/schema.sql")) {
            if (in == null) {
                throw new SQLException("db/schema.sql is missing from the class path");
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Cannot read db/schema.sql", e);
        }
        StringBuilder current = new StringBuilder();
        try (Statement st = c.createStatement()) {
            for (String line : script.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    String sql = current.toString().trim();
                    st.execute(sql.substring(0, sql.length() - 1));
                    current.setLength(0);
                }
            }
        }
    }
}
