/* Javo additions, 2026-11-03. SPDX-License-Identifier: GPL-3.0-only */
package app.javo.security;
import java.io.*;
import java.net.URI;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Platform-independent primitives used by Android and the host verification tests. */
public final class UpdateSecurity {
    private UpdateSecurity() {}
    public static void requireHttps(String value) {
        URI uri = URI.create(value);
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null ||
                uri.getFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443))
            throw new IllegalArgumentException("Update URLs must use HTTPS without credentials or fragments");
    }
    public static byte[] verifyPayload(String payload, String signature, String publicKey) throws Exception {
        if (payload.length() > 262144 || signature.length() > 1024 || publicKey.isEmpty())
            throw new SecurityException("Invalid or unconfigured signed update manifest");
        byte[] bytes = Base64.getDecoder().decode(payload);
        var key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
        var verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(key);
        verifier.update(bytes);
        if (!verifier.verify(Base64.getDecoder().decode(signature))) throw new SecurityException("Update manifest signature mismatch");
        return bytes;
    }
    public static String sha256(byte[] value) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(value));
    }
    public static String sha256(File file) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var input = new FileInputStream(file)) {
            byte[] buffer = new byte[65536]; int size;
            while ((size = input.read(buffer)) != -1) digest.update(buffer, 0, size);
        }
        return hex(digest.digest());
    }
    private static String hex(byte[] bytes) {
        var out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return out.toString();
    }
}
