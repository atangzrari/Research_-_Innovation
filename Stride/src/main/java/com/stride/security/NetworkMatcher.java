package com.stride.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/** Tests whether an IP address falls inside a list of CIDR ranges (the campus network factor, RQ17). */
public final class NetworkMatcher {

    private static final class Range {
        final byte[] network;
        final int prefix;

        Range(byte[] network, int prefix) {
            this.network = network;
            this.prefix = prefix;
        }
    }

    private final List<Range> ranges = new ArrayList<>();

    public NetworkMatcher(List<String> cidrs) {
        for (String cidr : cidrs) {
            String[] parts = cidr.trim().split("/");
            try {
                byte[] addr = literal(parts[0]);
                int prefix = parts.length > 1 ? Integer.parseInt(parts[1]) : addr.length * 8;
                if (prefix < 0 || prefix > addr.length * 8) {
                    throw new IllegalArgumentException("Bad prefix in " + cidr);
                }
                ranges.add(new Range(addr, prefix));
            } catch (UnknownHostException | NumberFormatException e) {
                throw new IllegalArgumentException("Bad network range: " + cidr, e);
            }
        }
    }

    public boolean isEmpty() {
        return ranges.isEmpty();
    }

    public boolean contains(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        byte[] addr;
        try {
            addr = literal(ip.trim());
        } catch (UnknownHostException e) {
            return false;
        }
        for (Range r : ranges) {
            if (r.network.length == addr.length && matches(r.network, addr, r.prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(byte[] net, byte[] addr, int prefix) {
        int full = prefix / 8;
        for (int i = 0; i < full; i++) {
            if (net[i] != addr[i]) {
                return false;
            }
        }
        int rest = prefix % 8;
        if (rest == 0) {
            return true;
        }
        int mask = (0xFF << (8 - rest)) & 0xFF;
        return (net[full] & mask) == (addr[full] & mask);
    }

    /** Parses an IP literal without any DNS lookup. */
    private static byte[] literal(String text) throws UnknownHostException {
        String t = text.startsWith("[") && text.endsWith("]") ? text.substring(1, text.length() - 1) : text;
        boolean looksLikeIp = t.contains(":") || t.matches("\\d{1,3}(\\.\\d{1,3}){3}");
        if (!looksLikeIp) {
            throw new UnknownHostException("Not an IP literal: " + text);
        }
        byte[] b = InetAddress.getByName(t).getAddress();
        // Map IPv4-mapped IPv6 (::ffff:a.b.c.d) to plain IPv4.
        if (b.length == 16) {
            boolean mapped = true;
            for (int i = 0; i < 10; i++) {
                mapped &= b[i] == 0;
            }
            mapped &= b[10] == (byte) 0xFF && b[11] == (byte) 0xFF;
            if (mapped) {
                return new byte[]{b[12], b[13], b[14], b[15]};
            }
        }
        return b;
    }
}
