package com.stride.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** A person with a Stride account. Holds no password data, so it is safe to keep in the session. */
public class User implements Serializable {

    private static final long serialVersionUID = 2L;

    private final int userId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final Role role;
    private final String studentNumber;
    private final boolean active;
    private final boolean mustChangePassword;
    private final LocalDateTime lastLoginAt;

    public User(int userId, String firstName, String lastName, String email, Role role) {
        this(userId, firstName, lastName, email, role, null, true, false, null);
    }

    public User(int userId, String firstName, String lastName, String email, Role role,
                String studentNumber, boolean active, boolean mustChangePassword, LocalDateTime lastLoginAt) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.studentNumber = studentNumber;
        this.active = active;
        this.mustChangePassword = mustChangePassword;
        this.lastLoginAt = lastLoginAt;
    }

    public int getUserId() { return userId; }
    public int getId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    /** Two capital letters for the round avatar, e.g. "DS". */
    public String getInitials() {
        String f = firstName.isEmpty() ? "" : firstName.substring(0, 1);
        String l = lastName.isEmpty() ? "" : lastName.substring(0, 1);
        return (f + l).toUpperCase();
    }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public String getStudentNumber() { return studentNumber; }
    public boolean isActive() { return active; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }

    public boolean isStudent() { return role == Role.STUDENT; }
    public boolean isLecturer() { return role == Role.LECTURER; }
    public boolean isAdministrator() { return role == Role.ADMINISTRATOR; }
    /** Lecturers and administrators may see management reports. */
    public boolean isStaff() { return role != Role.STUDENT; }

    public User withMustChangePassword(boolean flag) {
        return new User(userId, firstName, lastName, email, role, studentNumber, active, flag, lastLoginAt);
    }
}
