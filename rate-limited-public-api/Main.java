import java.util.*;

/** Public API rate limiting: per-key token bucket (burst + steady rate) with Retry-After on rejection. */
public class Main {
    static class Bucket {
        double tokens; long last;
        Bucket(double t, long l) { tokens = t; last = l; }
    }

    static final double CAPACITY = 5, REFILL_PER_SEC = 1;
    static final Map<String, Bucket> buckets = new HashMap<>(); // in production: Redis hash + Lua script for atomicity

    /** Returns 0 if allowed, else seconds the caller should wait. */
    static long tryAcquire(String apiKey, long nowMs) {
        Bucket b = buckets.computeIfAbsent(apiKey, k -> new Bucket(CAPACITY, nowMs));
        b.tokens = Math.min(CAPACITY, b.tokens + (nowMs - b.last) / 1000.0 * REFILL_PER_SEC);
        b.last = nowMs;
        if (b.tokens >= 1) { b.tokens -= 1; return 0; }
        return (long) Math.ceil((1 - b.tokens) / REFILL_PER_SEC);
    }

    static String handle(String apiKey, long nowMs) {
        long wait = tryAcquire(apiKey, nowMs);
        return wait == 0 ? "200" : "429 Retry-After=" + wait;
    }

    public static void main(String[] args) {
        // Estimate: 50k keys x ~40 B = 2 MB; 20k req/s -> one Redis round trip each, or local buckets
        // with periodic sync when slight over-admission is acceptable.
        long t = 0;
        for (int i = 0; i < 5; i++) check(handle("k1", t).equals("200"), "burst of 5 allowed #" + i);
        check(handle("k1", t).equals("429 Retry-After=1"), "6th request rejected");
        check(handle("k2", t).equals("200"), "keys are isolated");
        check(handle("k1", t + 1000).equals("200"), "one token refilled after 1s");
        check(handle("k1", t + 1000).startsWith("429"), "and only one");
        check(handle("k1", t + 60_000).equals("200") && buckets.get("k1").tokens <= CAPACITY, "refill capped at capacity");
        System.out.println("OK rate-limited-public-api");
    }

    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
}
