package com.stride.unit;

import com.stride.SampleTimetable;
import com.stride.TestApp;
import com.stride.controller.AssistantController;
import com.stride.config.App;
import com.stride.model.Announcement;
import com.stride.model.Assessment;
import com.stride.model.Module;
import com.stride.model.Role;
import com.stride.model.User;
import com.stride.report.Report;
import com.stride.report.ReportService;
import com.stride.service.AssistantService;
import com.stride.service.ServiceException;
import com.stride.service.StudyPlanService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Planner, assistant, reports, notices and administration on the seeded database. */
public class FeaturesTest {

    private static App app;
    private static User student;
    private static User lecturer;
    private static User admin;

    @BeforeClass
    public static void up() {
        app = TestApp.start();
        student = app.users.findByEmail("student@stride.test").get();
        lecturer = app.users.findByEmail("lecturer@stride.test").get();
        admin = app.users.findByEmail("admin@stride.test").get();
    }

    @AfterClass
    public static void down() {
        app.close();
    }

    // ------------------------------------------------------------ study planner (RQ23)

    private static Assessment a(String title, LocalDateTime due, int hours) {
        return new Assessment(1, 1, "CSE305", "Web", title, Assessment.Kind.ASSIGNMENT, due, 20, hours);
    }

    @Test
    public void plannerNeverBooksOverAClassAndStopsBeforeTheDueDay() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 26, 7, 0);   // Monday morning
        StudyPlanService.Plan p = StudyPlanService.plan(List.of(a("Essay", now.plusDays(4).withHour(12), 6)),
                SampleTimetable.ENTRIES, now, StudyPlanService.Preferences.defaults());
        int study = 0;
        for (StudyPlanService.Day d : p.getDays()) {
            assertTrue(d.getStudyHours() <= 3);
            for (StudyPlanService.Block b : d.getBlocks()) {
                if (!b.isStudy()) continue;
                study++;
                assertTrue(b.getStart().toLocalDate().isBefore(now.plusDays(4).toLocalDate()));
                for (var e : SampleTimetable.ENTRIES) {
                    assertFalse(e.overlaps(b.getStart().getDayOfWeek(), b.getStart().toLocalTime(), b.getEnd().toLocalTime()));
                }
            }
        }
        assertEquals(6, study);
        assertTrue(p.getWarnings().isEmpty());
    }

    @Test
    public void plannerWarnsWhenTimeIsShortAndOnCollisions() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 19, 0);
        StudyPlanService.Plan p = StudyPlanService.plan(List.of(a("Big", now.plusDays(1), 20), a("Other", now.plusDays(1).plusHours(3), 2)),
                List.of(), now, StudyPlanService.Preferences.defaults());
        assertTrue(p.getWarnings().stream().anyMatch(w -> w.startsWith("Not enough free time")));
        assertTrue(p.getWarnings().stream().anyMatch(w -> w.startsWith("Deadline collision")));
    }

    @Test
    public void seededPlanCoversEveryDeadline() {
        StudyPlanService.Plan p = app.planner.plan(student, app.now(), StudyPlanService.Preferences.defaults());
        assertEquals(5, p.getItems().size());
        for (StudyPlanService.Item i : p.getItems()) assertEquals(0, i.getShortfall());
    }

    // ------------------------------------------------------------ assistant (RQ25): fixed test queries

    private static AssistantService.Reply ask(String q) {
        return app.assistant.answer(q, AssistantController.context(app, student));
    }

    @Test
    public void assistantFixedQueries() {
        String[][] cases = {
                {"What is my next class?", "next-class"},
                {"what do i have today", "day"},
                {"Classes on Friday?", "day"},
                {"What is my attendance in CSE305?", "attendance"},
                {"which subject has my lowest attendance", "attendance"},
                {"When is my next deadline?", "deadlines"},
                {"any assignments due for networks?", "deadlines"},
                {"Is there a free room right now?", "free-rooms"},
                {"Where is Lab 2?", "where"},
                {"Any new notices?", "notices"},
                {"what's the latest news", "notices"},
                {"How do I check in?", "checkin"},
                {"hello", "help"},
                {"what is the capital of France", "help"},
        };
        for (String[] c : cases) assertEquals(c[0], c[1], ask(c[0]).getIntent());
    }

    @Test
    public void assistantAnswersFromRealData() {
        assertTrue(ask("What is my attendance in CSE305?").getText().contains("75%"));
        assertTrue(ask("which subject has my lowest attendance").getText().contains("CSE315"));
        assertTrue(ask("What is my next class?").getText().contains("Computer Systems Administration"));
        assertTrue(ask("When is my next deadline?").getText().contains("Servlet portfolio"));
        assertTrue(ask("Where is Lab 2?").getText().contains("Computer labs"));
    }

    // ------------------------------------------------------------ reports (RP01-RP05)

    @Test
    public void everyReportBuildsForStaffWithChartAndCsv() {
        for (ReportService.Info i : ReportService.CATALOGUE) {
            for (User u : List.of(lecturer, admin)) {
                Report r = app.reports.build(i.getId(), u);
                assertNotNull(i.getId(), r.getChart());
                assertFalse(i.getId(), r.getRows().isEmpty());
                assertTrue(r.toCsv().startsWith(String.join(",", r.getColumns())));
            }
        }
    }

    @Test
    public void lecturerSeesOnlyOwnModules() {
        Report r = app.reports.build("rp01", lecturer);
        assertEquals(2, r.getRows().size());
        assertEquals(app.modules.all().size(), app.reports.build("rp01", admin).getRows().size());
    }

    @Test
    public void studentsCannotSeeReports() {
        assertThrows(ServiceException.class, () -> app.reports.build("rp01", student));
    }

    // ------------------------------------------------------------ notices (RQ19)

    @Test
    public void lecturerPostsOnlyToOwnModule() {
        Module cse305 = app.modules.findByCode("CSE305").get();
        Module cse310 = app.modules.findByCode("CSE310").get();
        app.announcements.post(lecturer, "Room change", "Next class is in Lab 3.", cse305.getId(), false);
        assertThrows(ServiceException.class, () -> app.announcements.post(lecturer, "x", "y", cse310.getId(), false));
        assertThrows(ServiceException.class, () -> app.announcements.post(lecturer, "x", "y", null, false));
        assertThrows(ServiceException.class, () -> app.announcements.post(student, "x", "y", cse305.getId(), false));
        boolean seen = false;
        for (Announcement a : app.announcements.feed(student)) seen |= a.getTitle().equals("Room change");
        assertTrue(seen);
    }

    @Test
    public void markAllReadClearsUnread() {
        User s = app.users.findByEmail("lesedi.kgosi@stride.test").get();
        assertTrue(app.announcements.unread(s) > 0);
        app.announcements.markAllRead(s);
        assertEquals(0, app.announcements.unread(s));
    }

    // ------------------------------------------------------------ administration (RQ20-RQ22)

    @Test
    public void adminCreatesAccountThatMustChangePassword() {
        String temp = app.accounts.create(admin, "Neo", "Test", "neo.test@stride.test", Role.STUDENT, "CSE24-777");
        User u = app.accounts.authenticate("neo.test@stride.test", temp, "1").get();
        assertTrue(u.isMustChangePassword());
        app.accounts.changePassword(u, temp, "a much longer passphrase", "a much longer passphrase");
        assertFalse(app.accounts.find(u.getUserId()).get().isMustChangePassword());
        assertThrows(ServiceException.class, () -> app.accounts.create(admin, "A", "B", "neo.test@stride.test", Role.STUDENT, "X1"));
        assertThrows(ServiceException.class, () -> app.accounts.create(lecturer, "A", "B", "z@stride.test", Role.STUDENT, "X2"));
    }

    @Test
    public void deactivatedAccountCannotSignIn() {
        User u = app.users.findByEmail("kabo.seretse@stride.test").get();
        app.accounts.setActive(admin, u.getUserId(), false);
        assertFalse(app.accounts.authenticate("kabo.seretse@stride.test", "student123", "1").isPresent());
        assertThrows(ServiceException.class, () -> app.accounts.setActive(admin, admin.getUserId(), false));
        app.accounts.setActive(admin, u.getUserId(), true);
    }

    @Test
    public void timetableClashesAreRejected() {
        Module cse310 = app.modules.findByCode("CSE310").get();
        int lab2 = app.campus.rooms().stream().filter(r -> r.getName().equals("Lab 2")).findFirst().get().getId();
        int hall = app.campus.rooms().stream().filter(r -> r.getName().equals("Lecture Hall")).findFirst().get().getId();
        List<String> p = app.curriculum.clashes(cse310.getId(), lab2, DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0));
        assertTrue(p.toString(), p.stream().anyMatch(s -> s.contains("Lab 2 is already booked")));
        assertTrue(p.toString(), p.stream().anyMatch(s -> s.contains("students are also in CSE305")));
        assertTrue(app.curriculum.clashes(cse310.getId(), hall, DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(17, 0)).isEmpty());
        assertFalse(app.curriculum.clashes(cse310.getId(), hall, DayOfWeek.MONDAY, LocalTime.of(17, 0), LocalTime.of(19, 0)).isEmpty());
        assertThrows(ServiceException.class, () -> app.curriculum.addSlot(admin, cse310.getId(), lab2, DayOfWeek.MONDAY,
                LocalTime.of(11, 0), LocalTime.of(12, 0)));
    }

    @Test
    public void enrolByNumberAndEmail() {
        int id = app.curriculum.createModule(admin, "CSE399", "Test module", null, null);
        List<String> r = app.curriculum.enrolMany(admin, id, "CSE24-901, lesedi.kgosi@stride.test\nnobody\nCSE24-901");
        assertTrue(r.get(0), r.get(0).startsWith("2 students"));
        assertEquals(2, app.curriculum.students(id).size());
    }

    @Test
    public void onlySafeLinksAreStored() {
        Module m = app.modules.findByCode("CSE305").get();
        assertThrows(ServiceException.class, () -> app.curriculum.addResource(lecturer, m, "x", "javascript:alert(1)", null));
        app.curriculum.addResource(lecturer, m, "Docs", "https://example.org/docs", "Reference");
    }

    @Test
    public void lecturersCannotEditOtherModules() {
        Module m = app.modules.findByCode("CSE310").get();
        assertThrows(ServiceException.class, () -> app.curriculum.addAssessment(lecturer, m, "x", Assessment.Kind.TEST,
                app.now().plusDays(3), 10, 2));
    }
}
