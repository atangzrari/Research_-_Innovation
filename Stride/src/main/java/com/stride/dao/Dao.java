package com.stride.dao;

import com.stride.db.DataException;
import com.stride.db.Database;

import java.sql.Connection;
import java.sql.SQLException;

/** Base for the data-access classes: borrows a pooled connection for each call. */
abstract class Dao {

    protected final Database db;

    protected Dao(Database db) {
        this.db = db;
    }

    protected <T> T with(Database.Work<T> work) {
        try (Connection c = db.get()) {
            return work.run(c);
        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    protected <T> T tx(Database.Work<T> work) {
        try {
            return db.inTransaction(work);
        } catch (SQLException e) {
            throw new DataException(e);
        }
    }

    /** Builds "?, ?, ?" for an IN list of the given size (at least one element). */
    protected static String marks(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(i == 0 ? "?" : ", ?");
        }
        return sb.toString();
    }
}
