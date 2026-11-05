package app.javo.integration

import android.content.Context
import app.revanced.manager.R
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.downloader.Downloader
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.downloader.OutputDownloadScope
import app.revanced.manager.downloader.Package
import app.revanced.manager.downloader.Scope
import app.revanced.manager.downloader.download
import app.revanced.manager.network.downloader.ApkMirrorTransfer
import app.revanced.manager.network.downloader.LoadedDownloader
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.koin.core.context.GlobalContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(DownloaderHostApi::class)
internal suspend fun checkDownloads(context: Context, fixture: File, bundle: File, folder: File): JSONArray {
    val checks = JSONArray()
    val repository = GlobalContext.get().get<DownloadedAppRepository>()
    val scope = object : Scope {
        override val hostPackageName = context.packageName
        override val downloaderPackageName = context.packageName
        override val dataDir = folder
    }
    fun fake(block: suspend OutputDownloadScope.(Package, OutputStream) -> Unit): LoadedDownloader {
        val plugin = Downloader<Package>(R.string.app_name) {
            get { _, _ -> null }
            download(block)
        }.build(scope, context, context.resources)
        return LoadedDownloader(context.packageName, "fixture.Download", "Test", scope, plugin)
    }
    val payload = fixture.readBytes()
    var finalProgress: Pair<Long, Long?>? = null
    val downloaded = repository.download(
        fake { _, output ->
            val buffer = ByteArray(payload.size + 7)
            payload.copyInto(buffer, destinationOffset = 7)
            output.write(buffer, 7, payload.size)
        },
        Package("com.google.android.youtube", "fixture-1"),
        "com.google.android.youtube", "fixture-1", true, true,
        onDownload = { finalProgress = it }
    )
    check(downloaded.readBytes().contentEquals(payload))
    check(finalProgress == (payload.size.toLong() to null))
    checks.put("unknown size and nonzero buffer offset complete with exact byte count")

    try {
        repository.download(
            fake { _, _ -> delay(5_000) },
            Package("com.google.android.youtube", "fixture-1"),
            "com.google.android.youtube", "fixture-1", true, true,
            onDownload = {}, timeoutMillis = 50
        )
        error("Stalled downloader was accepted")
    } catch (expected: IOException) {
        check(expected.message.orEmpty().contains("timed out"))
    }
    check(downloaded.exists())
    checks.put("stalled plugin fails and preserves the previous valid APK")

    val merged = File(folder, "merged-video.apk")
    merged.outputStream().use { output ->
        ApkMirrorTransfer.processArchive(bundle, output, File(folder, "merge").also { it.mkdirs() }) {}
    }
    @Suppress("DEPRECATION")
    val info = checkNotNull(context.packageManager.getPackageArchiveInfo(merged.absolutePath, 0))
    check(info.packageName == "com.google.android.youtube")
    // Use the merged output in the existing patch/sign/install integration.
    merged.copyTo(fixture, overwrite = true)
    checks.put("two-split APKM serializes before resources close and parses as an APK")

    val malicious = File(folder, "unsafe.apkm")
    ZipOutputStream(malicious.outputStream()).use {
        it.putNextEntry(ZipEntry("../escaped.apk"))
        it.write(payload)
        it.closeEntry()
    }
    try {
        File(folder, "unsafe-output.apk").outputStream().use { output ->
            ApkMirrorTransfer.processArchive(malicious, output, File(folder, "unsafe").also { it.mkdirs() }) {}
        }
        error("Unsafe bundle path accepted")
    } catch (expected: IOException) {
        check(expected.message.orEmpty().contains("Unsafe"))
    }
    check(!File(folder, "escaped.apk").exists())
    checks.put("archive path traversal rejected")
    return checks
}
