package com.stride.service;

import com.stride.model.AttendanceSession;
import com.stride.model.CheckIn;
import com.stride.model.ConfidenceLevel;

import java.util.Collections;
import java.util.List;

/** What happened when a student tried to check in: shown to them as plain words. */
public final class CheckInResult {

    private final boolean ok;
    private final String message;
    private final AttendanceSession session;
    private final CheckIn.Status status;
    private final int confidence;
    private final List<String> factors;
    private final boolean flagged;

    private CheckInResult(boolean ok, String message, AttendanceSession session, CheckIn.Status status,
                          int confidence, List<String> factors, boolean flagged) {
        this.ok = ok;
        this.message = message;
        this.session = session;
        this.status = status;
        this.confidence = confidence;
        this.factors = factors;
        this.flagged = flagged;
    }

    public static CheckInResult rejected(String message) {
        return new CheckInResult(false, message, null, null, 0, Collections.emptyList(), false);
    }

    public static CheckInResult rejected(String message, AttendanceSession session) {
        return new CheckInResult(false, message, session, null, 0, Collections.emptyList(), false);
    }

    public static CheckInResult accepted(AttendanceSession session, CheckIn.Status status,
                                         ConfidenceScorer.Score score) {
        String msg = status == CheckIn.Status.LATE
                ? "You're checked in, but marked late."
                : "You're checked in.";
        if (score.isFlagged()) {
            msg += " Your lecturer will review this check-in.";
        }
        return new CheckInResult(true, msg, session, status, score.getValue(), score.getFactors(), score.isFlagged());
    }

    public boolean isOk() { return ok; }
    public String getMessage() { return message; }
    public AttendanceSession getSession() { return session; }
    public CheckIn.Status getStatus() { return status; }
    public int getConfidence() { return confidence; }
    public String getConfidenceLabel() { return ConfidenceLevel.of(confidence).getLabel(); }
    public List<String> getFactors() { return factors; }
    public boolean isFlagged() { return flagged; }
}
