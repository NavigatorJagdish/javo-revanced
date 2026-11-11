import app.javo.security.FrameworkCache;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FrameworkCacheTest {
    private static File framework(File directory, boolean resources) throws Exception {
        File file = new File(directory, "1.apk");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            zip.putNextEntry(new ZipEntry(resources ? "resources.arsc" : "other"));
            zip.write(new byte[]{2, 0, 12, 0, 42});
            zip.closeEntry();
        }
        return file;
    }
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("javo-framework-test-");
        Path target = root.resolve("1.apk");
        Files.write(target, new byte[]{0x50, 0x4b, 3});
        AtomicInteger calls = new AtomicInteger();
        File recovered = FrameworkCache.prepare(root.toFile(), directory -> {
            calls.incrementAndGet();
            require(Files.size(target) == 3);
            return framework(directory, true);
        });
        require(FrameworkCache.valid(recovered) && calls.get() == 1);
        FrameworkCache.prepare(root.toFile(), directory -> { throw new AssertionError("Re-extracted a valid cache"); });
        Files.write(target, new byte[]{1});
        try {
            FrameworkCache.prepare(root.toFile(), directory -> framework(directory, false));
            throw new AssertionError("Published a framework without resources");
        } catch (java.io.IOException expected) { require(Files.size(target) == 1); }
        var executor = Executors.newFixedThreadPool(2);
        calls.set(0);
        try {
            FrameworkCache.Initializer initialize = directory -> {
                calls.incrementAndGet();
                Thread.sleep(100);
                require(Files.size(target) == 1);
                return framework(directory, true);
            };
            var one = executor.submit(() -> FrameworkCache.prepare(root.toFile(), initialize));
            var two = executor.submit(() -> FrameworkCache.prepare(root.toFile(), initialize));
            require(FrameworkCache.valid(one.get()) && FrameworkCache.valid(two.get()));
            require(calls.get() == 1);
        } finally { executor.shutdownNow(); }
        Files.delete(target);
        Files.delete(root.resolve(".javo-framework.lock"));
        Files.delete(root);
        System.out.println("Framework recovery, publication and concurrent initialization checks passed");
    }
}
