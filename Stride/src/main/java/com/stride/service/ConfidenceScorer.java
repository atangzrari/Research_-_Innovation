package com.stride.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Multi-factor attendance confidence (RQ17, Finding F4). No single signal is trusted on its own:
 * each independent signal adds to (or takes from) a score of 0 to 100, and low scores are flagged
 * for the lecturer instead of being silently accepted or rejected.
 *
 * <pre>
 *   Rotating code typed         +45     QR scanned in the room       +55
 *   On the campus network       +25     Off the campus network       -10
 *   Own device (not shared)     +10     Device already used by
 *                                       another student this session -45  (always flagged)
 *   Location inside campus      +15     Location outside campus      -25
 * </pre>
 */
public final class ConfidenceScorer {

    public enum Token { CODE, QR }

    public enum Location { NOT_USED, INSIDE, OUTSIDE }

    /** The outcome: a score, the human-readable factors, and whether it needs review. */
    public static final class Score {
        private final int value;
        private final List<String> factors;
        private final boolean flagged;

        Score(int value, List<String> factors, boolean flagged) {
            this.value = value;
            this.factors = Collections.unmodifiableList(factors);
            this.flagged = flagged;
        }

        public int getValue() { return value; }
        public List<String> getFactors() { return factors; }
        public String getFactorText() { return String.join("; ", factors); }
        public boolean isFlagged() { return flagged; }
    }

    private final boolean networkCheckOn;
    private final int flagBelow;

    public ConfidenceScorer(boolean networkCheckOn, int flagBelow) {
        this.networkCheckOn = networkCheckOn;
        this.flagBelow = flagBelow;
    }

    public Score score(Token token, boolean onCampusNetwork, boolean deviceSharedWithOther, Location location) {
        int score = 0;
        List<String> factors = new ArrayList<>();
        if (token == Token.QR) {
            score += 55;
            factors.add("QR scanned (+55)");
        } else {
            score += 45;
            factors.add("Rotating code (+45)");
        }
        if (networkCheckOn) {
            if (onCampusNetwork) {
                score += 25;
                factors.add("Campus network (+25)");
            } else {
                score -= 10;
                factors.add("Off campus network (-10)");
            }
        }
        if (deviceSharedWithOther) {
            score -= 45;
            factors.add("Device already used by another student (-45)");
        } else {
            score += 10;
            factors.add("Own device (+10)");
        }
        if (location == Location.INSIDE) {
            score += 15;
            factors.add("Location on campus (+15)");
        } else if (location == Location.OUTSIDE) {
            score -= 25;
            factors.add("Location off campus (-25)");
        }
        int value = Math.max(0, Math.min(100, score));
        boolean flagged = deviceSharedWithOther || value < flagBelow;
        return new Score(value, factors, flagged);
    }
}
