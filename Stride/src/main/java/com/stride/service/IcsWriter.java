package com.stride.service;

import com.stride.model.TimetableEntry;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/** Writes an iCalendar (.ics) file so a timetable or study plan can go into a phone's calendar. */
public final class IcsWriter {

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private final StringBuilder out = new StringBuilder();
    private final String zone;
    private int counter;

    public IcsWriter(String calendarName, String zone) {
        this.zone = zone;
        line("BEGIN:VCALENDAR");
        line("VERSION:2.0");
        line("PRODID:-//Stride//Botswana Accountancy College//EN");
        line("CALSCALE:GREGORIAN");
        line("X-WR-CALNAME:" + escape(calendarName));
        line("X-WR-TIMEZONE:" + zone);
    }

    /** A weekly repeating class, starting from its next occurrence. */
    public IcsWriter weekly(TimetableEntry e, LocalDate from, int weeks) {
        LocalDate first = from.with(TemporalAdjusters.nextOrSame(e.getDay()));
        return event(first.atTime(e.getStart()), first.atTime(e.getEnd()),
                e.getModuleCode() + " " + e.getModuleName(), "Room: " + e.getRoom(), e.getRoom(),
                "RRULE:FREQ=WEEKLY;COUNT=" + weeks + ";BYDAY=" + byDay(e.getDay()));
    }

    public IcsWriter event(LocalDateTime start, LocalDateTime end, String summary, String description,
                           String location, String rule) {
        line("BEGIN:VEVENT");
        line("UID:stride-" + (++counter) + "-" + LOCAL.format(start) + "@stride");
        line("DTSTAMP:" + LOCAL.format(start) + "Z");
        line("DTSTART;TZID=" + zone + ":" + LOCAL.format(start));
        line("DTEND;TZID=" + zone + ":" + LOCAL.format(end));
        line("SUMMARY:" + escape(summary));
        if (description != null) line("DESCRIPTION:" + escape(description));
        if (location != null) line("LOCATION:" + escape(location));
        if (rule != null) line(rule);
        line("END:VEVENT");
        return this;
    }

    public String build() {
        line("END:VCALENDAR");
        return out.toString();
    }

    public static String forTimetable(List<TimetableEntry> entries, LocalDate from, String zone) {
        IcsWriter w = new IcsWriter("Stride timetable", zone);
        for (TimetableEntry e : entries) {
            w.weekly(e, from, 14);
        }
        return w.build();
    }

    private static String byDay(DayOfWeek d) {
        return d.name().substring(0, 2).toUpperCase(Locale.ROOT);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    private void line(String s) {
        out.append(s).append("\r\n");
    }
}
