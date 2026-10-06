import java.util.*;

/** Notification service: channel routing by user preference, retries with backoff, fallback channel. */
public class Main {
    interface Channel { String name(); boolean send(String to, String body); }

    /** Fails the first `failures` attempts, then succeeds. */
    static class Flaky implements Channel {
        final String name; final int failures; int attempts = 0;
        Flaky(String name, int failures) { this.name = name; this.failures = failures; }
        public String name() { return name; }
        public boolean send(String to, String body) { attempts++; return attempts > failures; }
    }

    static final List<Long> sleeps = new ArrayList<>(); // recorded instead of slept, keeps the demo fast

    static boolean sendWithRetry(Channel c, String to, String body, int maxAttempts) {
        long backoffMs = 100;
        for (int i = 1; i <= maxAttempts; i++) {
            if (c.send(to, body)) return true;
            if (i < maxAttempts) { sleeps.add(backoffMs); backoffMs *= 2; }
        }
        return false;
    }

    /** Try channels in preference order; the first that delivers wins. */
    static String notify(List<Channel> prefs, String to, String body) {
        for (Channel c : prefs) if (sendWithRetry(c, to, body, 3)) return c.name();
        return "DEAD_LETTER";
    }

    public static void main(String[] args) {
        // Estimate: 1M users x 5 notifications/day ~ 60/s average, 10x burst = 600/s -> queue + workers per channel.
        Flaky email = new Flaky("email", 2), sms = new Flaky("sms", 0);
        check(notify(List.of(email), "u1", "order shipped").equals("email"), "retry succeeds on 3rd attempt");
        check(email.attempts == 3 && sleeps.equals(List.of(100L, 200L)), "exponential backoff 100,200");

        Flaky down = new Flaky("push", 99);
        check(notify(List.of(down, sms), "u2", "hi").equals("sms"), "falls back to sms");
        check(down.attempts == 3, "gave up on push after 3 attempts");
        check(notify(List.of(new Flaky("x", 99)), "u3", "hi").equals("DEAD_LETTER"), "dead letter");
        System.out.println("OK notification-service backoff=" + sleeps);
    }

    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
}
