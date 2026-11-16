package com.stride.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Brute-force protection for sign-in (defect D5): a lock per e-mail address, a lock per source
 * address, and a cap on attempts per minute for the whole server. The per-e-mail lock alone let
 * one attacker try many different e-mail addresses, each costing about 0.6 s of hashing.
 */
public final class SignInGuard {

    private final LoginThrottle byEmail;
    private final LoginThrottle byAddress;
    private final int globalPerMinute;
    private final Clock clock;
    private final Deque<Instant> recent = new ArrayDeque<>();

    public SignInGuard(int emailMax, int addressMax, int globalPerMinute, Duration lockout, Clock clock) {
        this.byEmail = new LoginThrottle(emailMax, lockout, clock);
        this.byAddress = new LoginThrottle(addressMax, lockout, clock);
        this.globalPerMinute = globalPerMinute;
        this.clock = clock;
    }

    /** Zero if this attempt may go ahead, otherwise how long to wait. Counts the attempt globally. */
    public Duration check(String email, String address) {
        Duration e = byEmail.remainingLock(email);
        Duration a = byAddress.remainingLock(address);
        Duration worst = e.compareTo(a) >= 0 ? e : a;
        if (!worst.isZero()) {
            return worst;
        }
        synchronized (recent) {
            Instant now = clock.instant();
            Instant cutoff = now.minusSeconds(60);
            while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) {
                recent.pollFirst();
            }
            if (recent.size() >= globalPerMinute) {
                Duration wait = Duration.between(now, recent.peekFirst().plusSeconds(60));
                return wait.isNegative() || wait.isZero() ? Duration.ofSeconds(1) : wait;
            }
            recent.addLast(now);
        }
        return Duration.ZERO;
    }

    public void failure(String email, String address) {
        byEmail.recordFailure(email);
        byAddress.recordFailure(address);
    }

    public void success(String email, String address) {
        byEmail.reset(email);
        byAddress.reset(address);
    }
}
