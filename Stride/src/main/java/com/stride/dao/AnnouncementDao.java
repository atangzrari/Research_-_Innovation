package com.stride.dao;

import com.stride.db.Database;
import com.stride.db.Jdbc;
import com.stride.model.Announcement;
import com.stride.model.Role;
import com.stride.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Notices (RQ10, RQ19) and which of them each person has read. */
public class AnnouncementDao extends Dao {

    private static final String SELECT = "SELECT a.id, a.title, a.body, a.pinned, a.posted_at, a.module_id, "
            + "a.author_id, m.code AS module_code, CONCAT(u.first_name, ' ', u.last_name) AS author_name, "
            + "u.role AS author_role, CASE WHEN r.user_id IS NULL THEN FALSE ELSE TRUE END AS is_read "
            + "FROM announcements a JOIN users u ON u.id = a.author_id LEFT JOIN modules m ON m.id = a.module_id "
            + "LEFT JOIN announcement_reads r ON r.announcement_id = a.id AND r.user_id = ? ";

    public AnnouncementDao(Database db) {
        super(db);
    }

    static Announcement map(ResultSet rs) throws SQLException {
        return new Announcement(rs.getInt("id"), rs.getString("title"), rs.getString("body"),
                rs.getString("author_name"), Role.valueOf(rs.getString("author_role")).getLabel(),
                rs.getString("module_code"), rs.getBoolean("pinned"), Jdbc.dateTime(rs, "posted_at"),
                rs.getBoolean("is_read"));
    }

    /** The audience rule: who may see which notices. */
    private static String visibleTo(User user) {
        switch (user.getRole()) {
            case STUDENT:
                return "WHERE (a.module_id IS NULL OR a.module_id IN (SELECT module_id FROM enrolments WHERE student_id = ?)) ";
            case LECTURER:
                return "WHERE (a.module_id IS NULL OR a.author_id = ? OR a.module_id IN "
                        + "(SELECT id FROM modules WHERE lecturer_id = ?)) ";
            default:
                return "WHERE 1 = 1 ";
        }
    }

    private static Object[] params(User user, Object... extra) {
        java.util.List<Object> p = new java.util.ArrayList<>();
        p.add(user.getUserId());
        if (user.getRole() == Role.STUDENT) {
            p.add(user.getUserId());
        } else if (user.getRole() == Role.LECTURER) {
            p.add(user.getUserId());
            p.add(user.getUserId());
        }
        java.util.Collections.addAll(p, extra);
        return p.toArray();
    }

    /** Notices this person may see: pinned first, then newest first. */
    public List<Announcement> feedFor(User user, int limit) {
        return with(c -> Jdbc.query(c, SELECT + visibleTo(user) + "ORDER BY a.pinned DESC, a.posted_at DESC LIMIT "
                + limit, AnnouncementDao::map, params(user)));
    }

    public List<Announcement> forModule(User user, int moduleId) {
        return with(c -> Jdbc.query(c, SELECT + visibleTo(user) + "AND a.module_id = ? ORDER BY a.posted_at DESC",
                AnnouncementDao::map, params(user, moduleId)));
    }

    public int unreadCount(User user) {
        return with(c -> Jdbc.count(c, "SELECT COUNT(*) FROM (" + SELECT + visibleTo(user) + ") x WHERE x.is_read = FALSE",
                params(user)));
    }

    public Optional<Integer> authorOf(int id) {
        return with(c -> Jdbc.one(c, "SELECT author_id FROM announcements WHERE id = ?", rs -> rs.getInt(1), id));
    }

    public int create(String title, String body, int authorId, Integer moduleId, boolean pinned, LocalDateTime now) {
        return with(c -> Jdbc.insert(c, "INSERT INTO announcements (title, body, author_id, module_id, pinned, posted_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)", title, body, authorId, moduleId, pinned, now));
    }

    public void delete(int id) {
        with(c -> Jdbc.update(c, "DELETE FROM announcements WHERE id = ?", id));
    }

    public void markAllRead(User user, LocalDateTime now) {
        with(c -> Jdbc.update(c, "INSERT INTO announcement_reads (user_id, announcement_id, read_at) "
                + "SELECT ?, x.id, ? FROM (" + SELECT + visibleTo(user) + ") x WHERE x.is_read = FALSE",
                concat(new Object[]{user.getUserId(), now}, params(user))));
    }

    private static Object[] concat(Object[] a, Object[] b) {
        Object[] out = new Object[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
