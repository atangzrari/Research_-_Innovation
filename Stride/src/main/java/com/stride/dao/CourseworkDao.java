package com.stride.dao;

import com.stride.db.Database;
import com.stride.db.Jdbc;
import com.stride.model.Assessment;
import com.stride.model.Resource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Assessments (for the study planner) and module resources. */
public class CourseworkDao extends Dao {

    private static final String ASSESS = "SELECT a.*, m.code, m.name AS module_name FROM assessments a "
            + "JOIN modules m ON m.id = a.module_id ";

    public CourseworkDao(Database db) {
        super(db);
    }

    static Assessment map(ResultSet rs) throws SQLException {
        return new Assessment(rs.getInt("id"), rs.getInt("module_id"), rs.getString("code"),
                rs.getString("module_name"), rs.getString("title"), Assessment.Kind.valueOf(rs.getString("kind")),
                Jdbc.dateTime(rs, "due_at"), rs.getInt("weight"), rs.getInt("est_hours"));
    }

    public List<Assessment> forStudentBetween(int studentId, LocalDateTime from, LocalDateTime to) {
        return with(c -> Jdbc.query(c, ASSESS + "WHERE a.module_id IN (SELECT module_id FROM enrolments WHERE "
                + "student_id = ?) AND a.due_at >= ? AND a.due_at < ? ORDER BY a.due_at", CourseworkDao::map,
                studentId, from, to));
    }

    public List<Assessment> forModule(int moduleId) {
        return with(c -> Jdbc.query(c, ASSESS + "WHERE a.module_id = ? ORDER BY a.due_at", CourseworkDao::map, moduleId));
    }

    public int createAssessment(int moduleId, String title, Assessment.Kind kind, LocalDateTime due, int weight,
                                int hours) {
        return with(c -> Jdbc.insert(c, "INSERT INTO assessments (module_id, title, kind, due_at, weight, est_hours) "
                + "VALUES (?, ?, ?, ?, ?, ?)", moduleId, title, kind, due, weight, hours));
    }

    public void deleteAssessment(int id, int moduleId) {
        with(c -> Jdbc.update(c, "DELETE FROM assessments WHERE id = ? AND module_id = ?", id, moduleId));
    }

    public List<Resource> resources(int moduleId) {
        return with(c -> Jdbc.query(c, "SELECT * FROM resources WHERE module_id = ? ORDER BY kind, title",
                rs -> new Resource(rs.getInt("id"), rs.getInt("module_id"), rs.getString("title"), rs.getString("url"),
                        rs.getString("kind")), moduleId));
    }

    public int createResource(int moduleId, String title, String url, String kind) {
        return with(c -> Jdbc.insert(c, "INSERT INTO resources (module_id, title, url, kind) VALUES (?, ?, ?, ?)",
                moduleId, title, url, kind));
    }

    public void deleteResource(int id, int moduleId) {
        with(c -> Jdbc.update(c, "DELETE FROM resources WHERE id = ? AND module_id = ?", id, moduleId));
    }
}
