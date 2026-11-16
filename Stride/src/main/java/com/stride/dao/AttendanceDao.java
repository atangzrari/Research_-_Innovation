package com.stride.dao;

import com.stride.db.Database;
import com.stride.db.Jdbc;
import com.stride.model.AttendanceSession;
import com.stride.model.CheckIn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Attendance sessions and check-ins (RQ15-RQ18). */
public class AttendanceDao extends Dao {

    private static final String SESSION = "SELECT s.*, m.code, m.name AS module_name, r.name AS room_name, "
            + "(SELECT COUNT(*) FROM check_ins ci WHERE ci.session_id = s.id AND ci.status IN ('PRESENT','LATE')) AS n_in, "
            + "(SELECT COUNT(*) FROM check_ins ci WHERE ci.session_id = s.id AND ci.flagged = TRUE) AS n_flag, "
            + "(SELECT COUNT(*) FROM enrolments e WHERE e.module_id = s.module_id) AS n_enrol "
            + "FROM attendance_sessions s JOIN modules m ON m.id = s.module_id LEFT JOIN rooms r ON r.id = s.room_id ";

    private static final String CHECKIN = "SELECT ci.*, CONCAT(u.first_name, ' ', u.last_name) AS student_name, "
            + "u.student_number, CONCAT(k.first_name, ' ', k.last_name) AS corrector_name FROM check_ins ci "
            + "JOIN users u ON u.id = ci.student_id LEFT JOIN users k ON k.id = ci.corrected_by ";

    /** The parts of a held session needed to work out attendance percentages. */
    public static final class HeldSession {
        public final int id;
        public final int moduleId;
        public final LocalDate date;

        HeldSession(int id, int moduleId, LocalDate date) {
            this.id = id;
            this.moduleId = moduleId;
            this.date = date;
        }
    }

    /** A compact check-in row for reports. */
    public static final class Mark {
        public final int sessionId;
        public final int studentId;
        public final CheckIn.Status status;
        public final CheckIn.Method method;
        public final int confidence;
        public final boolean flagged;
        public final String factors;

        Mark(int sessionId, int studentId, CheckIn.Status status, CheckIn.Method method, int confidence,
             boolean flagged, String factors) {
            this.sessionId = sessionId;
            this.studentId = studentId;
            this.status = status;
            this.method = method;
            this.confidence = confidence;
            this.flagged = flagged;
            this.factors = factors;
        }
    }

    public AttendanceDao(Database db) {
        super(db);
    }

    static AttendanceSession mapSession(ResultSet rs) throws SQLException {
        AttendanceSession s = new AttendanceSession(rs.getInt("id"), rs.getInt("module_id"), rs.getString("code"),
                rs.getString("module_name"), Jdbc.intOrNull(rs, "room_id"), rs.getString("room_name"),
                Jdbc.intOrNull(rs, "timetable_entry_id"), rs.getInt("lecturer_id"), Jdbc.date(rs, "session_date"),
                Jdbc.dateTime(rs, "starts_at"), Jdbc.dateTime(rs, "opened_at"), Jdbc.dateTime(rs, "closes_at"),
                Jdbc.dateTime(rs, "closed_at"), rs.getString("secret"));
        s.setCheckedIn(rs.getInt("n_in"));
        s.setFlagged(rs.getInt("n_flag"));
        s.setEnrolled(rs.getInt("n_enrol"));
        return s;
    }

    static CheckIn mapCheckIn(ResultSet rs) throws SQLException {
        return new CheckIn(rs.getInt("id"), rs.getInt("session_id"), rs.getInt("student_id"),
                rs.getString("student_name"), rs.getString("student_number"), Jdbc.dateTime(rs, "checked_in_at"),
                CheckIn.Method.valueOf(rs.getString("method")), CheckIn.Status.valueOf(rs.getString("status")),
                rs.getInt("confidence"), rs.getString("factors"), rs.getBoolean("flagged"),
                rs.getString("correction_reason"), rs.getString("corrector_name"));
    }

    public int createSession(int moduleId, Integer roomId, Integer entryId, int lecturerId, LocalDate date,
                             LocalDateTime startsAt, LocalDateTime openedAt, LocalDateTime closesAt, String secret) {
        return with(c -> insertSession(c, moduleId, roomId, entryId, lecturerId, date, startsAt, openedAt, closesAt,
                null, secret));
    }

    static int insertSession(Connection c, int moduleId, Integer roomId, Integer entryId, int lecturerId,
                             LocalDate date, LocalDateTime startsAt, LocalDateTime openedAt, LocalDateTime closesAt,
                             LocalDateTime closedAt, String secret) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO attendance_sessions (module_id, room_id, timetable_entry_id, lecturer_id, "
                        + "session_date, starts_at, opened_at, closes_at, closed_at, secret) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                moduleId, roomId, entryId, lecturerId, date, startsAt, openedAt, closesAt, closedAt, secret);
    }

    public Optional<AttendanceSession> session(int id) {
        return with(c -> Jdbc.one(c, SESSION + "WHERE s.id = ?", AttendanceDao::mapSession, id));
    }

    /** Sessions open right now for modules the student is enrolled in. */
    public List<AttendanceSession> openForStudent(int studentId, LocalDateTime now) {
        return with(c -> Jdbc.query(c, SESSION + "WHERE s.closed_at IS NULL AND s.opened_at <= ? AND s.closes_at > ? "
                        + "AND s.module_id IN (SELECT module_id FROM enrolments WHERE student_id = ?) ORDER BY s.opened_at",
                AttendanceDao::mapSession, now, now, studentId));
    }

    /** A lecturer's sessions, newest first. */
    public List<AttendanceSession> forLecturer(int lecturerId, int limit) {
        return with(c -> Jdbc.query(c, SESSION + "WHERE s.lecturer_id = ? ORDER BY s.opened_at DESC LIMIT " + limit,
                AttendanceDao::mapSession, lecturerId));
    }

    /** The open session for a timetable slot on a date, if any. */
    public Optional<AttendanceSession> openForEntry(int entryId, LocalDate date, LocalDateTime now) {
        return with(c -> Jdbc.one(c, SESSION + "WHERE s.timetable_entry_id = ? AND s.session_date = ? "
                + "AND s.closed_at IS NULL AND s.closes_at > ?", AttendanceDao::mapSession, entryId, date, now));
    }

    public void close(int sessionId, LocalDateTime now) {
        with(c -> Jdbc.update(c, "UPDATE attendance_sessions SET closed_at = ? WHERE id = ? AND closed_at IS NULL",
                now, sessionId));
    }

    public void extend(int sessionId, LocalDateTime newClose) {
        with(c -> Jdbc.update(c, "UPDATE attendance_sessions SET closes_at = ?, closed_at = NULL WHERE id = ?",
                newClose, sessionId));
    }

    /** Sessions that have finished taking check-ins (closed by hand or past their closing time). */
    public List<HeldSession> held(LocalDateTime now) {
        return with(c -> Jdbc.query(c, "SELECT id, module_id, session_date FROM attendance_sessions "
                        + "WHERE closed_at IS NOT NULL OR closes_at <= ? ORDER BY session_date",
                rs -> new HeldSession(rs.getInt("id"), rs.getInt("module_id"), Jdbc.date(rs, "session_date")), now));
    }

    public List<Mark> marks() {
        return with(c -> Jdbc.query(c, "SELECT session_id, student_id, status, method, confidence, flagged, factors "
                + "FROM check_ins", rs -> new Mark(rs.getInt("session_id"), rs.getInt("student_id"),
                CheckIn.Status.valueOf(rs.getString("status")), CheckIn.Method.valueOf(rs.getString("method")),
                rs.getInt("confidence"), rs.getBoolean("flagged"), rs.getString("factors"))));
    }

    public List<Mark> marksForStudent(int studentId) {
        return with(c -> Jdbc.query(c, "SELECT session_id, student_id, status, method, confidence, flagged, factors "
                + "FROM check_ins WHERE student_id = ?", rs -> new Mark(rs.getInt("session_id"),
                rs.getInt("student_id"), CheckIn.Status.valueOf(rs.getString("status")),
                CheckIn.Method.valueOf(rs.getString("method")), rs.getInt("confidence"), rs.getBoolean("flagged"),
                rs.getString("factors")), studentId));
    }

    public List<CheckIn> checkIns(int sessionId) {
        return with(c -> Jdbc.query(c, CHECKIN + "WHERE ci.session_id = ? ORDER BY ci.checked_in_at",
                AttendanceDao::mapCheckIn, sessionId));
    }

    public Optional<CheckIn> checkIn(int sessionId, int studentId) {
        return with(c -> Jdbc.one(c, CHECKIN + "WHERE ci.session_id = ? AND ci.student_id = ?",
                AttendanceDao::mapCheckIn, sessionId, studentId));
    }

    /** A student's recent check-ins, newest first. */
    public List<CheckIn> recentForStudent(int studentId, int limit) {
        return with(c -> Jdbc.query(c, CHECKIN + "WHERE ci.student_id = ? ORDER BY ci.checked_in_at DESC LIMIT " + limit,
                AttendanceDao::mapCheckIn, studentId));
    }

    /** True if another student already checked in to this session from the same device. */
    public boolean deviceUsedByOther(int sessionId, String deviceHash, int studentId) {
        return with(c -> Jdbc.count(c, "SELECT COUNT(*) FROM check_ins WHERE session_id = ? AND device_hash = ? "
                + "AND student_id <> ?", sessionId, deviceHash, studentId) > 0);
    }

    /**
     * Saves a check-in. Returns false if the student already has one for this session: the
     * unique key (session_id, student_id) rejects the duplicate inside the database.
     */
    public boolean insertCheckIn(int sessionId, int studentId, LocalDateTime at, CheckIn.Method method,
                                 CheckIn.Status status, int confidence, String factors, String ip, String deviceHash,
                                 boolean flagged) {
        try {
            with(c -> insertCheckIn(c, sessionId, studentId, at, method, status, confidence, factors, ip, deviceHash,
                    flagged));
            return true;
        } catch (com.stride.db.DataException e) {
            if (e.isDuplicate()) {
                return false;
            }
            throw e;
        }
    }

    static int insertCheckIn(Connection c, int sessionId, int studentId, LocalDateTime at, CheckIn.Method method,
                             CheckIn.Status status, int confidence, String factors, String ip, String deviceHash,
                             boolean flagged) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO check_ins (session_id, student_id, checked_in_at, method, status, confidence, "
                        + "factors, client_ip, device_hash, flagged) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                sessionId, studentId, at, method, status, confidence, factors, ip, deviceHash, flagged);
    }

    /** Lecturer correction (RQ18): updates the record, or creates one if the student never checked in. */
    public void correct(int sessionId, int studentId, CheckIn.Status status, String reason, int correctorId,
                        LocalDateTime now) {
        tx(c -> {
            int changed = Jdbc.update(c, "UPDATE check_ins SET status = ?, correction_reason = ?, corrected_by = ?, "
                            + "corrected_at = ?, flagged = FALSE WHERE session_id = ? AND student_id = ?",
                    status, reason, correctorId, now, sessionId, studentId);
            if (changed == 0) {
                int id = insertCheckIn(c, sessionId, studentId, now, CheckIn.Method.MANUAL, status, 100,
                        "Set by lecturer", null, null, false);
                Jdbc.update(c, "UPDATE check_ins SET correction_reason = ?, corrected_by = ?, corrected_at = ? "
                        + "WHERE id = ?", reason, correctorId, now, id);
            }
            return null;
        });
    }

    /** Lecturer accepts a flagged check-in as genuine. */
    public void clearFlag(int sessionId, int studentId, int reviewerId, LocalDateTime now) {
        with(c -> Jdbc.update(c, "UPDATE check_ins SET flagged = FALSE, correction_reason = 'Reviewed and accepted', "
                + "corrected_by = ?, corrected_at = ? WHERE session_id = ? AND student_id = ?",
                reviewerId, now, sessionId, studentId));
    }

    /** Number of held sessions per module id. */
    public Map<Integer, Integer> heldCountByModule(LocalDateTime now) {
        return with(c -> {
            Map<Integer, Integer> out = new HashMap<>();
            Jdbc.query(c, "SELECT module_id, COUNT(*) AS n FROM attendance_sessions WHERE closed_at IS NOT NULL "
                    + "OR closes_at <= ? GROUP BY module_id", rs -> out.put(rs.getInt("module_id"), rs.getInt("n")), now);
            return out;
        });
    }
}
