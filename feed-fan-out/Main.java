import java.util.*;

/** Feed fan-out: push to followers' inboxes on write (cheap reads) vs merge at read time (cheap writes). */
public class Main {
    record Post(long ts, String author, String text) {}

    static final Map<String, Set<String>> followers = new HashMap<>();
    static final Map<String, List<Post>> postsByAuthor = new HashMap<>(); // fan-out on read source
    static final Map<String, List<Post>> inbox = new HashMap<>();         // fan-out on write target
    static long writeOps = 0;

    static void follow(String user, String author) { followers.computeIfAbsent(author, k -> new HashSet<>()).add(user); }

    static void publish(String author, long ts, String text) {
        Post p = new Post(ts, author, text);
        postsByAuthor.computeIfAbsent(author, k -> new ArrayList<>()).add(p);
        for (String f : followers.getOrDefault(author, Set.of())) {
            inbox.computeIfAbsent(f, k -> new ArrayList<>()).add(p);
            writeOps++;
        }
    }

    static List<Post> readOnWrite(String user, int n) {
        List<Post> l = new ArrayList<>(inbox.getOrDefault(user, List.of()));
        l.sort(Comparator.comparingLong(Post::ts).reversed());
        return l.subList(0, Math.min(n, l.size()));
    }

    static List<Post> readOnRead(Set<String> following, int n) {
        List<Post> l = new ArrayList<>();
        for (String a : following) l.addAll(postsByAuthor.getOrDefault(a, List.of()));
        l.sort(Comparator.comparingLong(Post::ts).reversed());
        return l.subList(0, Math.min(n, l.size()));
    }

    public static void main(String[] args) {
        // Estimate: avg 200 followers -> 200 inbox writes per post. A celebrity with 10M followers makes write
        // fan-out too costly: hybrid = push for normal authors, pull for celebrities at read time.
        for (String u : List.of("u1", "u2", "u3")) follow(u, "alice");
        follow("u1", "bob");
        publish("alice", 1, "a1"); publish("bob", 2, "b1"); publish("alice", 3, "a2");
        List<Post> w = readOnWrite("u1", 10), r = readOnRead(Set.of("alice", "bob"), 10);
        check(w.equals(r), "both strategies give the same feed");
        check(w.get(0).text().equals("a2"), "newest first");
        check(readOnWrite("u2", 10).size() == 2, "u2 only sees alice");
        check(writeOps == 7, "write amplification: 3 + 1 + 3 inbox writes, got " + writeOps);
        System.out.println("OK feed-fan-out writeOps=" + writeOps);
    }

    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
}
