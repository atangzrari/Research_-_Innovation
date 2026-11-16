package com.stride.service;

import com.stride.dao.AttendanceDao;
import com.stride.dao.AuditDao;
import com.stride.dao.ModuleDao;
import com.stride.dao.TimetableDao;
import com.stride.model.AttendanceRecord;
import com.stride.model.AttendanceSession;
import com.stride.model.CheckIn;
import com.stride.model.Module;
import com.stride.model.RosterRow;
import com.stride.model.TimetableEntry;
import com.stride.model.User;
import com.stride.security.LoginThrottle;
import com.stride.security.NetworkMatcher;
import com.stride.security.RotatingCode;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The attendance engine (RQ15-RQ18). Every check-in is validated before it is saved (Figure 3.7):
 * the code or QR token, that the session is still open, that the student is enrolled, and that
 * there is no earlier check-in. A record is never created when validation fails.
 */
public class AttendanceService {

    /** Signals gathered by the web layer for one check-in attempt. */
    public static final class Attempt {
        final String typedCode;
        final Integer qrSessionId;
        final String qrToken;
        final String clientIp;
        final String deviceHash;
        final ConfidenceScorer.Location location;

        public Attempt(String typedCode, Integer qrSessionId, String qrToken, String clientIp, String deviceHash,
                       ConfidenceScorer.Location location) {
            this.typedCode = typedCode;
            this.qrSessionId = qrSessionId;
            this.qrToken = qrToken;
            this.clientIp = clientIp;
            this.deviceHash = deviceHash;
            this.location = location == null ? ConfidenceScorer.Location.NOT_USED : location;
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AttendanceDao attendance;
    private final ModuleDao modules;
    private final TimetableDao timetable;
    private final AuditDao audit;
    private final Clock clock;
    private final ZoneId zone;
    private final NetworkMatcher campusNetwork;
    private final ConfidenceScorer scorer;
    private final int codeSeconds;
    private final int sessionMinutes;
    private final int lateMinutes;
    private final LoginThrottle wrongCodes;

    public AttendanceService(AttendanceDao attendance, ModuleDao modules, TimetableDao timetable, AuditDao audit,
                             Clock clock, ZoneId zone, NetworkMatcher campusNetwork, int codeSeconds,
                             int sessionMinutes, int lateMinutes, int flagBelow) {
        this.attendance = attendance;
        this.modules = modules;
        this.timetable = timetable;
        this.audit = audit;
        this.clock = clock;
        this.zone = zone;
        this.campusNetwork = campusNetwork;
        this.scorer = new ConfidenceScorer(!campusNetwork.isEmpty(), flagBelow);
        this.codeSeconds = codeSeconds;
        this.sessionMinutes = sessionMinutes;
        this.lateMinutes = lateMinutes;
        this.wrongCodes = new LoginThrottle(10, Duration.ofMinutes(5), clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(zone));
    }

    public int getCodeSeconds() {
        return codeSeconds;
    }

    // ------------------------------------------------------------------ lecturer side

    /** Opens (or returns the already-open) session for a timetable slot today (RQ15). */
    public AttendanceSession openForEntry(User lecturer, int entryId) {
        TimetableEntry entry = timetable.find(entryId)
                .orElseThrow(() -> new ServiceException("That class is not on the timetable."));
        requireTeaches(lecturer, entry.getModuleId());
        LocalDateTime now = now();
        Optional<AttendanceSession> existing = attendance.openForEntry(entryId, now.toLocalDate(), now);
        if (existing.isPresent()) {
            return existing.get();
        }
        LocalDateTime startsAt = entry.getDay() == now.getDayOfWeek()
                ? now.toLocalDate().atTime(entry.getStart()) : now;
        int id = attendance.createSession(entry.getModuleId(), entry.getRoomId(), entry.getId(), lecturer.getUserId(),
                now.toLocalDate(), startsAt, now, now.plusMinutes(sessionMinutes), newSecret());
        audit.log(now, lecturer.getUserId(), "SESSION_OPEN", entry.getModuleCode() + " in " + entry.getRoom(), null);
        return attendance.session(id).orElseThrow();
    }

    /** Opens an extra session that is not on the timetable, e.g. a make-up class. */
    public AttendanceSession openAdHoc(User lecturer, int moduleId, Integer roomId) {
        requireTeaches(lecturer, moduleId);
        LocalDateTime now = now();
        int id = attendance.createSession(moduleId, roomId, null, lecturer.getUserId(), now.toLocalDate(), now, now,
                now.plusMinutes(sessionMinutes), newSecret());
        audit.log(now, lecturer.getUserId(), "SESSION_OPEN", "Extra session for module " + moduleId, null);
        return attendance.session(id).orElseThrow();
    }

    public AttendanceSession sessionForLecturer(User lecturer, int sessionId) {
        AttendanceSession s = attendance.session(sessionId)
                .orElseThrow(() -> new ServiceException("That session does not exist."));
        requireTeaches(lecturer, s.getModuleId());
        return s;
    }

    public void close(User lecturer, int sessionId) {
        AttendanceSession s = sessionForLecturer(lecturer, sessionId);
        attendance.close(s.getId(), now());
        audit.log(now(), lecturer.getUserId(), "SESSION_CLOSE", s.getModuleCode() + " session " + s.getId(), null);
    }

    /** Keeps check-in open for ten more minutes. */
    public void extend(User lecturer, int sessionId) {
        AttendanceSession s = sessionForLecturer(lecturer, sessionId);
        LocalDateTime base = s.isOpenAt(now()) ? s.getClosesAt() : now();
        attendance.extend(s.getId(), base.plusMinutes(10));
    }

    public List<AttendanceSession> recentSessions(User lecturer) {
        return attendance.forLecturer(lecturer.getUserId(), 50);
    }

    /** The code to show on the projector now. Only for an open session. */
    public String currentCode(AttendanceSession s) {
        return RotatingCode.code(s.getSecret(), RotatingCode.window(clock.instant(), codeSeconds));
    }

    public String currentQrToken(AttendanceSession s) {
        return RotatingCode.qrToken(s.getSecret(), RotatingCode.window(clock.instant(), codeSeconds));
    }

    public int secondsUntilNextCode() {
        return RotatingCode.secondsLeft(clock.instant(), codeSeconds);
    }

    public boolean isOpen(AttendanceSession s) {
        return s.isOpenAt(now());
    }

    /** Everyone enrolled in the session's module, with their check-in if any. */
    public List<RosterRow> roster(AttendanceSession s) {
        Map<Integer, CheckIn> byStudent = new HashMap<>();
        for (CheckIn c : attendance.checkIns(s.getId())) {
            byStudent.put(c.getStudentId(), c);
        }
        List<RosterRow> rows = new ArrayList<>();
        for (User u : modules.students(s.getModuleId())) {
            rows.add(new RosterRow(u, byStudent.get(u.getUserId())));
        }
        rows.sort((a, b) -> {
            int fa = a.getCheckIn() != null && a.getCheckIn().isFlagged() ? 0 : 1;
            int fb = b.getCheckIn() != null && b.getCheckIn().isFlagged() ? 0 : 1;
            return fa != fb ? fa - fb : a.getStudent().getLastName().compareTo(b.getStudent().getLastName());
        });
        return rows;
    }

    /** Lecturer correction of one student's record, with a reason (RQ18). Students are refused. */
    public void correct(User lecturer, int sessionId, int studentId, CheckIn.Status status, String reason) {
        AttendanceSession s = sessionForLecturer(lecturer, sessionId);
        if (status == null) {
            throw new ServiceException("Choose Present, Late, Absent or Excused.");
        }
        String why = reason == null ? "" : reason.trim();
        if (why.length() < 3) {
            throw new ServiceException("Give a short reason for the change, for example \"doctor's note\".");
        }
        if (why.length() > 300) {
            why = why.substring(0, 300);
        }
        if (!modules.isEnrolled(studentId, s.getModuleId())) {
            throw new ServiceException("That student is not enrolled in " + s.getModuleCode() + ".");
        }
        attendance.correct(s.getId(), studentId, status, why, lecturer.getUserId(), now());
        audit.log(now(), lecturer.getUserId(), "ATTENDANCE_CORRECT",
                s.getModuleCode() + " session " + s.getId() + ", student " + studentId + " -> " + status + ": " + why, null);
    }

    /** Lecturer accepts a flagged check-in after review. */
    public void acceptFlagged(User lecturer, int sessionId, int studentId) {
        AttendanceSession s = sessionForLecturer(lecturer, sessionId);
        attendance.clearFlag(s.getId(), studentId, lecturer.getUserId(), now());
        audit.log(now(), lecturer.getUserId(), "ATTENDANCE_ACCEPT",
                s.getModuleCode() + " session " + s.getId() + ", student " + studentId, null);
    }

    private void requireTeaches(User user, int moduleId) {
        if (!user.isLecturer()) {
            throw new ServiceException("Only a lecturer can do that.");
        }
        Module m = modules.find(moduleId).orElseThrow(() -> new ServiceException("That module does not exist."));
        if (m.getLecturerId() == null || m.getLecturerId() != user.getUserId()) {
            throw new ServiceException("You do not teach " + m.getCode() + ".");
        }
    }

    private static String newSecret() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    // ------------------------------------------------------------------ student side

    public List<AttendanceSession> openSessionsFor(User student) {
        return attendance.openForStudent(student.getUserId(), now());
    }

    /** Validates and records a check-in (RQ16). */
    public CheckInResult checkIn(User student, Attempt attempt) {
        if (!student.isStudent()) {
            return CheckInResult.rejected("Only students check in to classes.");
        }
        String key = "student:" + student.getUserId();
        if (!wrongCodes.remainingLock(key).isZero()) {
            return CheckInResult.rejected("Too many wrong codes. Wait five minutes, then try again.");
        }
        LocalDateTime now = now();
        List<AttendanceSession> open = attendance.openForStudent(student.getUserId(), now);

        AttendanceSession session = null;
        ConfidenceScorer.Token token;
        if (attempt.qrSessionId != null) {
            token = ConfidenceScorer.Token.QR;
            Optional<AttendanceSession> target = attendance.session(attempt.qrSessionId);
            if (target.isEmpty()) {
                return CheckInResult.rejected("That QR code is not a Stride check-in code.");
            }
            AttendanceSession s = target.get();
            if (!modules.isEnrolled(student.getUserId(), s.getModuleId())) {
                return CheckInResult.rejected("You are not enrolled in " + s.getModuleCode() + ", so you cannot check in to it.", s);
            }
            if (!s.isOpenAt(now)) {
                return CheckInResult.rejected("Check-in for " + s.getModuleCode() + " has closed. Speak to your lecturer.", s);
            }
            if (!RotatingCode.tokenMatches(s.getSecret(), attempt.qrToken, clock.instant(), codeSeconds)) {
                wrongCodes.recordFailure(key);
                return CheckInResult.rejected("That QR code has expired. Scan the code on the screen again.", s);
            }
            session = s;
        } else {
            token = ConfidenceScorer.Token.CODE;
            String typed = RotatingCode.normalise(attempt.typedCode);
            if (typed.length() != RotatingCode.CODE_LENGTH) {
                return CheckInResult.rejected("The code has " + RotatingCode.CODE_LENGTH
                        + " letters and numbers. Check the screen and try again.");
            }
            if (open.isEmpty()) {
                return CheckInResult.rejected("None of your classes is taking attendance right now.");
            }
            for (AttendanceSession s : open) {
                if (RotatingCode.codeMatches(s.getSecret(), typed, clock.instant(), codeSeconds)) {
                    session = s;
                    break;
                }
            }
            if (session == null) {
                wrongCodes.recordFailure(key);
                return CheckInResult.rejected("That code is wrong or has expired. Codes change every "
                        + codeSeconds + " seconds: type the one on the screen now.");
            }
        }

        Optional<CheckIn> earlier = attendance.checkIn(session.getId(), student.getUserId());
        if (earlier.isPresent()) {
            return CheckInResult.rejected("You already checked in to " + session.getModuleCode() + " at "
                    + earlier.get().getTimeText() + ".", session);
        }

        boolean onCampus = campusNetwork.contains(attempt.clientIp);
        boolean shared = attempt.deviceHash != null
                && attendance.deviceUsedByOther(session.getId(), attempt.deviceHash, student.getUserId());
        ConfidenceScorer.Score score = scorer.score(token, onCampus, shared, attempt.location);
        CheckIn.Status status = now.isAfter(session.getStartsAt().plusMinutes(lateMinutes))
                ? CheckIn.Status.LATE : CheckIn.Status.PRESENT;
        CheckIn.Method method = token == ConfidenceScorer.Token.QR ? CheckIn.Method.QR : CheckIn.Method.CODE;

        boolean saved = attendance.insertCheckIn(session.getId(), student.getUserId(), now, method, status,
                score.getValue(), score.getFactorText(), attempt.clientIp, attempt.deviceHash, score.isFlagged());
        if (!saved) {
            return CheckInResult.rejected("You are already checked in to " + session.getModuleCode() + ".", session);
        }
        wrongCodes.reset(key);
        return CheckInResult.accepted(session, status, score);
    }

    /** Attendance per enrolled module: classes attended out of classes held (RQ11). */
    public List<AttendanceRecord> summaryFor(User student, List<Module> enrolled) {
        LocalDateTime now = now();
        Map<Integer, List<Integer>> heldByModule = new HashMap<>();
        for (AttendanceDao.HeldSession h : attendance.held(now)) {
            heldByModule.computeIfAbsent(h.moduleId, k -> new ArrayList<>()).add(h.id);
        }
        Map<Integer, CheckIn.Status> marks = new HashMap<>();
        for (AttendanceDao.Mark m : attendance.marksForStudent(student.getUserId())) {
            marks.put(m.sessionId, m.status);
        }
        List<AttendanceRecord> out = new ArrayList<>();
        for (Module m : enrolled) {
            int attended = 0;
            int total = 0;
            for (int sessionId : heldByModule.getOrDefault(m.getId(), List.of())) {
                CheckIn.Status st = marks.get(sessionId);
                if (st == CheckIn.Status.EXCUSED) {
                    continue;   // an excused absence does not count against the student
                }
                total++;
                if (st != null && st.isAttended()) {
                    attended++;
                }
            }
            out.add(new AttendanceRecord(m.getCode(), m.getName(), attended, total));
        }
        return out;
    }

    public List<CheckIn> history(User student, int limit) {
        return attendance.recentForStudent(student.getUserId(), limit);
    }

    /** Ids of sessions a student was marked for, used by tests and reports. */
    public Set<Integer> sessionIdsWithMarks(int studentId) {
        Set<Integer> ids = new HashSet<>();
        for (AttendanceDao.Mark m : attendance.marksForStudent(studentId)) {
            ids.add(m.sessionId);
        }
        return ids;
    }
}
