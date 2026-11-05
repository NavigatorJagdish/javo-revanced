package app.revanced.manager.downloader;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;

/** HTTP lifecycle shared by loaded downloaders. Never logs URLs, cookies or tokens. */
public final class DownloadConnection {
    private DownloadConnection() {}

    public static HttpURLConnection connect(String address, Map<String, String> headers) throws IOException {
        URL url = URI.create(address).toURL();
        URL original = url;
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (!url.getProtocol().equals("https")) throw new IOException("Download requires HTTPS");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            // Cookies and authorization belong to the original origin only.
            for (Map.Entry<String, String> header : headers.entrySet()) {
                boolean sameOrigin = original.getHost().equalsIgnoreCase(url.getHost())
                        && original.getPort() == url.getPort();
                boolean mirrorReferer = header.getKey().equalsIgnoreCase("Referer")
                        && app.revanced.manager.downloader.webview.ApkMirrorPolicy.isMirror(original.toExternalForm())
                        && app.revanced.manager.downloader.webview.ApkMirrorPolicy.isMirror(url.toExternalForm());
                if (sameOrigin || mirrorReferer || header.getKey().equalsIgnoreCase("User-Agent")) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            connection.setRequestProperty("Accept-Encoding", "identity");
            try {
                int status = connection.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Download redirect has no destination");
                    url = new URL(url, location);
                    connection.disconnect();
                    continue;
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Download server returned HTTP " + status + ". Reopen the source or select an APK from storage.");
                }
                String type = connection.getContentType();
                if (type != null && type.toLowerCase(java.util.Locale.ROOT).contains("text/html")) {
                    throw new IOException("The server returned a web page instead of an APK. Reopen the download page.");
                }
                return connection;
            } catch (IOException | RuntimeException error) {
                connection.disconnect();
                throw error;
            }
        }
        throw new IOException("Too many download redirects");
    }

    public static Long contentLength(HttpURLConnection connection) {
        String value = connection.getHeaderField("Content-Length");
        if (value == null) return null;
        try {
            long size = Long.parseLong(value);
            return size > 0 ? size : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static InputStream stream(HttpURLConnection connection) throws IOException {
        try {
            return new FilterInputStream(connection.getInputStream()) {
                @Override public void close() throws IOException {
                    try { super.close(); } finally { connection.disconnect(); }
                }
            };
        } catch (IOException error) {
            connection.disconnect();
            throw error;
        }
    }
}
