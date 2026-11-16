package com.stride.service;

import com.stride.dao.CourseworkDao;
import com.stride.dao.TimetableDao;
import com.stride.model.Assessment;
import com.stride.model.TimetableEntry;
import com.stride.model.User;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rule-based study schedule (RQ23). Uses only the student's own timetable and assessment dates.
 * <ol>
 *   <li>Take assessments due in the next three weeks, earliest deadline first.</li>
 *   <li>Free study hours are the whole hours in the study window that do not overlap a class.</li>
 *   <li>Give each assessment its estimated hours, at most two hours a day per assessment and at
 *       most the daily limit overall, from today up to the day before it is due.</li>
 *   <li>Warn when time runs short and when two deadlines fall within 48 hours (collision).</li>
 * </ol>
 */
public class StudyPlanService {

    public static final int HORIZON_DAYS = 21;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH);
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    /** The student's choices for planning. */
    public static final class Preferences {
        public final LocalTime from;
        public final LocalTime to;
        public final int maxHoursPerDay;
        public final boolean sundays;

        public Preferences(LocalTime from, LocalTime to, int maxHoursPerDay, boolean sundays) {
            this.from = from;
            this.to = to;
            this.maxHoursPerDay = Math.max(1, Math.min(8, maxHoursPerDay));
            this.sundays = sundays;
        }

        public static Preferences defaults() {
            return new Preferences(LocalTime.of(8, 0), LocalTime.of(20, 0), 3, false);
        }

        public String getFromText() { return HM.format(from); }
        public String getToText() { return HM.format(to); }
        public int getMaxHoursPerDay() { return maxHoursPerDay; }
        public boolean isSundays() { return sundays; }
    }

    /** One hour in the plan: either a class or a study block. */
    public static final class Block {
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final String title;
        private final String moduleCode;
        private final boolean study;

        Block(LocalDateTime start, LocalDateTime end, String title, String moduleCode, boolean study) {
            this.start = start;
            this.end = end;
            this.title = title;
            this.moduleCode = moduleCode;
            this.study = study;
        }

        public LocalDateTime getStart() { return start; }
        public LocalDateTime getEnd() { return end; }
        public String getStartText() { return HM.format(start); }
        public String getEndText() { return HM.format(end); }
        public String getTitle() { return title; }
        public String getModuleCode() { return moduleCode; }
        public boolean isStudy() { return study; }
    }

    public static final class Day {
        private final LocalDate date;
        private final List<Block> blocks = new ArrayList<>();

        Day(LocalDate date) {
            this.date = date;
        }

        public LocalDate getDate() { return date; }
        public String getLabel() { return DAY.format(date); }
        public List<Block> getBlocks() { return blocks; }
        public int getStudyHours() {
            int n = 0;
            for (Block b : blocks) if (b.study) n++;
            return n;
        }
    }

    /** How one assessment fared in the plan. */
    public static final class Item {
        private final Assessment assessment;
        private final int planned;
        private final long daysLeft;

        Item(Assessment assessment, int planned, long daysLeft) {
            this.assessment = assessment;
            this.planned = planned;
            this.daysLeft = daysLeft;
        }

        public Assessment getAssessment() { return assessment; }
        public int getPlanned() { return planned; }
        public int getNeeded() { return assessment.getEstHours(); }
        public int getShortfall() { return Math.max(0, assessment.getEstHours() - planned); }
        public long getDaysLeft() { return daysLeft; }
        public int getPercent() {
            return assessment.getEstHours() == 0 ? 100 : Math.min(100, Math.round(100f * planned / assessment.getEstHours()));
        }
    }

    public static final class Plan {
        private final List<Day> days;
        private final List<Item> items;
        private final List<String> warnings;
        private final Preferences preferences;

        Plan(List<Day> days, List<Item> items, List<String> warnings, Preferences preferences) {
            this.days = days;
            this.items = items;
            this.warnings = warnings;
            this.preferences = preferences;
        }

        public List<Day> getDays() { return days; }
        public List<Item> getItems() { return items; }
        public List<String> getWarnings() { return warnings; }
        public Preferences getPreferences() { return preferences; }
        public int getTotalHours() {
            int n = 0;
            for (Day d : days) n += d.getStudyHours();
            return n;
        }
        /** Days that have at least one block, for display. */
        public List<Day> getBusyDays() {
            List<Day> out = new ArrayList<>();
            for (Day d : days) if (!d.blocks.isEmpty()) out.add(d);
            return out;
        }
    }

    private final CourseworkDao coursework;
    private final TimetableDao timetable;

    public StudyPlanService(CourseworkDao coursework, TimetableDao timetable) {
        this.coursework = coursework;
        this.timetable = timetable;
    }

    public List<Assessment> upcoming(User student, LocalDateTime now, int days) {
        return coursework.forStudentBetween(student.getUserId(), now, now.plusDays(days));
    }

    public Plan plan(User student, LocalDateTime now, Preferences prefs) {
        List<Assessment> due = coursework.forStudentBetween(student.getUserId(), now, now.plusDays(HORIZON_DAYS));
        return plan(due, timetable.forStudent(student.getUserId()), now, prefs);
    }

    /** The planning rule itself, free of the database so that it can be unit-tested. */
    public static Plan plan(List<Assessment> assessments, List<TimetableEntry> classes, LocalDateTime now,
                            Preferences prefs) {
        List<Assessment> due = new ArrayList<>(assessments);
        due.sort(Comparator.comparing(Assessment::getDueAt));
        LocalDate first = now.toLocalDate();
        LocalDate last = first;
        for (Assessment a : due) {
            if (a.getDueAt().toLocalDate().isAfter(last)) last = a.getDueAt().toLocalDate();
        }
        Map<LocalDate, Day> days = new LinkedHashMap<>();
        Map<LocalDate, List<LocalDateTime>> freeHours = new LinkedHashMap<>();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            Day day = new Day(d);
            days.put(d, day);
            for (TimetableEntry e : classes) {
                if (e.getDay() == d.getDayOfWeek()) {
                    day.blocks.add(new Block(d.atTime(e.getStart()), d.atTime(e.getEnd()),
                            e.getModuleName() + " (class, " + e.getRoom() + ")", e.getModuleCode(), false));
                }
            }
            List<LocalDateTime> hours = new ArrayList<>();
            if (d.getDayOfWeek() != DayOfWeek.SUNDAY || prefs.sundays) {
                for (LocalTime t = prefs.from; !t.plusHours(1).isAfter(prefs.to); t = t.plusHours(1)) {
                    LocalDateTime start = d.atTime(t);
                    if (start.isBefore(now)) continue;
                    boolean clash = false;
                    for (TimetableEntry e : classes) {
                        if (e.overlaps(d.getDayOfWeek(), t, t.plusHours(1))) {
                            clash = true;
                            break;
                        }
                    }
                    if (!clash) hours.add(start);
                }
            }
            freeHours.put(d, hours);
        }

        List<Item> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (Assessment a : due) {
            int remaining = a.getEstHours();
            int planned = 0;
            LocalDateTime dueAt = a.getDueAt();
            // Round-robin over the days, one hour per day per pass, so the work is spread out;
            // a second pass adds a second hour a day. Never more than two a day per assessment.
            boolean progress = true;
            while (remaining > 0 && progress) {
                progress = false;
                for (Map.Entry<LocalDate, List<LocalDateTime>> entry : freeHours.entrySet()) {
                    if (remaining == 0) break;
                    LocalDate d = entry.getKey();
                    if (!d.isBefore(dueAt.toLocalDate())) break;
                    Day day = days.get(d);
                    if (day.getStudyHours() >= prefs.maxHoursPerDay) continue;
                    int already = 0;
                    for (Block b : day.blocks) if (b.study && a.getTitle().equals(b.title)) already++;
                    if (already >= 2) continue;
                    List<LocalDateTime> hours = entry.getValue();
                    if (hours.isEmpty()) continue;
                    LocalDateTime start = hours.remove(0);
                    day.blocks.add(new Block(start, start.plusHours(1), a.getTitle(), a.getModuleCode(), true));
                    remaining--;
                    planned++;
                    progress = true;
                }
            }
            long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), dueAt.toLocalDate());
            items.add(new Item(a, planned, daysLeft));
            if (remaining > 0) {
                warnings.add("Not enough free time before " + a.getModuleCode() + " " + a.getTitle() + " ("
                        + a.getDueText() + "): " + remaining + (remaining == 1 ? " hour" : " hours")
                        + " short. Start now, raise your daily limit, or ask your lecturer for advice.");
            }
        }
        for (int i = 0; i + 1 < due.size(); i++) {
            Assessment a = due.get(i);
            Assessment b = due.get(i + 1);
            if (Duration.between(a.getDueAt(), b.getDueAt()).toHours() < 48) {
                warnings.add("Deadline collision: " + a.getModuleCode() + " " + a.getTitle() + " and "
                        + b.getModuleCode() + " " + b.getTitle() + " are due within 48 hours of each other.");
            }
        }
        for (Day d : days.values()) {
            d.blocks.sort(Comparator.comparing(Block::getStart));
        }
        return new Plan(new ArrayList<>(days.values()), items, warnings, prefs);
    }
}
