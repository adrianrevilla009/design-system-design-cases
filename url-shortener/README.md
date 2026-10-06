# url-shortener

A counter-based base62 ID generator with a two-entry LRU cache in front of an in-memory store, in `Main.java`.

## Goal
Show how short IDs are produced without collisions and how a cache absorbs the read-heavy traffic of a shortener.

## Run it
```
java Main.java
```
Expected output (this was run with Java 21):

```
OK url-shortener ids=4c92,4c93 storeReads=2
```

## What it proves
- IDs come from a counter starting at 1,000,000 encoded in base62, so two calls give distinct IDs (`4c92`, `4c93`) with no collision check.
- Resolving the same ID three times hits the store once; the other two reads come from the `LinkedHashMap` LRU cache (capacity 2).
- The final `storeReads=2` is the first miss plus the lookup of an unknown ID, which returns `null` and is not cached.
- The estimate in `main` (about 40 writes/s, 4000 reads/s, 7 base62 characters give 3.5e12 IDs) is a comment, not measured.

## Trade-offs
- A sequential counter makes IDs guessable; scrambling or a random suffix would be needed if links must be private.
- The counter and store are plain Java objects standing in for a Redis `INCR` and Postgres; a single shared counter is a bottleneck and a single point of failure.
- Unknown IDs are never cached, so a flood of bad IDs would reach the store every time.

## When not to use it
- When links must be unguessable or custom-named; use random or user-chosen slugs with a uniqueness check.
- When you need expiry, analytics or deletion; this has none of them.
