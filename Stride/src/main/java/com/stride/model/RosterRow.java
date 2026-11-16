package com.stride.model;

/** A student enrolled in a session's module, with their check-in if there is one. */
public class RosterRow {
    private final User student;
    private final CheckIn checkIn;

    public RosterRow(User student, CheckIn checkIn) {
        this.student = student;
        this.checkIn = checkIn;
    }

    public User getStudent() { return student; }
    public CheckIn getCheckIn() { return checkIn; }
    public boolean isCheckedIn() { return checkIn != null && checkIn.getStatus().isAttended(); }
    public String getStatusLabel() { return checkIn == null ? "Not checked in" : checkIn.getStatus().getLabel(); }
}
