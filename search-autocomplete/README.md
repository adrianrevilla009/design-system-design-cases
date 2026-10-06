# search-autocomplete

A trie where every node stores its top three completions by score, in `Main.java`.

## Goal
Show how precomputing the best suggestions per prefix makes a lookup cost only the length of the prefix.

## Run it
```
java Main.java
```
Expected output (this was run with Java 21):

```
OK search-autocomplete
```

## What it proves
- For the prefix `ord` the result is `order`, `order status`, `orders list` (scores 50, 40, 30), sorted by score then alphabetically.
- Six terms were inserted under `or`, but only three are returned, so K caps the result.
- An unknown prefix such as `zzz` returns an empty list.
- Re-inserting `order refund` with score 100 moves it to the top for `order`.

## Trade-offs
- Storing top-K at every node costs memory and makes inserts slower (each insert re-sorts a list per character).
- Scores are static input; nothing here learns from query logs or decays old terms.
- Matching is case-sensitive prefix matching only; no typo tolerance or tokenising.

## When not to use it
- When you need fuzzy or full-text matching; use a search engine.
- When the term set changes constantly; rebuilding top-K per node gets expensive.
