package com.stride.unit;

import com.stride.SampleTimetable;
import com.stride.TestApp;
import com.stride.config.App;
import com.stride.data.ServiceCatalogue;
import com.stride.model.AttendanceRecord;
import com.stride.model.CampusService;
import com.stride.model.FreeRoom;
import com.stride.model.Role;
import com.stride.model.Room;
import com.stride.model.TimetableEntry;
import com.stride.model.TodayClass;
import com.stride.model.User;
import com.stride.security.LoginThrottle;
import com.stride.security.PasswordHasher;
import com.stride.service.ScheduleService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** The 49 logic cases from the report (Appendix B), now run with JUnit against the real classes. */
public class ReportCasesTest {

    private static final List<TimetableEntry> TT = SampleTimetable.ENTRIES;
    private static final List<Room> ROOMS = SampleTimetable.ROOMS;
    private static App app;

    @BeforeClass
    public static void up() {
        app = TestApp.start();
    }

    @AfterClass
    public static void down() {
        app.close();
    }

    private static TimetableEntry slot(String start, String end) {
        return new TimetableEntry(DayOfWeek.MONDAY, LocalTime.parse(start), LocalTime.parse(end), "X", "X", "R");
    }

    private static LocalTime t(String s) {
        return LocalTime.parse(s);
    }

    private static FreeRoom room(List<FreeRoom> list, String name) {
        for (FreeRoom f : list) if (f.getName().equals(name)) return f;
        return null;
    }

    // ---- Timetable overlap
    @Test public void t01_insideOverlaps() { assertTrue(slot("08:00", "10:00").overlaps(DayOfWeek.MONDAY, t("09:00"), t("09:30"))); }
    @Test public void t02_startAtEndDoesNotOverlap() { assertFalse(slot("08:00", "10:00").overlaps(DayOfWeek.MONDAY, t("10:00"), t("11:00"))); }
    @Test public void t03_endAtStartDoesNotOverlap() { assertFalse(slot("08:00", "10:00").overlaps(DayOfWeek.MONDAY, t("07:00"), t("08:00"))); }
    @Test public void t04_otherDayNeverOverlaps() { assertFalse(slot("08:00", "10:00").overlaps(DayOfWeek.TUESDAY, t("09:00"), t("09:30"))); }
    @Test public void t05_straddleOverlaps() { assertTrue(slot("08:00", "10:00").overlaps(DayOfWeek.MONDAY, t("07:30"), t("08:30"))); }

    // ---- Today's classes
    @Test public void t06_mondayHasThree() { assertEquals(3, ScheduleService.classesOn(TT, DayOfWeek.MONDAY, t("07:00")).size()); }
    @Test public void t07_firstInProgress() { assertEquals(TodayClass.Status.NOW, ScheduleService.classesOn(TT, DayOfWeek.MONDAY, t("09:00")).get(0).getStatus()); }
    @Test public void t08_othersLater() {
        List<TodayClass> l = ScheduleService.classesOn(TT, DayOfWeek.MONDAY, t("09:00"));
        assertEquals(TodayClass.Status.LATER, l.get(1).getStatus());
        assertEquals(TodayClass.Status.LATER, l.get(2).getStatus());
    }
    @Test public void t09_atEndIsFinished() { assertEquals(TodayClass.Status.DONE, ScheduleService.classesOn(TT, DayOfWeek.MONDAY, t("10:00")).get(0).getStatus()); }
    @Test public void t10_atStartInProgress() { assertEquals(TodayClass.Status.NOW, ScheduleService.classesOn(TT, DayOfWeek.MONDAY, t("08:00")).get(0).getStatus()); }
    @Test public void t11_timeOrder() {
        List<TodayClass> l = ScheduleService.classesOn(TT, DayOfWeek.WEDNESDAY, t("07:00"));
        for (int i = 1; i < l.size(); i++) assertTrue(l.get(i - 1).getStartText().compareTo(l.get(i).getStartText()) < 0);
    }
    @Test public void t12_sundayEmpty() { assertTrue(ScheduleService.classesOn(TT, DayOfWeek.SUNDAY, t("10:00")).isEmpty()); }

    // ---- Free rooms
    @Test public void t13_noneBeforeOpening() { assertTrue(ScheduleService.freeRooms(ROOMS, TT, DayOfWeek.MONDAY, t("06:59")).isEmpty()); }
    @Test public void t14_noneFromClosing() { assertTrue(ScheduleService.freeRooms(ROOMS, TT, DayOfWeek.MONDAY, t("18:00")).isEmpty()); }
    @Test public void t15_openAtSeven() { assertEquals(8, ScheduleService.freeRooms(ROOMS, TT, DayOfWeek.MONDAY, t("07:00")).size()); }
    @Test public void t16_monday0900() {
        List<FreeRoom> f = ScheduleService.freeRooms(ROOMS, TT, DayOfWeek.MONDAY, t("09:00"));
        assertEquals(7, f.size());
        assertEquals(null, room(f, "Block A 101"));
    }
    @Test public void t17_monday1100() {
        List<FreeRoom> f = ScheduleService.freeRooms(ROOMS, TT, DayOfWeek.MONDAY, t("11:00"));
        assertEquals(7, f.size());
        assertEquals(null, room(f, "Lab 2"));
    }
    @Test public void t18_thursday1400() {
        List<FreeRoom> f = ScheduleService.freeRooms(ROOMS, TT, DayOfWeek.THURSDAY, t("14:00"));
        assertEquals(7, f.size());
        assertEquals(null, room(f, "Block A 101"));
    }
    @Test public void t19_freeAtExactEnd() { assertTrue(room(ScheduleService.freeRooms(ROOMS, TT, DayOfWeek.MONDAY, t("10:00")), "Block A 101") != null); }
    @Test public void t20_freeUntilNextBooking() { assertEquals("10:30", room(ScheduleService.freeRooms(ROOMS, TT, DayOfWeek.MONDAY, t("09:00")), "Lab 2").getFreeUntil()); }
    @Test public void t21_closingTime() { assertEquals("closing time", room(ScheduleService.freeRooms(ROOMS, TT, DayOfWeek.MONDAY, t("09:00")), "Lecture Hall").getFreeUntil()); }

    // ---- Catalogue search
    @Test public void t22_blankReturnsAll() {
        assertEquals(5, ServiceCatalogue.search("").size());
        assertEquals(5, ServiceCatalogue.search(null).size());
    }
    @Test public void t23_results() {
        List<CampusService> r = ServiceCatalogue.search("results");
        assertEquals(1, r.size());
        assertEquals("gradebook", r.get(0).getId());
    }
    @Test public void t24_ignoresCase() { assertEquals("moodle", ServiceCatalogue.search("MOODLE").get(0).getId()); }
    @Test public void t25_books() { assertEquals("library", ServiceCatalogue.search("books").get(0).getId()); }
    @Test public void t26_allWordsMustMatch() {
        assertEquals(1, ServiceCatalogue.search("timetable rooms").size());
        assertEquals(0, ServiceCatalogue.search("timetable zebra").size());
    }
    @Test public void t27_unknown() { assertTrue(ServiceCatalogue.search("xylophone").isEmpty()); }

    // ---- Password hashing (production round count)
    @Test public void t28_correctVerifies() { assertTrue(PasswordHasher.verify("secret pass", PasswordHasher.hash("secret pass", 600_000))); }
    @Test public void t29_wrongRejected() { assertFalse(PasswordHasher.verify("wrong", PasswordHasher.hash("secret pass", 1000))); }
    @Test public void t30_salted() { assertNotEquals(PasswordHasher.hash("same", 1000), PasswordHasher.hash("same", 1000)); }
    @Test public void t31_format() {
        String[] p = PasswordHasher.hash("x", 600_000).split("\\$");
        assertEquals(4, p.length);
        assertEquals("pbkdf2", p[0]);
        assertEquals("600000", p[1]);
    }
    @Test public void t32_malformedRejected() {
        assertFalse(PasswordHasher.verify("x", "nonsense"));
        assertFalse(PasswordHasher.verify("x", "pbkdf2$abc$def$ghi"));
        assertFalse(PasswordHasher.verify("x", "pbkdf2$0$AAAA$AAAA"));
    }
    @Test public void t33_nulls() {
        assertFalse(PasswordHasher.verify(null, "pbkdf2$1$a$b"));
        assertFalse(PasswordHasher.verify("x", null));
    }

    // ---- Accounts (database-backed now)
    private static Optional<User> auth(String e, String p) { return app.accounts.authenticate(e, p, "127.0.0.1"); }
    @Test public void t34_student() { assertEquals(Role.STUDENT, auth("student@stride.test", "student123").get().getRole()); }
    @Test public void t35_lecturer() { assertEquals(Role.LECTURER, auth("lecturer@stride.test", "lecturer123").get().getRole()); }
    @Test public void t36_admin() { assertEquals(Role.ADMINISTRATOR, auth("admin@stride.test", "admin123").get().getRole()); }
    @Test public void t37_trimAndCase() { assertTrue(auth("  Student@Stride.TEST ", "student123").isPresent()); }
    @Test public void t38_wrongPassword() { assertFalse(auth("student@stride.test", "nope").isPresent()); }
    @Test public void t39_unknownEmail() { assertFalse(auth("nobody@stride.test", "student123").isPresent()); }
    @Test public void t40_nulls() { assertFalse(auth(null, null).isPresent()); }

    // ---- Sign-in throttle
    private static final class MovableClock extends Clock {
        Instant now = Instant.parse("2026-10-29T10:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
        @Override public Instant instant() { return now; }
    }
    @Test public void t41_fourDoNotLock() {
        LoginThrottle th = new LoginThrottle(5, Duration.ofMinutes(5), new MovableClock());
        for (int i = 0; i < 4; i++) th.recordFailure("a");
        assertTrue(th.remainingLock("a").isZero());
    }
    @Test public void t42_fifthLocks() {
        LoginThrottle th = new LoginThrottle(5, Duration.ofMinutes(5), new MovableClock());
        for (int i = 0; i < 5; i++) th.recordFailure("a");
        assertEquals(300, th.remainingLock("a").getSeconds());
    }
    @Test public void t43_perEmail() {
        LoginThrottle th = new LoginThrottle(5, Duration.ofMinutes(5), new MovableClock());
        for (int i = 0; i < 5; i++) th.recordFailure("a");
        assertTrue(th.remainingLock("b").isZero());
    }
    @Test public void t44_expires() {
        MovableClock c = new MovableClock();
        LoginThrottle th = new LoginThrottle(5, Duration.ofMinutes(5), c);
        for (int i = 0; i < 5; i++) th.recordFailure("a");
        c.now = c.now.plusSeconds(301);
        assertTrue(th.remainingLock("a").isZero());
    }
    @Test public void t45_resetClearsFailures() {
        LoginThrottle th = new LoginThrottle(5, Duration.ofMinutes(5), new MovableClock());
        for (int i = 0; i < 4; i++) th.recordFailure("a");
        th.reset("a");
        th.recordFailure("a");
        assertTrue(th.remainingLock("a").isZero());
    }
    @Test public void t46_resetClearsLock() {
        LoginThrottle th = new LoginThrottle(5, Duration.ofMinutes(5), new MovableClock());
        for (int i = 0; i < 5; i++) th.recordFailure("a");
        th.reset("a");
        assertTrue(th.remainingLock("a").isZero());
    }

    // ---- Sample-data integrity (now checked in the seeded database)
    @Test public void t47_attendedNotAboveTotal() {
        User s = app.users.findByEmail("student@stride.test").get();
        List<AttendanceRecord> r = app.attendance.summaryFor(s, app.modules.forStudent(s.getUserId()));
        assertEquals(5, r.size());
        for (AttendanceRecord a : r) assertTrue(a.getAttended() <= a.getTotal());
    }
    @Test public void t48_noDoubleBooking() {
        List<TimetableEntry> all = app.timetable.all();
        for (int i = 0; i < all.size(); i++)
            for (int j = i + 1; j < all.size(); j++)
                if (all.get(i).getRoomId() == all.get(j).getRoomId()) assertFalse(all.get(i).overlaps(all.get(j)));
    }
    @Test public void t49_everyRoomExists() {
        Set<String> names = new HashSet<>();
        for (Room r : app.campus.rooms()) names.add(r.getName());
        for (TimetableEntry e : app.timetable.all()) assertTrue(names.contains(e.getRoom()));
    }
}
