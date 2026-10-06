# rate-limited-public-api

A per-API-key token bucket that returns `200` or `429` with a `Retry-After` value, in `Main.java`.

## Goal
Show how a token bucket allows a short burst while enforcing a steady rate, and tells rejected callers how long to wait.

## Run it
```
java Main.java
```
Expected output (this was run with Java 21):

```
OK rate-limited-public-api
```

## What it proves
- A key may make 5 requests at once (capacity 5); the 6th gets `429 Retry-After=1`.
- Another key has its own bucket and is unaffected.
- After 1 second exactly one token is back (one request passes, the next is rejected).
- After 60 seconds the bucket refills only up to capacity, never above it.
- Time is passed in as milliseconds, so the checks are deterministic.

## Trade-offs
- Buckets live in a `HashMap`; across several servers they need a shared store, and the code comment points to a Redis hash with a Lua script for atomicity (not run here).
- Nothing is thread-safe, and old keys are never evicted.
- One global limit per key; no per-endpoint or per-plan limits.

## When not to use it
- When you need a hard cap per fixed window (such as monthly quotas); use a counter instead.
- When bursts must be forbidden entirely; use a leaky bucket or fixed spacing.
