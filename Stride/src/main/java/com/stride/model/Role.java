package com.stride.model;

import java.util.Locale;

public enum Role {
    STUDENT("Student"),
    LECTURER("Lecturer"),
    ADMINISTRATOR("Administrator");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Enum name as a property, so views can compare roles with a plain string. */
    public String getName() {
        return name();
    }

    /** Parses a role name; null or unknown text gives null. */
    public static Role parse(String text) {
        if (text == null) {
            return null;
        }
        try {
            return Role.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
