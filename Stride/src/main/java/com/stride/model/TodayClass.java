package com.stride.model;

import java.time.format.DateTimeFormatter;

/** A timetable entry as shown on today's list, with where it is relative to "now". */
public class TodayClass {

    public enum Status { LATER, NOW, DONE }

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final TimetableEntry entry;
    private final Status status;

    public TodayClass(TimetableEntry entry, Status status) {
        this.entry = entry;
        this.status = status;
    }

    public TimetableEntry getEntry() { return entry; }
    public int getEntryId() { return entry.getId(); }
    public String getModuleCode() { return entry.getModuleCode(); }
    public String getModuleName() { return entry.getModuleName(); }
    public String getRoom() { return entry.getRoom(); }
    public String getStartText() { return HM.format(entry.getStart()); }
    public String getEndText() { return HM.format(entry.getEnd()); }
    public Status getStatus() { return status; }
    public boolean isNow() { return status == Status.NOW; }
    public boolean isDone() { return status == Status.DONE; }

    public String getStatusLabel() {
        switch (status) {
            case NOW: return "In progress";
            case DONE: return "Finished";
            default: return "Later today";
        }
    }
}
