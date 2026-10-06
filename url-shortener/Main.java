import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** URL shortener: counter -> base62 ID, plus a small LRU cache in front of the store. */
public class Main {
    static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final AtomicLong counter = new AtomicLong(1_000_000); // stands in for a Redis INCR / DB sequence
    static final Map<String, String> store = new HashMap<>();    // stands in for Postgres
    static int storeReads = 0;
    static final Map<String, String> cache = new LinkedHashMap<>(16, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, String> e) { return size() > 2; }
    };

    static String encode(long n) {
        StringBuilder sb = new StringBuilder();
        do { sb.append(ALPHABET.charAt((int) (n % 62))); n /= 62; } while (n > 0);
        return sb.reverse().toString();
    }

    static String shorten(String url) {
        String id = encode(counter.getAndIncrement());
        store.put(id, url);
        return id;
    }

    static String resolve(String id) {
        String hit = cache.get(id);
        if (hit != null) return hit;
        storeReads++;
        String url = store.get(id);
        if (url != null) cache.put(id, url);
        return url;
    }

    public static void main(String[] args) {
        // Estimate: 100M new URLs/month ~ 40 writes/s; 100:1 reads ~ 4000 reads/s; 7 base62 chars = 3.5e12 IDs.
        String a = shorten("https://example.com/orders/1");
        String b = shorten("https://example.com/orders/2");
        check(!a.equals(b), "ids are unique");
        check(a.length() <= 7, "id short enough: " + a);
        check(resolve(a).endsWith("/1"), "resolve a");
        resolve(a); resolve(a);
        check(storeReads == 1, "repeat reads served from cache");
        check(resolve("nope") == null, "unknown id -> null");
        System.out.println("OK url-shortener ids=" + a + "," + b + " storeReads=" + storeReads);
    }

    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
}
