import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Presigned upload: the API signs (key, expiry, max size); the storage side verifies without calling the API. */
public class Main {
    static final byte[] SECRET = "demo-only-shared-secret".getBytes(StandardCharsets.UTF_8); // demo value, not a credential

    static String sign(String key, long expires, long maxBytes) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((key + "|" + expires + "|" + maxBytes).getBytes(StandardCharsets.UTF_8)));
    }

    static String presign(String key, long nowSec, long ttlSec, long maxBytes) throws Exception {
        long exp = nowSec + ttlSec;
        return "/upload/" + key + "?expires=" + exp + "&max=" + maxBytes + "&sig=" + sign(key, exp, maxBytes);
    }

    /** Storage-side check: signature, expiry, and size. */
    static String accept(String key, long expires, long maxBytes, String sig, long nowSec, long bodyBytes) throws Exception {
        boolean good = MessageDigest.isEqual(sign(key, expires, maxBytes).getBytes(), sig.getBytes()); // constant time
        if (!good) return "403 bad signature";
        if (nowSec > expires) return "403 expired";
        if (bodyBytes > maxBytes) return "413 too large";
        return "200 stored";
    }

    public static void main(String[] args) throws Exception {
        // Estimate: 10 MB avg x 100 uploads/s = 1 GB/s that never touches the API servers.
        long now = 1_000;
        System.out.println(presign("invoices/42.pdf", now, 300, 5_000_000));
        long exp = now + 300;
        String sig = sign("invoices/42.pdf", exp, 5_000_000);
        check(accept("invoices/42.pdf", exp, 5_000_000, sig, now + 10, 1_000).equals("200 stored"), "valid upload");
        check(accept("invoices/42.pdf", exp, 5_000_000, sig, now + 301, 1_000).startsWith("403"), "expired");
        check(accept("invoices/43.pdf", exp, 5_000_000, sig, now + 10, 1_000).startsWith("403"), "key tampering");
        check(accept("invoices/42.pdf", exp, 9_000_000, sig, now + 10, 1_000).startsWith("403"), "limit tampering");
        check(accept("invoices/42.pdf", exp, 5_000_000, sig, now + 10, 6_000_000).startsWith("413"), "too large");
        System.out.println("OK file-upload-presigned");
    }

    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
}
