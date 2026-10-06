import java.util.*;

/** Autocomplete: a trie where each node keeps its top-K completions by score, so lookup is O(prefix length). */
public class Main {
    static final int K = 3;

    static class Node {
        final Map<Character, Node> kids = new TreeMap<>();
        final List<Map.Entry<String, Integer>> top = new ArrayList<>(); // best K (term, score) under this prefix
    }

    static final Node root = new Node();

    static void insert(String term, int score) {
        Node n = root;
        offer(n, term, score);
        for (char c : term.toCharArray()) {
            n = n.kids.computeIfAbsent(c, k -> new Node());
            offer(n, term, score);
        }
    }

    static void offer(Node n, String term, int score) {
        n.top.removeIf(e -> e.getKey().equals(term));
        n.top.add(Map.entry(term, score));
        n.top.sort((a, b) -> !a.getValue().equals(b.getValue()) ? b.getValue() - a.getValue() : a.getKey().compareTo(b.getKey()));
        if (n.top.size() > K) n.top.remove(K);
    }

    static List<String> suggest(String prefix) {
        Node n = root;
        for (char c : prefix.toCharArray()) {
            n = n.kids.get(c);
            if (n == null) return List.of();
        }
        return n.top.stream().map(Map.Entry::getKey).toList();
    }

    public static void main(String[] args) {
        // Estimate: 10M terms x ~20 chars; 30k keystrokes/s must answer in <50ms -> precomputed top-K per node,
        // sharded by first letters, rebuilt offline from query logs.
        insert("order", 50); insert("orders list", 30); insert("order status", 40);
        insert("organization", 10); insert("order refund", 5); insert("invoice", 99);
        check(suggest("ord").equals(List.of("order", "order status", "orders list")), "top-3 for 'ord': " + suggest("ord"));
        check(suggest("or").size() == 3, "K caps results");
        check(suggest("inv").equals(List.of("invoice")), "inv");
        check(suggest("zzz").isEmpty(), "no match");
        insert("order refund", 100);
        check(suggest("order").get(0).equals("order refund"), "score update re-ranks");
        System.out.println("OK search-autocomplete");
    }

    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
}
