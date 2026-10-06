import java.util.*;

/** Double-entry ledger: every transaction must sum to zero; balances are derived from entries only. */
public class Main {
    record Entry(String account, long cents) {}

    static final List<List<Entry>> journal = new ArrayList<>(); // append-only
    static final Set<String> seen = new HashSet<>();            // idempotency keys

    static boolean post(String idempotencyKey, List<Entry> entries) {
        if (entries.size() < 2) throw new IllegalArgumentException("need at least two entries");
        long sum = entries.stream().mapToLong(Entry::cents).sum();
        if (sum != 0) throw new IllegalArgumentException("unbalanced transaction, sum=" + sum);
        if (!seen.add(idempotencyKey)) return false; // replay: already applied
        journal.add(List.copyOf(entries));
        return true;
    }

    static long balance(String account) {
        return journal.stream().flatMap(List::stream).filter(e -> e.account().equals(account)).mapToLong(Entry::cents).sum();
    }

    public static void main(String[] args) {
        // Estimate: 1k tx/s x 2 entries x 100 B = 200 KB/s ~ 17 GB/day; in Postgres enforce the sum with a
        // deferred constraint trigger, or insert all entries of a transaction in one statement.
        check(post("t1", List.of(new Entry("cash", 10_000), new Entry("revenue", -10_000))), "post t1");
        check(!post("t1", List.of(new Entry("cash", 10_000), new Entry("revenue", -10_000))), "replay ignored");
        post("t2", List.of(new Entry("refunds", 2_500), new Entry("cash", -2_500)));
        check(balance("cash") == 7_500, "cash balance");
        long total = journal.stream().flatMap(List::stream).mapToLong(Entry::cents).sum();
        check(total == 0, "global invariant: all entries sum to zero");
        try {
            post("t3", List.of(new Entry("cash", 100), new Entry("revenue", -99)));
            check(false, "should reject");
        } catch (IllegalArgumentException expected) {
            // rejected as designed
        }
        check(journal.size() == 2, "rejected tx not recorded");
        System.out.println("OK double-entry-ledger cash=" + balance("cash"));
    }

    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
}
