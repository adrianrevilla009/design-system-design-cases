# scheduler-delayed-jobs

A delayed-job scheduler built on a min-heap by due time, with leases that give at-least-once delivery, in `Main.java`.

## Goal
Show how to hand out jobs in due-time order and redeliver a job whose worker never acknowledged it.

## Run it
```
java Main.java
```
Expected output (this was run with Java 21):

```
OK scheduler-delayed-jobs
```

## What it proves
- Jobs scheduled as `late` (100), `early` (10) and `mid` (50) come out earliest first, and nothing is returned at time 5 because none is due.
- `poll` leases a job for 30 time units; an unacknowledged `mid` leased at 60 is handed out again at 95.
- `ack` removes the job for good, and after all three are acked the queue and job map are empty.
- Time is passed in as a number, so the checks are deterministic.

## Trade-offs
- At-least-once delivery means a job can run twice; handlers must be idempotent.
- State lives in plain Java collections; production would use a Redis sorted set or a Postgres `due_at` index with `SKIP LOCKED` (named in the code comment, not run here).
- Expired leases are only checked during `poll`, which scans every in-flight entry.

## When not to use it
- When exactly-once execution is required; this cannot give it.
- For recurring schedules (cron-like); there is no repeat or cancel support.
