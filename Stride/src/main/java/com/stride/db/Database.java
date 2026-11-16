package com.stride.db;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * A small, bounded JDBC connection pool. Calling close() on a borrowed connection returns it
 * to the pool. Works with H2 and MySQL Connector/J alike.
 */
public final class Database implements AutoCloseable {

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    private final String url;
    private final String user;
    private final String password;
    private final BlockingQueue<Connection> idle;
    private final Semaphore permits;
    private volatile boolean closed;

    public Database(String url, String user, String password, int size) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.idle = new ArrayBlockingQueue<>(size);
        this.permits = new Semaphore(size, true);
        loadDriver(url);
    }

    private static void loadDriver(String url) {
        String driver = url.startsWith("jdbc:mysql:") ? "com.mysql.cj.jdbc.Driver"
                : url.startsWith("jdbc:h2:") ? "org.h2.Driver" : null;
        if (driver != null) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("JDBC driver not on the class path: " + driver, e);
            }
        }
    }

    public boolean isMySql() {
        return url.startsWith("jdbc:mysql:");
    }

    /** Borrows a connection; close it (try-with-resources) to give it back. */
    public Connection get() throws SQLException {
        if (closed) {
            throw new SQLException("Database is closed");
        }
        try {
            if (!permits.tryAcquire(10, TimeUnit.SECONDS)) {
                throw new SQLException("No database connection free after 10 seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a connection", e);
        }
        try {
            Connection raw = idle.poll();
            if (raw == null || !raw.isValid(2)) {
                quietClose(raw);
                raw = DriverManager.getConnection(url, user, password);
            }
            raw.setAutoCommit(true);
            return wrap(raw);
        } catch (SQLException | RuntimeException e) {
            permits.release();
            throw e;
        }
    }

    private Connection wrap(Connection raw) {
        boolean[] returned = {false};
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        if (!returned[0]) {
                            returned[0] = true;
                            try {
                                if (!raw.getAutoCommit()) {
                                    raw.rollback();
                                }
                            } catch (SQLException ignored) {
                                quietClose(raw);
                                permits.release();
                                return null;
                            }
                            if (closed || !idle.offer(raw)) {
                                quietClose(raw);
                            }
                            permits.release();
                        }
                        return null;
                    }
                    if ("isClosed".equals(method.getName())) {
                        return returned[0] || raw.isClosed();
                    }
                    if (returned[0]) {
                        throw new SQLException("Connection already returned to the pool");
                    }
                    try {
                        return method.invoke(raw, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    /** Runs work in one transaction: commit on success, roll back on any failure. */
    public <T> T inTransaction(Work<T> work) throws SQLException {
        try (Connection c = get()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private static void quietClose(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // nothing more to do
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        Connection c;
        while ((c = idle.poll()) != null) {
            quietClose(c);
        }
    }
}
