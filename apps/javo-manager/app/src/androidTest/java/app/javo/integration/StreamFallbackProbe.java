package app.javo.integration;

import dalvik.system.DexClassLoader;
import java.lang.reflect.Method;
import org.json.JSONObject;

/** Runs the transformed real extension on Android; does not contact YouTube. */
public final class StreamFallbackProbe {
    private static Class<?> format, stream;
    private static Method guard;
    private static Object format(String url, String cipher) throws Exception {
        Object builder = format.getMethod("newBuilder").invoke(null);
        builder.getClass().getMethod("setUrl", String.class).invoke(builder, url);
        builder.getClass().getMethod("setSignatureCipher", String.class).invoke(builder, cipher);
        return builder.getClass().getMethod("build").invoke(builder);
    }
    private static void check(String name, int expected, Object... formats) throws Exception {
        Object builder = stream.getMethod("newBuilder").invoke(null);
        for (Object value : formats) builder.getClass().getMethod("addAdaptiveFormats", format).invoke(builder, value);
        Object data = builder.getClass().getMethod("build").invoke(builder);
        int result = (Integer) guard.invoke(null, data);
        if (result != expected) throw new AssertionError(name + ": " + result + " != " + expected);
        System.out.println("PASS " + name);
    }
    private static void checkParser(ClassLoader loader, boolean usable) throws Exception {
        Object streamBuilder = stream.getMethod("newBuilder").invoke(null);
        streamBuilder.getClass().getMethod("addAdaptiveFormats", format).invoke(streamBuilder,
            format(usable ? "https://media.invalid/video" : "", ""));
        Object data = streamBuilder.getClass().getMethod("build").invoke(streamBuilder);
        Class<?> response = loader.loadClass("app.revanced.extension.shared.innertube.PlayerResponseOuterClass$PlayerResponse");
        Object builder = response.getMethod("newBuilder").invoke(null);
        builder.getClass().getMethod("setStreamingData", stream).invoke(builder, data);
        Object message = builder.getClass().getMethod("build").invoke(builder);
        final byte[] bytes = (byte[]) response.getMethod("toByteArray").invoke(message);
        final boolean[] closed = { false };
        java.net.HttpURLConnection connection = new java.net.HttpURLConnection(new java.net.URL("https://fixture.invalid")) {
            public void disconnect() { }
            public boolean usingProxy() { return false; }
            public void connect() { }
            public int getContentLength() { return bytes.length; }
            public java.io.InputStream getInputStream() {
                return new java.io.ByteArrayInputStream(bytes) {
                    public void close() { closed[0] = true; }
                };
            }
        };
        Class<?> client = loader.loadClass("app.revanced.extension.shared.spoof.ClientType");
        Object vr = client.getField("ANDROID_VR_1_43_32").get(null);
        Method parser = guard.getDeclaringClass().getDeclaredMethod("buildPlayerResponseBuffer", client, java.net.HttpURLConnection.class);
        parser.setAccessible(true);
        Object result = parser.invoke(null, vr, connection);
        if ((result != null) != usable || !closed[0]) throw new AssertionError("Parser fallback or input close failed");
        System.out.println("PASS parser-" + (usable ? "usable-preserved" : "SABR-fallback-and-close"));
    }
    private static void checkClientPublication(ClassLoader loader) throws Exception {
        Class<?> request = guard.getDeclaringClass();
        Class<?> client = loader.loadClass("app.revanced.extension.shared.spoof.ClientType");
        Object vr = client.getField("ANDROID_VR_1_43_32").get(null);
        Object reel = client.getField("ANDROID_REEL").get(null);
        Object creator = client.getField("ANDROID_CREATOR").get(null);
        Method setter = request.getMethod("setClientOrderToUse", java.util.List.class, client);
        java.lang.reflect.Field field = request.getDeclaredField("clientOrderToUse");
        field.setAccessible(true);
        setter.invoke(null, java.util.Arrays.asList(vr, reel), vr);
        Object baseline = field.get(null);
        final java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch resume = new java.util.concurrent.CountDownLatch(1);
        final java.util.List<Object> values = java.util.Arrays.asList(reel, vr, creator);
        java.util.List<Object> paused = new java.util.AbstractList<Object>() {
            public int size() { return values.size(); }
            public Object get(int index) { return values.get(index); }
            public boolean contains(Object value) { return values.contains(value); }
            public java.util.Iterator<Object> iterator() {
                final java.util.Iterator<Object> iterator = values.iterator();
                return new java.util.Iterator<Object>() {
                    boolean first = true;
                    public boolean hasNext() {
                        if (first) {
                            first = false;
                            entered.countDown();
                            try {
                                if (!resume.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Client publisher wait timed out");
                            } catch (InterruptedException error) { throw new RuntimeException(error); }
                        }
                        return iterator.hasNext();
                    }
                    public Object next() { return iterator.next(); }
                };
            }
        };
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Thread publisher = new Thread(() -> {
            try { setter.invoke(null, paused, reel); } catch (Throwable error) { failure.set(error); }
        });
        publisher.start();
        boolean unpublished;
        try {
            if (!entered.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Client publisher never reached iterator");
            unpublished = field.get(null) == baseline;
        } finally { resume.countDown(); }
        publisher.join(10_000);
        Object[] result = (Object[]) field.get(null);
        if (!unpublished || publisher.isAlive() || failure.get() != null ||
            !java.util.Arrays.equals(result, new Object[]{reel, vr, creator}))
            throw new AssertionError("Client list was published before it was complete", failure.get());
        System.out.println("PASS atomic-client-list-publication");
        setter.invoke(null, java.util.Arrays.asList(vr, vr, reel), vr);
        if (!java.util.Arrays.equals((Object[]) field.get(null), new Object[]{vr, reel}))
            throw new AssertionError("Duplicate preferred clients left null entries");
        System.out.println("PASS duplicate-preferred-client");
    }
    public static JSONObject runChecks(ClassLoader loader, android.content.Context context) throws Exception {
        java.lang.reflect.Field extensionContext = loader.loadClass("app.revanced.extension.shared.Utils").getDeclaredField("context");
        extensionContext.setAccessible(true);
        extensionContext.set(null, new android.content.ContextWrapper(context) {
            public android.content.SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("javo_stream_regression_" + name, mode);
            }
        });
        format = loader.loadClass("app.revanced.extension.shared.innertube.PlayerResponseOuterClass$Format");
        stream = loader.loadClass("app.revanced.extension.shared.innertube.PlayerResponseOuterClass$StreamingData");
        Class<?> request = loader.loadClass("app.revanced.extension.shared.spoof.requests.StreamingDataRequest");
        guard = request.getMethod("javoPlayableAdaptiveFormats", stream);
        Object empty = format("", "");
        check("empty-response", 0);
        check("SABR-only-response-rejected", 0, empty);
        check("direct-URL-preserved", 1, format("https://media.invalid/video", ""));
        check("cipher-preserved", 1, format("", "url=https%3A%2F%2Fmedia.invalid%2Fvideo&s=test"));
        check("mixed-formats", 2, empty, format("https://media.invalid/audio", ""));
        Object[] many = new Object[1000];
        java.util.Arrays.fill(many, empty);
        check("many-unusable-formats", 0, many);
        many[999] = format("https://media.invalid/last", "");
        check("last-format-usable", 1000, many);
        checkParser(loader, false);
        checkParser(loader, true);
        checkClientPublication(loader);
        java.lang.reflect.Field destination = loader.loadClass("app.revanced.extension.shared.checks.Check").getDeclaredField("GOOD_SOURCE");
        destination.setAccessible(true);
        if (!destination.get(null).toString().equals("https://navigatorjagdish.github.io/javo-revanced/docs/installation/"))
            throw new AssertionError("Update/help action does not use the Javo guide");
        System.out.println("PASS Javo-update-help-URI");
        return new JSONObject().put("success", true).put("androidApi", android.os.Build.VERSION.SDK_INT)
            .put("cases", 12).put("realNetworkPlayback", "not-tested");
    }
    public static void main(String[] args) throws Exception {
        android.os.Looper.prepareMainLooper();
        ClassLoader loader = new DexClassLoader(args[0], args[1], null, StreamFallbackProbe.class.getClassLoader());
        // The standalone shell probe has no installed app. Supply default-only
        // preferences; instrumentation uses the real target Context instead.
        android.content.Context context = new android.content.ContextWrapper(null) {
            public android.content.res.Resources getResources() { return android.content.res.Resources.getSystem(); }
            public String getPackageName() { return "app.javo.fixture"; }
            public android.content.SharedPreferences getSharedPreferences(String name, int mode) {
                final android.content.SharedPreferences.Editor editor = (android.content.SharedPreferences.Editor)
                    java.lang.reflect.Proxy.newProxyInstance(getClassLoader(), new Class[]{android.content.SharedPreferences.Editor.class},
                        (proxy, method, values) -> method.getReturnType() == boolean.class ? true :
                            method.getReturnType() == void.class ? null : proxy);
                return (android.content.SharedPreferences) java.lang.reflect.Proxy.newProxyInstance(getClassLoader(),
                    new Class[]{android.content.SharedPreferences.class}, (proxy, method, values) -> {
                        if (method.getName().equals("edit")) return editor;
                        if (method.getName().equals("getAll")) return java.util.Collections.emptyMap();
                        if (method.getName().startsWith("get")) return values[1];
                        if (method.getReturnType() == boolean.class) return false;
                        return null;
                    });
            }
            public ClassLoader getClassLoader() { return StreamFallbackProbe.class.getClassLoader(); }
        };
        System.out.println(runChecks(loader, context));
        System.exit(0);
    }
}
