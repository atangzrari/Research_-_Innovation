package com.stride.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** A time-limited attendance session opened by a lecturer for one class (RQ15). */
public class AttendanceSession {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);

    private final int id;
    private final int moduleId;
    private final String moduleCode;
    private final String moduleName;
    private final Integer roomId;
    private final String roomName;
    private final Integer timetableEntryId;
    private final int lecturerId;
    private final LocalDate sessionDate;
    private final LocalDateTime startsAt;
    private final LocalDateTime openedAt;
    private final LocalDateTime closesAt;
    private final LocalDateTime closedAt;
    private final String secret;
    private int checkedIn;
    private int enrolled;
    private int flagged;

    public AttendanceSession(int id, int moduleId, String moduleCode, String moduleName, Integer roomId,
                             String roomName, Integer timetableEntryId, int lecturerId, LocalDate sessionDate,
                             LocalDateTime startsAt, LocalDateTime openedAt, LocalDateTime closesAt,
                             LocalDateTime closedAt, String secret) {
        this.id = id;
        this.moduleId = moduleId;
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.roomId = roomId;
        this.roomName = roomName;
        this.timetableEntryId = timetableEntryId;
        this.lecturerId = lecturerId;
        this.sessionDate = sessionDate;
        this.startsAt = startsAt;
        this.openedAt = openedAt;
        this.closesAt = closesAt;
        this.closedAt = closedAt;
        this.secret = secret;
    }

    public int getId() { return id; }
    public int getModuleId() { return moduleId; }
    public String getModuleCode() { return moduleCode; }
    public String getModuleName() { return moduleName; }
    public Integer getRoomId() { return roomId; }
    public String getRoomName() { return roomName == null ? "No room" : roomName; }
    public Integer getTimetableEntryId() { return timetableEntryId; }
    public int getLecturerId() { return lecturerId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getClosesAt() { return closesAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    /** The per-session key for the rotating code. Never shown to students. */
    public String getSecret() { return secret; }

    public boolean isOpenAt(LocalDateTime now) {
        return closedAt == null && now.isBefore(closesAt) && !now.isBefore(openedAt);
    }

    /** When check-in actually stopped: the manual close, or the automatic closing time. */
    public LocalDateTime getEndedAt() {
        return closedAt != null && closedAt.isBefore(closesAt) ? closedAt : closesAt;
    }

    public String getDateText() { return DAY.format(sessionDate); }
    public String getStartText() { return HM.format(startsAt); }
    public String getClosesText() { return HM.format(closesAt); }

    public int getCheckedIn() { return checkedIn; }
    public void setCheckedIn(int checkedIn) { this.checkedIn = checkedIn; }
    public int getEnrolled() { return enrolled; }
    public void setEnrolled(int enrolled) { this.enrolled = enrolled; }
    public int getFlagged() { return flagged; }
    public void setFlagged(int flagged) { this.flagged = flagged; }
}
