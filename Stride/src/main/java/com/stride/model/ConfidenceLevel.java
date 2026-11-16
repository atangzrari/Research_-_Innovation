package com.stride.model;

/** Bands of the multi-factor attendance confidence score (RQ17). */
public enum ConfidenceLevel {
    HIGH("High"), MEDIUM("Medium"), LOW("Low");

    private final String label;

    ConfidenceLevel(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    public static ConfidenceLevel of(int score) {
        if (score >= 80) return HIGH;
        if (score >= 50) return MEDIUM;
        return LOW;
    }
}
