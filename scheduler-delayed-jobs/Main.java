import java.util.*;

/** Delayed-job scheduler: a min-heap by due time, with at-least-once delivery via visibility timeout. */
public class Main {
    record Job(String id, long dueAt) {}

    static final PriorityQueue<Job> ready = new PriorityQueue<>(Comparator.comparingLong(Job::dueAt));
    static final Map<String, Long> inFlight = new HashMap<>(); // job id -> lease expiry
    static final Map<String, Job> jobs = new HashMap<>();
    static final long LEASE = 30;

    static void schedule(String id, long dueAt) { Job j = new Job(id, dueAt); jobs.put(id, j); ready.add(j); }

    /** Hand out the next due job and lease it; an unacked lease expires and the job is redelivered. */
    static Optional<Job> poll(long now) {
        inFlight.entrySet().removeIf(e -> {
            if (e.getValue() > now) return false;
            ready.add(jobs.get(e.getKey()));
            return true;
        });
        if (ready.isEmpty() || ready.peek().dueAt() > now) return Optional.empty();
        Job j = ready.poll();
        inFlight.put(j.id(), now + LEASE);
        return Optional.of(j);
    }

    static void ack(String id) { inFlight.remove(id); jobs.remove(id); }

    public static void main(String[] args) {
        // Estimate: 10M delayed jobs, 1k due/s -> Redis ZSET (score = dueAt) or Postgres index on due_at + SKIP LOCKED.
        schedule("late", 100); schedule("early", 10); schedule("mid", 50);
        check(poll(5).isEmpty(), "nothing due yet");
        check(poll(60).get().id().equals("early"), "earliest first");
        ack("early");
        check(poll(60).get().id().equals("mid"), "then mid");
        check(poll(61).isEmpty(), "late not due; mid is leased");
        check(poll(95).get().id().equals("mid"), "mid redelivered after lease expiry (no ack)");
        ack("mid");
        check(poll(200).get().id().equals("late"), "late runs once due");
        ack("late");
        check(poll(300).isEmpty() && jobs.isEmpty(), "drained");
        System.out.println("OK scheduler-delayed-jobs");
    }

    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
}
