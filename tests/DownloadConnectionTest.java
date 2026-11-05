import app.revanced.manager.downloader.DownloadConnection;
import java.io.*;
import java.net.*;
import java.util.*;

/** Exercises the production connection helper with a deterministic HTTPS handler. */
public final class DownloadConnectionTest {
    private static final Map<String, FakeConnection> responses = new HashMap<>();
    private static int passed;

    private static final class FakeConnection extends HttpURLConnection {
        final Map<String, String> fields = new HashMap<>();
        boolean disconnected;
        void status(int value) { responseCode = value; }
        FakeConnection(URL url) { super(url); responseCode = 200; }
        @Override public void connect() {}
        @Override public void disconnect() { disconnected = true; }
        @Override public boolean usingProxy() { return false; }
        @Override public int getResponseCode() { return responseCode; }
        @Override public String getHeaderField(String key) { return fields.get(key); }
        @Override public String getContentType() { return fields.get("Content-Type"); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(new byte[]{1, 2, 3}); }
    }
    private static FakeConnection response(String host) throws Exception {
        var result = new FakeConnection(new URL("https://" + host + "/apk"));
        responses.put(host, result);
        return result;
    }
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        passed++;
        System.out.println("PASS " + label);
    }
    public static void main(String[] args) throws Exception {
        URL.setURLStreamHandlerFactory(protocol -> protocol.equals("https") ? new URLStreamHandler() {
            @Override protected URLConnection openConnection(URL url) throws IOException {
                var result = responses.get(url.getHost());
                if (result == null) throw new IOException("Unexpected test host");
                return result;
            }
        } : null);
        var noSize = response("missing.test");
        var connection = DownloadConnection.connect("https://missing.test/apk", Map.of());
        check(DownloadConnection.contentLength(connection) == null, "missing content length is supported");
        check(connection.getReadTimeout() == 30_000 && connection.getConnectTimeout() == 15_000, "finite connection and read timeouts");
        noSize.fields.put("Content-Length", "garbage");
        check(DownloadConnection.contentLength(connection) == null, "invalid content length is supported");
        noSize.fields.put("Content-Length", "123");
        check(DownloadConnection.contentLength(connection) == 123, "valid content length parsed");
        try (var stream = DownloadConnection.stream(connection)) {
            check(stream.readAllBytes().length == 3, "download stream readable");
        }
        check(noSize.disconnected, "closing stream releases connection");
        var html = response("html.test");
        html.fields.put("Content-Type", "text/html; charset=UTF-8");
        try { DownloadConnection.connect("https://html.test/apk", Map.of()); throw new AssertionError("HTML accepted"); }
        catch (IOException expected) { check(html.disconnected, "HTML response rejected and released"); }
        var forbidden = response("forbidden.test");
        forbidden.status(403);
        try { DownloadConnection.connect("https://forbidden.test/apk", Map.of()); throw new AssertionError("HTTP error accepted"); }
        catch (IOException expected) { check(forbidden.disconnected, "HTTP failure released"); }
        var first = response("first.test");
        first.status(302);
        first.fields.put("Location", "https://second.test/apk");
        var second = response("second.test");
        DownloadConnection.connect("https://first.test/apk", Map.of("Cookie", "private", "Authorization", "private", "User-Agent", "test"));
        check(second.getRequestProperty("Cookie") == null && second.getRequestProperty("Authorization") == null, "credentials omitted on redirected request");
        check("test".equals(second.getRequestProperty("User-Agent")), "user agent preserved");
        var downgrade = response("downgrade.test");
        downgrade.status(302);
        downgrade.fields.put("Location", "http://plain.test/apk");
        try { DownloadConnection.connect("https://downgrade.test/apk", Map.of()); throw new AssertionError("Downgrade accepted"); }
        catch (IOException expected) { check(downgrade.disconnected, "HTTPS downgrade rejected"); }
        System.out.println(passed + " download connection checks passed");
    }
}
