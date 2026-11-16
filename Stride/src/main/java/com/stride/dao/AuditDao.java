package com.stride.dao;

import com.stride.db.Database;
import com.stride.db.Jdbc;
import com.stride.model.AuditEntry;

import java.time.LocalDateTime;
import java.util.List;

/** Append-only record of security-relevant actions: sign-ins, corrections, admin changes. */
public class AuditDao extends Dao {

    public AuditDao(Database db) {
        super(db);
    }

    public void log(LocalDateTime at, Integer actorId, String action, String detail, String ip) {
        String safe = detail == null ? "" : detail.length() > 500 ? detail.substring(0, 500) : detail;
        with(c -> Jdbc.update(c, "INSERT INTO audit_log (logged_at, actor_id, action, detail, client_ip) "
                + "VALUES (?, ?, ?, ?, ?)", at, actorId, action, safe, ip));
    }

    public List<AuditEntry> recent(String action, int limit) {
        String where = action == null || action.isBlank() ? "" : "WHERE l.action = ? ";
        Object[] params = action == null || action.isBlank() ? new Object[0] : new Object[]{action};
        return with(c -> Jdbc.query(c, "SELECT l.*, CONCAT(u.first_name, ' ', u.last_name) AS actor FROM audit_log l "
                        + "LEFT JOIN users u ON u.id = l.actor_id " + where + "ORDER BY l.id DESC LIMIT " + limit,
                rs -> new AuditEntry(rs.getInt("id"), Jdbc.dateTime(rs, "logged_at"),
                        rs.getObject("actor_id") == null ? null : rs.getString("actor"), rs.getString("action"),
                        rs.getString("detail"), rs.getString("client_ip")), params));
    }

    public List<String> actions() {
        return with(c -> Jdbc.query(c, "SELECT DISTINCT action FROM audit_log ORDER BY action", rs -> rs.getString(1)));
    }
}
