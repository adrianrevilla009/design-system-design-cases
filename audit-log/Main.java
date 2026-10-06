import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Append-only audit log: each record carries the hash of the previous one, so edits are detectable. */
public class Main {
    record Rec(long seq, String actor, String action, String prevHash, String hash) {}

    static final List<Rec> log = new ArrayList<>();

    static String sha256(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    static String hashOf(long seq, String actor, String action, String prev) throws Exception {
        return sha256(seq + "|" + actor + "|" + action + "|" + prev);
    }

    static void append(String actor, String action) throws Exception {
        String prev = log.isEmpty() ? "GENESIS" : log.get(log.size() - 1).hash();
        long seq = log.size();
        log.add(new Rec(seq, actor, action, prev, hashOf(seq, actor, action, prev)));
    }

    /** Returns the seq of the first bad record, or -1 if the chain is intact. */
    static long verify() throws Exception {
        String prev = "GENESIS";
        long expectedSeq = 0;
        for (Rec r : log) {
            boolean ok = r.seq() == expectedSeq && r.prevHash().equals(prev)
                && r.hash().equals(hashOf(r.seq(), r.actor(), r.action(), r.prevHash()));
            if (!ok) return expectedSeq;
            prev = r.hash();
            expectedSeq++;
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        // Estimate: 500 events/s x 300 B ~ 13 TB over 3 years raw; in Postgres revoke UPDATE/DELETE and partition by month.
        append("alice", "order.create"); append("bob", "order.refund"); append("alice", "user.delete");
        check(verify() == -1, "intact chain verifies");
        Rec r = log.get(1);
        log.set(1, new Rec(r.seq(), "mallory", r.action(), r.prevHash(), r.hash())); // tamper with the actor
        check(verify() == 1, "tampering detected at seq 1");
        log.set(1, r);
        log.remove(1); // deleting a record also breaks the chain
        check(verify() == 1, "deletion detected");
        System.out.println("OK audit-log");
    }

    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
}
