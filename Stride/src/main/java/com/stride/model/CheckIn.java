package com.stride.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** One student's attendance record for one session (RQ16-RQ18). */
public class CheckIn {

    public enum Method { CODE, QR, MANUAL }

    public enum Status {
        PRESENT("Present"), LATE("Late"), ABSENT("Absent"), EXCUSED("Excused");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() { return label; }
        public String getName() { return name(); }
        /** Present and late both count as attended. */
        public boolean isAttended() { return this == PRESENT || this == LATE; }
    }

    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int id;
    private final int sessionId;
    private final int studentId;
    private final String studentName;
    private final String studentNumber;
    private final LocalDateTime checkedInAt;
    private final Method method;
    private final Status status;
    private final int confidence;
    private final String factors;
    private final boolean flagged;
    private final String correctionReason;
    private final String correctedByName;

    public CheckIn(int id, int sessionId, int studentId, String studentName, String studentNumber,
                   LocalDateTime checkedInAt, Method method, Status status, int confidence, String factors,
                   boolean flagged, String correctionReason, String correctedByName) {
        this.id = id;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentNumber = studentNumber;
        this.checkedInAt = checkedInAt;
        this.method = method;
        this.status = status;
        this.confidence = confidence;
        this.factors = factors;
        this.flagged = flagged;
        this.correctionReason = correctionReason;
        this.correctedByName = correctedByName;
    }

    public int getId() { return id; }
    public int getSessionId() { return sessionId; }
    public int getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStudentNumber() { return studentNumber; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public String getTimeText() { return HMS.format(checkedInAt); }
    public Method getMethod() { return method; }
    public String getMethodLabel() {
        switch (method) {
            case QR: return "QR scan";
            case CODE: return "Typed code";
            default: return "Set by lecturer";
        }
    }
    public Status getStatus() { return status; }
    public int getConfidence() { return confidence; }
    public String getConfidenceLabel() { return ConfidenceLevel.of(confidence).getLabel(); }
    public String getFactors() { return factors; }
    public boolean isFlagged() { return flagged; }
    public String getCorrectionReason() { return correctionReason; }
    public String getCorrectedByName() { return correctedByName; }
    public boolean isCorrected() { return correctionReason != null; }
}
