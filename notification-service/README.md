# notification-service

Channel routing by preference order with retries, exponential backoff and a dead-letter result, in `Main.java`.

## Goal
Show how a notification sender retries a flaky channel, falls back to the next channel, and gives up cleanly when all fail.

## Run it
```
java Main.java
```
Expected output (this was run with Java 21):

```
OK notification-service backoff=[100, 200, 100, 200, 100, 200]
```

## What it proves
- A channel that fails twice succeeds on its third attempt, and the recorded waits are 100 ms then 200 ms (doubling).
- A channel that always fails is abandoned after 3 attempts and the next preferred channel (`sms`) delivers.
- If every channel fails, `notify` returns `DEAD_LETTER`.
- Waits are recorded in a list instead of slept, so the run is instant; real delays are not exercised.

## Trade-offs
- Retries happen inline in the caller; production would put a queue and workers per channel in between (the estimate comment suggests about 600 messages/s at burst).
- Backoff has no jitter, so many senders failing together would retry together.
- Channels are fakes (`Flaky`); no real email, SMS or push provider is called.

## When not to use it
- When a message must not be sent twice; retries here can duplicate a send if a failure was reported after delivery.
- For very high volume; there is no queue, rate limit or per-user throttling.
