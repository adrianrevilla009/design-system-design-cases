# audit-log

An append-only audit log where each record includes the SHA-256 hash of the previous one, in `Main.java`.

## Goal
Show how hash chaining makes edits and deletions in a log detectable.

## Run it
```
java Main.java
```
Expected output (this was run with Java 21):

```
OK audit-log
```

## What it proves
- Three appended records (starting from a `GENESIS` hash) verify as intact: `verify()` returns `-1`.
- Changing the actor of record 1 to `mallory` makes `verify()` return `1`, the first bad record.
- After restoring it, removing record 1 also fails verification at position 1, because the sequence number and previous hash no longer match.
- Each hash covers sequence number, actor, action and the previous hash.

## Trade-offs
- Anyone who can rewrite the whole log can recompute every hash; real tamper evidence needs the latest hash anchored somewhere else (a signature or external store).
- Verification reads the whole chain; large logs need checkpoints.
- The log is an in-memory list; the code comment suggests revoking UPDATE/DELETE and partitioning by month in Postgres, which was not run.

## When not to use it
- When the threat is only accidental change and database permissions already prevent edits.
- When you must be able to erase personal data on request; a chain makes deletion visible by design.
