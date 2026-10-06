# double-entry-ledger

An append-only double-entry journal with a zero-sum rule and idempotency keys, in `Main.java`.

## Goal
Show that a ledger stays consistent when every transaction must balance, replays are ignored, and balances are derived from entries only.

## Run it
```
java Main.java
```
Expected output (this was run with Java 21):

```
OK double-entry-ledger cash=7500
```

## What it proves
- A transaction whose entries do not sum to zero (`+100`, `-99`) throws and is not recorded; the journal keeps 2 transactions.
- Posting key `t1` a second time returns `false` and changes nothing.
- The `cash` balance is 7,500 cents (+10,000 from `t1`, -2,500 from `t2`), computed by summing entries.
- Across the whole journal, all entries sum to zero.

## Trade-offs
- `balance` scans the entire journal; a real ledger keeps running balances or snapshots.
- The idempotency set and journal are in memory and are lost on exit; in Postgres the sum would need a deferred constraint trigger or a single multi-row insert, as noted in the code comment (not run here).
- Amounts are `long` cents in a single currency; there is no multi-currency or account validation.

## When not to use it
- When a simple balance column is enough and there is no audit or reconciliation need.
- For concurrent writers; nothing here is thread-safe.
