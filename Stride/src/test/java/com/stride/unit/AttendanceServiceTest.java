package com.stride.unit;

import com.stride.TestApp;
import com.stride.config.App;
import com.stride.model.AttendanceRecord;
import com.stride.model.AttendanceSession;
import com.stride.model.CheckIn;
import com.stride.model.RosterRow;
import com.stride.model.TodayClass;
import com.stride.model.User;
import com.stride.service.AttendanceService;
import com.stride.service.CheckInResult;
import com.stride.service.ServiceException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** The attendance engine (RQ15-RQ18), on the seeded database at Thursday 15:27 (CSE302 in progress). */
public class AttendanceServiceTest {

    private App app;
    private User lecturer;
    private User otherLecturer;
    private User student;
    private User classmate;

    @Before
    public void up() {
        app = TestApp.start();
        lecturer = app.users.findByEmail("lecturer@stride.test").get();
        otherLecturer = app.users.findByEmail("neo.mokoena@stride.test").get();
        student = app.users.findByEmail("student@stride.test").get();
        classmate = app.users.findByEmail("tumelo.molefe@stride.test").get();
    }

    @After
    public void down() {
        app.close();
    }

    private AttendanceSession openCse302() {
        for (TodayClass c : app.schedule.classesToday(lecturer, app.now())) {
            if (c.getModuleCode().equals("CSE302")) return app.attendance.openForEntry(lecturer, c.getEntryId());
        }
        throw new AssertionError("CSE302 should be on today");
    }

    private static AttendanceService.Attempt code(String code, String ip, String device) {
        return new AttendanceService.Attempt(code, null, null, ip, device, null);
    }

    @Test
    public void reportFiguresAreReproduced() {
        List<AttendanceRecord> r = app.attendance.summaryFor(student, app.modules.forStudent(student.getUserId()));
        StringBuilder sb = new StringBuilder();
        for (AttendanceRecord a : r) sb.append(a.getModuleCode()).append(' ').append(a.getAttended()).append('/').append(a.getTotal()).append(' ');
        assertEquals("CSE206 7/8 CSE302 9/10 CSE305 6/8 CSE310 10/10 CSE315 5/7 ", sb.toString());
    }

    @Test
    public void openingTwiceReturnsTheSameSession() {
        assertEquals(openCse302().getId(), openCse302().getId());
    }

    @Test
    public void onlyTheModuleLecturerMayOpen() {
        int entry = app.schedule.classesToday(lecturer, app.now()).stream()
                .filter(c -> c.getModuleCode().equals("CSE302")).findFirst().get().getEntryId();
        assertThrows(ServiceException.class, () -> app.attendance.openForEntry(otherLecturer, entry));
        assertThrows(ServiceException.class, () -> app.attendance.openForEntry(student, entry));
    }

    @Test
    public void validCodeOnCampusIsAcceptedWithHighConfidence() {
        AttendanceSession s = openCse302();
        CheckInResult r = app.attendance.checkIn(student, code(app.attendance.currentCode(s), "127.0.0.1", "dev-a"));
        assertTrue(r.getMessage(), r.isOk());
        assertEquals(CheckIn.Status.LATE, r.getStatus());   // 15:27 is more than 15 minutes after 14:00
        assertEquals(80, r.getConfidence());
        assertFalse(r.isFlagged());
    }

    @Test
    public void lowerCaseAndSpacesAreAccepted() {
        AttendanceSession s = openCse302();
        String c = app.attendance.currentCode(s).toLowerCase();
        assertTrue(app.attendance.checkIn(student, code(c.substring(0, 3) + " " + c.substring(3), "127.0.0.1", "d")).isOk());
    }

    @Test
    public void duplicateCheckInIsRejectedAndNoSecondRecordMade() {
        AttendanceSession s = openCse302();
        assertTrue(app.attendance.checkIn(student, code(app.attendance.currentCode(s), "127.0.0.1", "d1")).isOk());
        CheckInResult again = app.attendance.checkIn(student, code(app.attendance.currentCode(s), "127.0.0.1", "d1"));
        assertFalse(again.isOk());
        assertTrue(again.getMessage().contains("already"));
        assertEquals(1, app.attendanceDao.checkIns(s.getId()).size());
    }

    @Test
    public void wrongCodeIsRejectedAndNothingSaved() {
        AttendanceSession s = openCse302();
        assertFalse(app.attendance.checkIn(student, code("ZZZZZZ", "127.0.0.1", "d")).isOk());
        assertFalse(app.attendance.checkIn(student, code("ab", "127.0.0.1", "d")).isOk());
        assertTrue(app.attendanceDao.checkIns(s.getId()).isEmpty());
    }

    @Test
    public void noOpenSessionMeansNothingToCheckInTo() {
        CheckInResult r = app.attendance.checkIn(student, code("ABCDEF", "127.0.0.1", "d"));
        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("right now"));
    }

    @Test
    public void closedSessionRejectsCheckIn() {
        AttendanceSession s = openCse302();
        String c = app.attendance.currentCode(s);
        app.attendance.close(lecturer, s.getId());
        assertFalse(app.attendance.checkIn(student, code(c, "127.0.0.1", "d")).isOk());
    }

    @Test
    public void qrTokenGivesQrFactor() {
        AttendanceSession s = openCse302();
        CheckInResult r = app.attendance.checkIn(student, new AttendanceService.Attempt(null, s.getId(),
                app.attendance.currentQrToken(s), "127.0.0.1", "d", null));
        assertTrue(r.getMessage(), r.isOk());
        assertEquals(90, r.getConfidence());
        assertEquals(CheckIn.Method.QR, app.attendanceDao.checkIn(s.getId(), student.getUserId()).get().getMethod());
    }

    @Test
    public void staleQrTokenIsRejected() {
        AttendanceSession s = openCse302();
        assertFalse(app.attendance.checkIn(student, new AttendanceService.Attempt(null, s.getId(),
                "AAAAAAAAAAAAAAAAAAAA", "127.0.0.1", "d", null)).isOk());
    }

    @Test
    public void offCampusCheckInIsFlagged() {
        AttendanceSession s = openCse302();
        CheckInResult r = app.attendance.checkIn(student, code(app.attendance.currentCode(s), "41.75.3.9", "d"));
        assertTrue(r.isOk());
        assertTrue(r.isFlagged());
        assertTrue(r.getConfidence() < 60);
    }

    @Test
    public void sharedDeviceIsFlagged() {
        AttendanceSession s = openCse302();
        assertTrue(app.attendance.checkIn(student, code(app.attendance.currentCode(s), "127.0.0.1", "same-phone")).isOk());
        CheckInResult second = app.attendance.checkIn(classmate, code(app.attendance.currentCode(s), "127.0.0.1", "same-phone"));
        assertTrue(second.isOk());
        assertTrue(second.isFlagged());
    }

    @Test
    public void studentNotEnrolledCannotCheckIn() {
        // The last three seeded students are not enrolled in CSE206.
        User outsider = app.users.findByEmail("wame.bogatsu@stride.test").get();
        User rniLecturer = app.users.findByEmail("kagiso.sebina@stride.test").get();
        AttendanceSession s = app.attendance.openAdHoc(rniLecturer, app.modules.findByCode("CSE206").get().getId(), null);
        CheckInResult r = app.attendance.checkIn(outsider, new AttendanceService.Attempt(null, s.getId(),
                app.attendance.currentQrToken(s), "127.0.0.1", "d", null));
        assertFalse(r.isOk());
        assertTrue(r.getMessage().contains("not enrolled"));
    }

    @Test
    public void lecturerCorrectionNeedsReasonAndIsAudited() {
        AttendanceSession s = openCse302();
        assertThrows(ServiceException.class, () -> app.attendance.correct(lecturer, s.getId(), student.getUserId(),
                CheckIn.Status.EXCUSED, ""));
        app.attendance.correct(lecturer, s.getId(), student.getUserId(), CheckIn.Status.EXCUSED, "Doctor's note");
        CheckIn c = app.attendanceDao.checkIn(s.getId(), student.getUserId()).get();
        assertEquals(CheckIn.Status.EXCUSED, c.getStatus());
        assertEquals("Doctor's note", c.getCorrectionReason());
        assertEquals("ATTENDANCE_CORRECT", app.audit.recent("ATTENDANCE_CORRECT", 1).get(0).getAction());
    }

    @Test
    public void studentsAndOtherLecturersCannotCorrect() {
        AttendanceSession s = openCse302();
        assertThrows(ServiceException.class, () -> app.attendance.correct(student, s.getId(), student.getUserId(),
                CheckIn.Status.PRESENT, "I was there"));
        assertThrows(ServiceException.class, () -> app.attendance.correct(otherLecturer, s.getId(),
                student.getUserId(), CheckIn.Status.PRESENT, "Was there"));
    }

    @Test
    public void rosterListsEveryEnrolledStudentFlaggedFirst() {
        AttendanceSession s = openCse302();
        app.attendance.checkIn(classmate, code(app.attendance.currentCode(s), "41.75.3.9", "x"));
        List<RosterRow> roster = app.attendance.roster(s);
        assertEquals(24, roster.size());
        assertNotNull(roster.get(0).getCheckIn());
        assertTrue(roster.get(0).getCheckIn().isFlagged());
    }

    @Test
    public void acceptingClearsTheFlag() {
        AttendanceSession s = openCse302();
        app.attendance.checkIn(student, code(app.attendance.currentCode(s), "41.75.3.9", "x"));
        app.attendance.acceptFlagged(lecturer, s.getId(), student.getUserId());
        assertFalse(app.attendanceDao.checkIn(s.getId(), student.getUserId()).get().isFlagged());
    }
}
