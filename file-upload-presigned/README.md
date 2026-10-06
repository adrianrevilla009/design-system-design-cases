# file-upload-presigned

An HMAC-SHA256 presigned upload URL and the storage-side check that validates it, in `Main.java`.

## Goal
Show how an API can authorise a direct upload by signing the key, expiry and size limit, so the storage side verifies it without calling the API back.

## Run it
```
java Main.java
```
Expected output (this was run with Java 21; the signature is deterministic):

```
/upload/invoices/42.pdf?expires=1300&max=5000000&sig=75831e9c...5d0e
OK file-upload-presigned
```


## What it proves
- `presign` builds a URL carrying `expires`, `max` and an HMAC over `key|expires|max`.
- `accept` returns `200 stored` for a valid request, `403 expired` after the expiry, and `413 too large` when the body exceeds the signed limit.
- Changing the key or raising the limit invalidates the signature (`403 bad signature`); the comparison uses `MessageDigest.isEqual`.
- The secret in the file is a demo value, not a credential.

## Trade-offs
- A shared secret means the storage side can also mint URLs; asymmetric signatures or per-bucket keys limit that.
- The size limit is only checked against a given body size here; real storage must enforce it while streaming.
- A signed URL can be reused until it expires; there is no one-time-use tracking.

## When not to use it
- When uploads must be scanned or transformed before being accepted; route them through a service instead.
- When you need resumable or multipart uploads; this models a single request only. No real object store was used.
