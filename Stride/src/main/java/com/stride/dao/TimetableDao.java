package com.stride.dao;

import com.stride.db.Database;
import com.stride.db.Jdbc;
import com.stride.model.TimetableEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public class TimetableDao extends Dao {

    private static final String SELECT = "SELECT t.id, t.module_id, t.room_id, t.day_of_week, t.start_time, "
            + "t.end_time, m.code, m.name AS module_name, r.name AS room_name FROM timetable_entries t "
            + "JOIN modules m ON m.id = t.module_id JOIN rooms r ON r.id = t.room_id ";
    private static final String ORDER = " ORDER BY t.day_of_week, t.start_time, m.code";

    public TimetableDao(Database db) {
        super(db);
    }

    static TimetableEntry map(ResultSet rs) throws SQLException {
        return new TimetableEntry(rs.getInt("id"), rs.getInt("module_id"), rs.getInt("room_id"),
                DayOfWeek.of(rs.getInt("day_of_week")), Jdbc.time(rs, "start_time"), Jdbc.time(rs, "end_time"),
                rs.getString("code"), rs.getString("module_name"), rs.getString("room_name"));
    }

    public List<TimetableEntry> all() {
        return with(c -> Jdbc.query(c, SELECT + ORDER, TimetableDao::map));
    }

    public Optional<TimetableEntry> find(int id) {
        return with(c -> Jdbc.one(c, SELECT + "WHERE t.id = ?", TimetableDao::map, id));
    }

    public List<TimetableEntry> forStudent(int studentId) {
        return with(c -> Jdbc.query(c, SELECT + "WHERE t.module_id IN (SELECT module_id FROM enrolments "
                + "WHERE student_id = ?)" + ORDER, TimetableDao::map, studentId));
    }

    public List<TimetableEntry> forLecturer(int lecturerId) {
        return with(c -> Jdbc.query(c, SELECT + "WHERE m.lecturer_id = ?" + ORDER, TimetableDao::map, lecturerId));
    }

    public int create(int moduleId, int roomId, DayOfWeek day, java.time.LocalTime start, java.time.LocalTime end) {
        return with(c -> Jdbc.insert(c, "INSERT INTO timetable_entries (module_id, room_id, day_of_week, start_time, "
                + "end_time) VALUES (?, ?, ?, ?, ?)", moduleId, roomId, day.getValue(), start, end));
    }

    public void delete(int id) {
        with(c -> Jdbc.update(c, "DELETE FROM timetable_entries WHERE id = ?", id));
    }
}
