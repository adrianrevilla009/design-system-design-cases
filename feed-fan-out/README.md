# feed-fan-out

A social feed built two ways, pushing to followers' inboxes on write and merging authors' posts on read, in `Main.java`.

## Goal
Show the trade between write amplification and read cost, and that both strategies produce the same feed.

## Run it
```
java Main.java
```
Expected output (this was run with Java 21):

```
OK feed-fan-out writeOps=7
```

## What it proves
- With `alice` followed by u1, u2, u3 and `bob` followed by u1, three posts cause 7 inbox writes (3 + 1 + 3).
- `readOnWrite` (reads the inbox) and `readOnRead` (merges authors' posts) return the same list for u1, newest first.
- u2, who follows only `alice`, sees 2 posts.
- The comment in `main` argues for a hybrid for authors with millions of followers; the hybrid itself is not implemented.

## Trade-offs
- Fan-out on write makes reads cheap but multiplies writes by follower count.
- Fan-out on read keeps writes cheap but sorts every followed author's posts at read time.
- Everything is in memory with no pagination, deletion or unfollow handling.

## When not to use it
- When authors have very large follower counts and you push to everyone; use pull or the hybrid.
- When feed order must be ranked, not just chronological.
