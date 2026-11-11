package app.javo.integration

import android.content.Context
import app.javo.security.UpdateSecurity
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
        val plugin = Downloader<android.os.Parcelable>(R.string.app_name) {
            get { _, _ -> null }
            download { data, output -> block(data as Package, output) }
        }.build(scope, context, context.resources)
        return LoadedDownloader(context.packageName, "fixture.Download", "Test", scope, plugin)
    }
    var finalProgress: Pair<Long, Long?>? = null
    val downloaded = repository.download(
        fake { _, output ->
            val offset = 7
            val buffer = ByteArray(64 * 1024 + offset)
            fixture.inputStream().use { input ->
                while (true) {
                    val count = input.read(buffer, offset, buffer.size - offset)
                    if (count == -1) break
                    output.write(buffer, offset, count)
                }
            }
        },
        Package("com.google.android.youtube", "fixture-1"),
        "com.google.android.youtube", "fixture-1", true, true,
        onDownload = { finalProgress = it }
    )
    check(UpdateSecurity.sha256(downloaded) == UpdateSecurity.sha256(fixture))
    check(finalProgress == (fixture.length() to null))
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
    val app = checkNotNull(info.applicationInfo).apply {
        sourceDir = merged.absolutePath
        publicSourceDir = merged.absolutePath
    }
    val resources = context.packageManager.getResourcesForApplication(app)
    val configuration = android.content.res.Configuration(resources.configuration).apply {
        setLocale(java.util.Locale("hi"))
    }
    @Suppress("DEPRECATION")
    val localized = android.content.res.Resources(resources.assets, resources.displayMetrics, configuration)
    val marker = localized.getIdentifier("app_name", "string", info.packageName)
    check(marker != 0 && localized.getString(marker) == "merged")
    // Use the merged output in the existing patch/sign/install integration.
    merged.copyTo(fixture, overwrite = true)
    checks.put("two-split APKM serializes before resources close and preserves split resources")

    val malicious = File(folder, "unsafe.apkm")
    ZipOutputStream(malicious.outputStream()).use {
        it.putNextEntry(ZipEntry("../escaped.apk"))
        fixture.inputStream().use { input -> input.copyTo(it) }
        it.closeEntry()
    }
    try {
        File(folder, "unsafe-output.apk").outputStream().use { output ->
            ApkMirrorTransfer.processArchive(malicious, output, File(folder, "unsafe").also { it.mkdirs() }) {}
        }
        error("Unsafe bundle path accepted")
    } catch (_: IOException) {
        // Android 14+ may reject the path while opening the ZIP, before extraction.
    }
    check(!File(folder, "escaped.apk").exists())
    checks.put("archive path traversal rejected")
    return checks
}
