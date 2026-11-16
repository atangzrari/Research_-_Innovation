package com.stride.db;

/** A database failure, unchecked so that controllers can let the error page handle it. */
public class DataException extends RuntimeException {
    public DataException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    /** True when the cause is a unique or primary-key violation (SQLSTATE class 23). */
    public boolean isDuplicate() {
        Throwable t = getCause();
        while (t != null) {
            if (t instanceof java.sql.SQLException) {
                String state = ((java.sql.SQLException) t).getSQLState();
                if (state != null && state.startsWith("23")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }
}
