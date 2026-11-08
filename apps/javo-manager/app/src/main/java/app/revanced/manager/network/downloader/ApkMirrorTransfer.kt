/*
 * Javo APKMirror transfer path, adapted from ReVanced Manager Downloaders.
 * Upstream APKMirrorDownloader.kt and Merger.kt, GPL-3.0.
 * Keeps merged modules open until serialization has finished.
 */
package app.revanced.manager.network.downloader

import app.revanced.manager.downloader.DownloadUrl
import com.reandroid.apk.ApkBundle
import com.reandroid.app.AndroidManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.util.zip.ZipFile

enum class ApkDownloadStage { DOWNLOADING, IMPORTING, MERGING, WRITING, VALIDATING }

object ApkMirrorTransfer {
    private const val MAX_BYTES = 2L * 1024 * 1024 * 1024

    suspend fun download(
        request: DownloadUrl,
        output: OutputStream,
        onProgress: suspend (Pair<Long, Long?>) -> Unit,
        onStage: suspend (ApkDownloadStage) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val work = Files.createTempDirectory("javo-apkmirror-").toFile()
        try {
            val archive = File(work, "download.zip")
            onStage(ApkDownloadStage.DOWNLOADING)
            try {
                val (stream, total) = request.toDownloadResult()
                stream.use { input ->
                    require(total == null || total <= MAX_BYTES) { "Download is too large" }
                    archive.outputStream().buffered().use { target ->
                        val buffer = ByteArray(64 * 1024)
                        var bytes = 0L
                        var lastReport = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count == -1) break
                            bytes += count
                            if (bytes > MAX_BYTES) throw IOException("Download exceeds 2 GiB")
                            target.write(buffer, 0, count)
                            val now = System.nanoTime()
                            if (now - lastReport >= 250_000_000L) {
                                onProgress(bytes to total)
                                lastReport = now
                            }
                        }
                        if (bytes == 0L || (total != null && bytes != total)) throw IOException("Download is incomplete")
                        onProgress(bytes to total)
                    }
                }
            } catch (error: java.net.SocketTimeoutException) {
                throw IOException("The APK source stopped responding. Retry the download or use a file from Downloads.", error)
            }
            processArchive(archive, output, work, onStage)
        } finally {
            work.deleteRecursively()
        }
    }

    suspend fun importArchive(archive: File, output: OutputStream, onStage: suspend (ApkDownloadStage) -> Unit) = withContext(Dispatchers.IO) {
        require(archive.length() in 1..MAX_BYTES) { "Selected archive is empty or exceeds 2 GiB" }
        val work = Files.createTempDirectory("javo-apk-import-").toFile()
        try {
            onStage(ApkDownloadStage.IMPORTING)
            processArchive(archive, output, work, onStage)
        } finally { work.deleteRecursively() }
    }

    internal suspend fun processArchive(
        archive: File, output: OutputStream, work: File,
        onStage: suspend (ApkDownloadStage) -> Unit,
    ) {
        ZipFile(archive).use { zip ->
            if (zip.getEntry("AndroidManifest.xml") != null) {
                onStage(ApkDownloadStage.WRITING)
                copyToOutput(archive, output)
            } else {
                onStage(ApkDownloadStage.MERGING)
                val splits = File(work, "splits").also { it.mkdirs() }
                extractSplits(zip, splits)
                currentCoroutineContext().ensureActive()
                ApkBundle().use { bundle ->
                    bundle.loadApkDirectory(splits)
                    val merged = bundle.mergeModules()
                    try {
                        merged.androidManifest.apply {
                            arrayOf(AndroidManifest.ID_isSplitRequired, AndroidManifest.ID_extractNativeLibs).forEach {
                                applicationElement.removeAttributesWithId(it)
                                manifestElement.removeAttributesWithId(it)
                            }
                            arrayOf(AndroidManifest.NAME_requiredSplitTypes, AndroidManifest.NAME_splitTypes).forEach { name ->
                                manifestElement.removeAttributeIf { it.name == name }
                            }
                            applicationElement.removeElementsIf { element ->
                                element.name == AndroidManifest.TAG_meta_data &&
                                    element.getAttributes { it.nameId == AndroidManifest.ID_name }
                                        .asSequence().any { it.valueString.orEmpty().matches(Regex("^com\\.android\\.(stamp|vending)\\..*")) }
                            }
                            refresh()
                        }
                        currentCoroutineContext().ensureActive()
                        onStage(ApkDownloadStage.WRITING)
                        // ARSCLib's OutputStream overload buffers entries in heap to
                        // compute ZIP sizes. Its file writer uses a seekable disk buffer.
                        val mergedFile = File(work, "merged.apk")
                        merged.writeApk(mergedFile)
                        copyToOutput(mergedFile, output)
                    } finally {
                        if (merged !in bundle.modules) merged.close()
                    }
                }
            }
        }
    }

    private suspend fun copyToOutput(file: File, output: OutputStream) {
        val context = currentCoroutineContext()
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                context.ensureActive()
                val count = input.read(buffer)
                if (count == -1) break
                output.write(buffer, 0, count)
            }
        }
    }

    internal suspend fun extractSplits(zip: ZipFile, directory: File) {
        val root = directory.toPath().toAbsolutePath().normalize()
        var files = 0
        var expandedBytes = 0L
        for (entry in zip.entries().asSequence()) {
            currentCoroutineContext().ensureActive()
            val target = root.resolve(entry.name).normalize()
            if (!target.startsWith(root) || entry.name.contains('\\')) throw IOException("Unsafe bundle entry")
            if (entry.isDirectory || !entry.name.endsWith(".apk", ignoreCase = true)) continue
            if (++files > 512) throw IOException("Too many bundle splits")
            Files.createDirectories(target.parent)
            // CREATE_NEW also rejects duplicate archive names.
            Files.newOutputStream(target, java.nio.file.StandardOpenOption.CREATE_NEW).use { output ->
                zip.getInputStream(entry).use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count == -1) break
                        expandedBytes += count
                        if (expandedBytes > MAX_BYTES) throw IOException("Expanded bundle exceeds 2 GiB")
                        output.write(buffer, 0, count)
                    }
                }
            }
        }
        if (files == 0) throw IOException("Download contains no APK files")
    }
}
