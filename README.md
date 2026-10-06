# design-system-design-cases

Nine classic system-design cases, each reduced to one runnable Java file that checks the core invariant, with the back-of-envelope estimate written in a comment next to the code. The cases use a tiny Orders-style domain where it fits.

## What is inside

| Folder | What it shows | Run |
| --- | --- | --- |
| [`url-shortener`](./url-shortener) | Counter to base62 IDs and an LRU cache in front of the store | `java Main.java` |
| [`notification-service`](./notification-service) | Channel preference order, exponential backoff, dead letter | `java Main.java` |
| [`file-upload-presigned`](./file-upload-presigned) | HMAC-signed upload URL checked for signature, expiry and size | `java Main.java` |
| [`scheduler-delayed-jobs`](./scheduler-delayed-jobs) | Min-heap by due time with lease-based redelivery | `java Main.java` |
| [`double-entry-ledger`](./double-entry-ledger) | Zero-sum transactions, idempotency keys, derived balances | `java Main.java` |
| [`audit-log`](./audit-log) | Hash-chained append-only log that detects edits and deletions | `java Main.java` |
| [`feed-fan-out`](./feed-fan-out) | Fan-out on write versus on read, with write amplification counted | `java Main.java` |
| [`search-autocomplete`](./search-autocomplete) | Trie with a precomputed top-3 list per prefix | `java Main.java` |
| [`rate-limited-public-api`](./rate-limited-public-api) | Per-key token bucket with a Retry-After value | `java Main.java` |

Run each command from inside its folder. Each prints a line starting with `OK` and exits with an error if a check fails.

## Prerequisites

- Java 21 (single-file source launch, no build tool, no containers).

## How to read it

Start with `url-shortener`, then open any folder you like; they are independent. Redis and Postgres are replaced by in-memory maps, and the comments name what would replace them in production. Nothing here was run against a real Redis or Postgres.
