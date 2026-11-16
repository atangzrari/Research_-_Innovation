package com.stride.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Locks an account name out for a while after too many failed sign-ins (slows password guessing). */
public final class LoginThrottle {

    private static final class State {
        int failures;
        Instant lockedUntil;
    }

    private final int maxFailures;
    private final Duration lockout;
    private final Clock clock;
    private final Map<String, State> states = new HashMap<>();

    public LoginThrottle(int maxFailures, Duration lockout, Clock clock) {
        this.maxFailures = maxFailures;
        this.lockout = lockout;
        this.clock = clock;
    }

    /** Time left on the lock for this key, or {@link Duration#ZERO} if it may try now. */
    public synchronized Duration remainingLock(String key) {
        State s = states.get(key);
        if (s == null || s.lockedUntil == null) {
            return Duration.ZERO;
        }
        Duration left = Duration.between(clock.instant(), s.lockedUntil);
        if (left.isNegative() || left.isZero()) {
            states.remove(key);   // lock expired: start fresh
            return Duration.ZERO;
        }
        return left;
    }

    public synchronized void recordFailure(String key) {
        purgeExpired();
        State s = states.computeIfAbsent(key, k -> new State());
        s.failures++;
        if (s.failures >= maxFailures) {
            s.lockedUntil = clock.instant().plus(lockout);
        }
    }

    public synchronized void reset(String key) {
        states.remove(key);
    }

    private void purgeExpired() {
        if (states.size() < 10_000) {
            return;
        }
        Instant now = clock.instant();
        states.values().removeIf(s -> s.lockedUntil != null && s.lockedUntil.isBefore(now));
    }
}
