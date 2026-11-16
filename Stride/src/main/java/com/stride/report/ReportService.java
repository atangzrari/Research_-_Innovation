package com.stride.report;

import com.stride.dao.AttendanceDao;
import com.stride.dao.CampusDao;
import com.stride.dao.ModuleDao;
import com.stride.dao.TimetableDao;
import com.stride.model.CheckIn;
import com.stride.model.ConfidenceLevel;
import com.stride.model.Module;
import com.stride.model.Room;
import com.stride.model.TimetableEntry;
import com.stride.model.User;
import com.stride.service.ServiceException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The five management reports (RP01-RP05). Lecturers see their own modules; administrators see
 * every module. Each report has a chart, a table, and a CSV download for a spreadsheet.
 */
public class ReportService {

    public static final class Info {
        private final String id;
        private final String title;
        private final String purpose;

        Info(String id, String title, String purpose) {
            this.id = id;
            this.title = title;
            this.purpose = purpose;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getPurpose() { return purpose; }
    }

    public static final List<Info> CATALOGUE = List.of(
            new Info("rp01", "Attendance by module", "Average attendance in each module, against the at-risk line."),
            new Info("rp02", "Students at risk", "Students whose attendance in a module is below the at-risk line."),
            new Info("rp03", "Check-in assurance", "How strongly each check-in was verified, and how many were flagged."),
            new Info("rp04", "Room use", "Weekly booked hours of each room against its opening hours."),
            new Info("rp05", "Weekly attendance trend", "Attendance week by week over the last eight weeks."));

    private static final DateTimeFormatter WEEK = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private final AttendanceDao attendance;
    private final ModuleDao modules;
    private final TimetableDao timetable;
    private final CampusDao campus;
    private final Clock clock;
    private final ZoneId zone;
    private final int riskPercent;
    private final LocalTime open;
    private final LocalTime close;

    public ReportService(AttendanceDao attendance, ModuleDao modules, TimetableDao timetable, CampusDao campus,
                         Clock clock, ZoneId zone, int riskPercent, LocalTime open, LocalTime close) {
        this.attendance = attendance;
        this.modules = modules;
        this.timetable = timetable;
        this.campus = campus;
        this.clock = clock;
        this.zone = zone;
        this.riskPercent = riskPercent;
        this.open = open;
        this.close = close;
    }

    public Report build(String id, User viewer) {
        if (viewer == null || viewer.isStudent()) {
            throw new ServiceException("Reports are for lecturers and administrators.");
        }
        LocalDateTime now = LocalDateTime.now(clock.withZone(zone));
        List<Module> scope = viewer.isAdministrator() ? modules.all() : modules.forLecturer(viewer.getUserId());
        Data d = new Data(scope, now);
        Report r;
        switch (id == null ? "" : id) {
            case "rp01": r = byModule(d); break;
            case "rp02": r = atRisk(d); break;
            case "rp03": r = assurance(d); break;
            case "rp04": r = rooms(); break;
            case "rp05": r = trend(d, now); break;
            default: throw new ServiceException("There is no report called " + id + ".");
        }
        r.setScope(viewer.isAdministrator() ? "All modules" : scope.isEmpty() ? "You teach no modules yet"
                : "Your modules: " + String.join(", ", codes(scope)));
        return r;
    }

    /** Loads everything a report needs once. */
    private final class Data {
        final List<Module> scope;
        final Map<Integer, List<AttendanceDao.HeldSession>> held = new HashMap<>();
        final Map<Integer, Map<Integer, AttendanceDao.Mark>> marksBySession = new HashMap<>();
        final Map<Integer, List<User>> students = new HashMap<>();

        Data(List<Module> scope, LocalDateTime now) {
            this.scope = scope;
            Set<Integer> ids = new HashSet<>();
            for (Module m : scope) {
                ids.add(m.getId());
                students.put(m.getId(), modules.students(m.getId()));
            }
            for (AttendanceDao.HeldSession h : attendance.held(now)) {
                if (ids.contains(h.moduleId)) held.computeIfAbsent(h.moduleId, k -> new ArrayList<>()).add(h);
            }
            for (AttendanceDao.Mark m : attendance.marks()) {
                marksBySession.computeIfAbsent(m.sessionId, k -> new HashMap<>()).put(m.studentId, m);
            }
        }

        /** {attended, counted} for one student in one module. Excused absences are not counted. */
        int[] tally(int moduleId, int studentId, List<AttendanceDao.HeldSession> within) {
            int attended = 0;
            int counted = 0;
            for (AttendanceDao.HeldSession h : within) {
                AttendanceDao.Mark mk = marksBySession.getOrDefault(h.id, Map.of()).get(studentId);
                if (mk != null && mk.status == CheckIn.Status.EXCUSED) continue;
                counted++;
                if (mk != null && mk.status.isAttended()) attended++;
            }
            return new int[]{attended, counted};
        }

        List<AttendanceDao.HeldSession> heldFor(int moduleId) {
            return held.getOrDefault(moduleId, List.of());
        }
    }

    private Report byModule(Data d) {
        Report r = new Report("rp01", CATALOGUE.get(0).getTitle(), CATALOGUE.get(0).getPurpose(),
                List.of("Module", "Name", "Lecturer", "Students", "Classes held", "Average attendance", "Students at risk"));
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (Module m : d.scope) {
            List<AttendanceDao.HeldSession> held = d.heldFor(m.getId());
            int att = 0;
            int cnt = 0;
            int risk = 0;
            for (User s : d.students.get(m.getId())) {
                int[] t = d.tally(m.getId(), s.getUserId(), held);
                att += t[0];
                cnt += t[1];
                if (t[1] > 0 && Math.round(100f * t[0] / t[1]) < riskPercent) risk++;
            }
            double avg = cnt == 0 ? 0 : 100.0 * att / cnt;
            r.row(m.getCode(), m.getName(), m.getLecturerId() == null ? "None" : m.getLecturerName(),
                    d.students.get(m.getId()).size(), held.size(), Chart.format(avg) + "%", risk);
            labels.add(m.getCode());
            values.add(avg);
        }
        r.setChart(Chart.bars("Average attendance by module", "%", labels, values, 100, (double) riskPercent));
        r.note("Average attendance = classes attended / classes held, over every enrolled student. Excused absences are left out.");
        r.note("At risk = below " + riskPercent + "% in that module.");
        return r;
    }

    private Report atRisk(Data d) {
        Report r = new Report("rp02", CATALOGUE.get(1).getTitle(), CATALOGUE.get(1).getPurpose(),
                List.of("Student", "Student number", "Module", "Attended", "Held", "Attendance"));
        Map<String, Integer> perModule = new LinkedHashMap<>();
        List<Object[]> found = new ArrayList<>();
        for (Module m : d.scope) {
            perModule.put(m.getCode(), 0);
            for (User s : d.students.get(m.getId())) {
                int[] t = d.tally(m.getId(), s.getUserId(), d.heldFor(m.getId()));
                if (t[1] == 0) continue;
                int pct = Math.round(100f * t[0] / t[1]);
                if (pct < riskPercent) {
                    found.add(new Object[]{s.getFullName(), s.getStudentNumber(), m.getCode(), t[0], t[1], pct});
                    perModule.merge(m.getCode(), 1, Integer::sum);
                }
            }
        }
        found.sort((a, b) -> Integer.compare((Integer) a[5], (Integer) b[5]));
        for (Object[] f : found) {
            r.row(f[0], f[1], f[2], f[3], f[4], f[5] + "%");
        }
        List<Double> counts = new ArrayList<>();
        double max = 1;
        for (int v : perModule.values()) {
            counts.add((double) v);
            max = Math.max(max, v);
        }
        r.setChart(Chart.bars("Students at risk, per module", "", new ArrayList<>(perModule.keySet()), counts,
                Math.ceil(max / 4.0) * 4, null));
        r.note(found.isEmpty() ? "No student is below " + riskPercent + "% in any module."
                : found.size() + " student-module pairs are below " + riskPercent + "%. Lowest first.");
        r.note("Use this list to contact students early, before attendance affects their results.");
        return r;
    }

    private Report assurance(Data d) {
        Report r = new Report("rp03", CATALOGUE.get(2).getTitle(), CATALOGUE.get(2).getPurpose(),
                List.of("Module", "Check-ins", "High", "Medium", "Low", "Flagged", "By QR", "Off campus network"));
        int[] totals = new int[4];
        for (Module m : d.scope) {
            int n = 0, hi = 0, med = 0, low = 0, flag = 0, qr = 0, off = 0;
            for (AttendanceDao.HeldSession h : d.heldFor(m.getId())) {
                for (AttendanceDao.Mark mk : d.marksBySession.getOrDefault(h.id, Map.of()).values()) {
                    if (mk.method == CheckIn.Method.MANUAL) continue;
                    n++;
                    switch (ConfidenceLevel.of(mk.confidence)) {
                        case HIGH: hi++; break;
                        case MEDIUM: med++; break;
                        default: low++; break;
                    }
                    if (mk.flagged) flag++;
                    if (mk.method == CheckIn.Method.QR) qr++;
                    if (mk.factors != null && mk.factors.contains("Off campus network")) off++;
                }
            }
            r.row(m.getCode(), n, hi, med, low, flag, n == 0 ? "0%" : Math.round(100f * qr / n) + "%", off);
            totals[0] += hi;
            totals[1] += med;
            totals[2] += low;
            totals[3] += flag;
        }
        int all = totals[0] + totals[1] + totals[2];
        List<Double> pct = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            pct.add(all == 0 ? 0.0 : 100.0 * totals[i] / all);
        }
        r.setChart(Chart.bars("Share of check-ins by confidence", "%",
                List.of("High", "Medium", "Low", "Flagged"), pct, 100, null));
        r.note("Confidence combines independent signals: the rotating code or QR, the campus network and the device (RQ17).");
        r.note("High = 80 or more, Medium = 50 to 79, Low = below 50. Flagged check-ins wait for the lecturer to accept or change them.");
        return r;
    }

    private Report rooms() {
        Report r = new Report("rp04", CATALOGUE.get(3).getTitle(), CATALOGUE.get(3).getPurpose(),
                List.of("Room", "Building", "Seats", "Booked hours a week", "Open hours a week", "Use"));
        double openHours = Duration.between(open, close).toMinutes() / 60.0 * 6;   // Monday to Saturday
        Map<String, Double> booked = new LinkedHashMap<>();
        for (Room room : campus.rooms()) booked.put(room.getName(), 0.0);
        for (TimetableEntry e : timetable.all()) {
            booked.merge(e.getRoom(), Duration.between(e.getStart(), e.getEnd()).toMinutes() / 60.0, Double::sum);
        }
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (Room room : campus.rooms()) {
            double h = booked.getOrDefault(room.getName(), 0.0);
            double use = 100.0 * h / openHours;
            r.row(room.getName(), room.getBuilding(), room.getCapacity(), Chart.format(h), Chart.format(openHours),
                    Chart.format(use) + "%");
            labels.add(room.getName());
            values.add(use);
        }
        r.setChart(Chart.bars("Share of opening hours booked", "%", labels, values, 100, null));
        r.note("Open hours = " + open + " to " + close + ", Monday to Saturday. Booked hours come from the published timetable.");
        r.note("Rooms with low use are good candidates for study space; the free-room finder already lists them.");
        return r;
    }

    private Report trend(Data d, LocalDateTime now) {
        Report r = new Report("rp05", CATALOGUE.get(4).getTitle(), CATALOGUE.get(4).getPurpose(),
                List.of("Week starting", "Classes held", "Attended", "Expected", "Attendance"));
        LocalDate thisWeek = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (int w = 7; w >= 0; w--) {
            LocalDate start = thisWeek.minusWeeks(w);
            LocalDate end = start.plusDays(7);
            int sessions = 0, att = 0, cnt = 0;
            for (Module m : d.scope) {
                List<AttendanceDao.HeldSession> inWeek = new ArrayList<>();
                for (AttendanceDao.HeldSession h : d.heldFor(m.getId())) {
                    if (!h.date.isBefore(start) && h.date.isBefore(end)) inWeek.add(h);
                }
                sessions += inWeek.size();
                for (User s : d.students.get(m.getId())) {
                    int[] t = d.tally(m.getId(), s.getUserId(), inWeek);
                    att += t[0];
                    cnt += t[1];
                }
            }
            if (sessions == 0) continue;
            double pct = cnt == 0 ? 0 : 100.0 * att / cnt;
            r.row(WEEK.format(start), sessions, att, cnt, Chart.format(pct) + "%");
            labels.add(WEEK.format(start));
            values.add(pct);
        }
        r.setChart(Chart.line("Attendance by week", "%", labels, values, 100, (double) riskPercent));
        r.note("Only weeks with at least one class held are shown.");
        return r;
    }

    private static List<String> codes(List<Module> list) {
        List<String> out = new ArrayList<>();
        for (Module m : list) out.add(m.getCode());
        return out;
    }
}
