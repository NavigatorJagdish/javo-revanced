// SPDX-License-Identifier: GPL-3.0-only
package app.javo.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Publishes only complete Apktool frameworks, including after interrupted patching. */
public final class FrameworkCache {
    private static final long MAX_RESOURCES = 64L * 1024 * 1024;
    private FrameworkCache() {}
    public interface Initializer { File create(File directory) throws Exception; }

    public static synchronized File prepare(File directory, Initializer initializer) throws Exception {
        Path root = directory.toPath();
        Files.createDirectories(root);
        Path target = root.resolve("1.apk");
        try (FileChannel channel = FileChannel.open(root.resolve(".javo-framework.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var ignored = channel.lock()) {
            if (valid(target.toFile())) return target.toFile();
            Path staging = Files.createTempDirectory(root, ".javo-framework-");
            try {
                File created = initializer.create(staging.toFile());
                if (!created.getCanonicalFile().equals(staging.resolve("1.apk").toFile().getCanonicalFile())
                        || !valid(created)) throw new IOException("Bundled Android framework is incomplete");
                try {
                    Files.move(created.toPath(), target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(created.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                }
                return target.toFile();
            } finally {
                Files.deleteIfExists(staging.resolve("1.apk"));
                Files.deleteIfExists(staging);
            }
        }
    }

    public static boolean valid(File file) {
        if (!file.isFile() || file.length() < 22 || file.length() > MAX_RESOURCES * 2) return false;
        try (ZipFile archive = new ZipFile(file)) {
            ZipEntry resources = archive.getEntry("resources.arsc");
            if (resources == null || resources.getSize() <= 0 || resources.getSize() > MAX_RESOURCES) return false;
            CRC32 crc = new CRC32();
            long count = 0;
            try (InputStream input = archive.getInputStream(resources)) {
                byte[] buffer = new byte[65536];
                for (int read; (read = input.read(buffer)) != -1;) {
                    count += read;
                    if (count > MAX_RESOURCES) return false;
                    crc.update(buffer, 0, read);
                }
            }
            return count == resources.getSize() && crc.getValue() == resources.getCrc();
        } catch (IOException invalid) {
            return false;
        }
    }
}
