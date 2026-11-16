package com.stride.dao;

import com.stride.db.Database;
import com.stride.db.Jdbc;
import com.stride.model.Module;
import com.stride.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ModuleDao extends Dao {

    private static final String SELECT = "SELECT m.id, m.code, m.name, m.description, m.lecturer_id, "
            + "CONCAT(u.first_name, ' ', u.last_name) AS lecturer_name, "
            + "(SELECT COUNT(*) FROM enrolments e WHERE e.module_id = m.id) AS students "
            + "FROM modules m LEFT JOIN users u ON u.id = m.lecturer_id ";

    public ModuleDao(Database db) {
        super(db);
    }

    static Module map(ResultSet rs) throws SQLException {
        return new Module(rs.getInt("id"), rs.getString("code"), rs.getString("name"), rs.getString("description"),
                Jdbc.intOrNull(rs, "lecturer_id"), rs.getString("lecturer_name"), rs.getInt("students"));
    }

    public List<Module> all() {
        return with(c -> Jdbc.query(c, SELECT + "ORDER BY m.code", ModuleDao::map));
    }

    public Optional<Module> find(int id) {
        return with(c -> Jdbc.one(c, SELECT + "WHERE m.id = ?", ModuleDao::map, id));
    }

    public Optional<Module> findByCode(String code) {
        return with(c -> Jdbc.one(c, SELECT + "WHERE UPPER(m.code) = UPPER(?)", ModuleDao::map, code));
    }

    public List<Module> forStudent(int studentId) {
        return with(c -> Jdbc.query(c, SELECT + "WHERE m.id IN (SELECT module_id FROM enrolments WHERE student_id = ?) "
                + "ORDER BY m.code", ModuleDao::map, studentId));
    }

    public List<Module> forLecturer(int lecturerId) {
        return with(c -> Jdbc.query(c, SELECT + "WHERE m.lecturer_id = ? ORDER BY m.code", ModuleDao::map,
                lecturerId));
    }

    public int create(String code, String name, String description, Integer lecturerId) {
        return with(c -> Jdbc.insert(c, "INSERT INTO modules (code, name, description, lecturer_id) VALUES (?, ?, ?, ?)",
                code, name, description, lecturerId));
    }

    public void update(int id, String code, String name, String description, Integer lecturerId) {
        with(c -> Jdbc.update(c, "UPDATE modules SET code = ?, name = ?, description = ?, lecturer_id = ? WHERE id = ?",
                code, name, description, lecturerId, id));
    }

    public boolean isEnrolled(int studentId, int moduleId) {
        return with(c -> Jdbc.count(c, "SELECT COUNT(*) FROM enrolments WHERE student_id = ? AND module_id = ?",
                studentId, moduleId) > 0);
    }

    /** Adds the enrolment; returns false if it already existed. */
    public boolean enrol(int studentId, int moduleId, LocalDateTime now) {
        return with(c -> {
            if (Jdbc.count(c, "SELECT COUNT(*) FROM enrolments WHERE student_id = ? AND module_id = ?",
                    studentId, moduleId) > 0) {
                return false;
            }
            Jdbc.update(c, "INSERT INTO enrolments (student_id, module_id, enrolled_at) VALUES (?, ?, ?)",
                    studentId, moduleId, now);
            return true;
        });
    }

    public void unenrol(int studentId, int moduleId) {
        with(c -> Jdbc.update(c, "DELETE FROM enrolments WHERE student_id = ? AND module_id = ?", studentId, moduleId));
    }

    public List<User> students(int moduleId) {
        return with(c -> Jdbc.query(c, "SELECT u.id, u.first_name, u.last_name, u.email, u.role, u.student_number, "
                + "u.password_hash, u.must_change_password, u.active, u.last_login_at FROM users u "
                + "JOIN enrolments e ON e.student_id = u.id WHERE e.module_id = ? ORDER BY u.last_name, u.first_name",
                UserDao::map, moduleId));
    }
}
