package com.stride.dao;

import com.stride.db.Database;
import com.stride.db.Jdbc;
import com.stride.model.Role;
import com.stride.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class UserDao extends Dao {

    /** A user together with the stored password hash; never put this in the session. */
    public static final class Account {
        public final User user;
        public final String passwordHash;

        Account(User user, String passwordHash) {
            this.user = user;
            this.passwordHash = passwordHash;
        }
    }

    private static final String COLS = "id, first_name, last_name, email, role, student_number, password_hash, "
            + "must_change_password, active, last_login_at";

    public UserDao(Database db) {
        super(db);
    }

    static User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"),
                rs.getString("email"), Role.valueOf(rs.getString("role")), rs.getString("student_number"),
                rs.getBoolean("active"), rs.getBoolean("must_change_password"), Jdbc.dateTime(rs, "last_login_at"));
    }

    public Optional<Account> findAccountByEmail(String normalisedEmail) {
        return with(c -> Jdbc.one(c, "SELECT " + COLS + " FROM users WHERE LOWER(email) = ?",
                rs -> new Account(map(rs), rs.getString("password_hash")), normalisedEmail));
    }

    public Optional<Account> findAccount(int id) {
        return with(c -> Jdbc.one(c, "SELECT " + COLS + " FROM users WHERE id = ?",
                rs -> new Account(map(rs), rs.getString("password_hash")), id));
    }

    public Optional<User> find(int id) {
        return with(c -> Jdbc.one(c, "SELECT " + COLS + " FROM users WHERE id = ?", UserDao::map, id));
    }

    public Optional<User> findByEmail(String normalisedEmail) {
        return with(c -> Jdbc.one(c, "SELECT " + COLS + " FROM users WHERE LOWER(email) = ?", UserDao::map,
                normalisedEmail));
    }

    public Optional<User> findByStudentNumber(String number) {
        return with(c -> Jdbc.one(c, "SELECT " + COLS + " FROM users WHERE student_number = ?", UserDao::map,
                number));
    }

    /** Users, optionally filtered by role and by a search over name, e-mail and student number. */
    public List<User> list(Role role, String search) {
        StringBuilder sql = new StringBuilder("SELECT " + COLS + " FROM users WHERE 1 = 1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (role != null) {
            sql.append(" AND role = ?");
            params.add(role.name());
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? OR LOWER(email) LIKE ?"
                    + " OR LOWER(COALESCE(student_number, '')) LIKE ?)");
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY role, last_name, first_name");
        return with(c -> Jdbc.query(c, sql.toString(), UserDao::map, params.toArray()));
    }

    public int create(String first, String last, String email, Role role, String studentNumber,
                      String passwordHash, boolean mustChange, LocalDateTime now) {
        return with(c -> Jdbc.insert(c, "INSERT INTO users (first_name, last_name, email, role, student_number, "
                        + "password_hash, must_change_password, active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, ?)",
                first, last, email, role, studentNumber, passwordHash, mustChange, now));
    }

    public void update(int id, String first, String last, String email, Role role, String studentNumber) {
        with(c -> Jdbc.update(c, "UPDATE users SET first_name = ?, last_name = ?, email = ?, role = ?, "
                + "student_number = ? WHERE id = ?", first, last, email, role, studentNumber, id));
    }

    public void setActive(int id, boolean active) {
        with(c -> Jdbc.update(c, "UPDATE users SET active = ? WHERE id = ?", active, id));
    }

    public void setPassword(int id, String hash, boolean mustChange) {
        with(c -> Jdbc.update(c, "UPDATE users SET password_hash = ?, must_change_password = ? WHERE id = ?",
                hash, mustChange, id));
    }

    public void touchLogin(int id, LocalDateTime now) {
        with(c -> Jdbc.update(c, "UPDATE users SET last_login_at = ? WHERE id = ?", now, id));
    }

    public Map<Role, Integer> countByRole() {
        return with(c -> {
            Map<Role, Integer> out = new TreeMap<>();
            for (Role r : Role.values()) {
                out.put(r, 0);
            }
            Jdbc.query(c, "SELECT role, COUNT(*) AS n FROM users WHERE active = TRUE GROUP BY role", rs -> {
                out.put(Role.valueOf(rs.getString("role")), rs.getInt("n"));
                return null;
            });
            return out;
        });
    }
}
