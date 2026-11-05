package app.revanced.manager.domain.repository

import android.app.Application
import android.content.Context
import android.os.Parcelable
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.AppDatabase.Companion.generateUid
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.network.downloader.ApkMirrorTransfer
import app.revanced.manager.network.downloader.ApkDownloadStage
import app.revanced.manager.downloader.DownloadUrl
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.downloader.OutputDownloadScope
import app.revanced.manager.downloader.Scope
import app.revanced.manager.util.PM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FilterOutputStream
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.outputStream

class DownloadedAppRepository(
    app: Application,
    db: AppDatabase,
    private val pm: PM
) {
    private val dir = app.getDir("downloaded-apps", Context.MODE_PRIVATE)
    private val dao = db.downloadedAppDao()

    fun getAll() = dao.getAllApps().distinctUntilChanged()

    fun getApkFileForApp(app: DownloadedApp): File =
        getApkFileForDir(dir.resolve(app.directory))

    private fun getApkFileForDir(directory: File) = directory.listFiles()!!.first()

    suspend fun cleanUp() {
        val threshold = 1000 * 60 * 60 * 6
        val now = System.currentTimeMillis()

        val targets = getAll().first().filter {
            (now - it.lastUsed) > threshold
        }
        delete(targets)
    }

    suspend fun deleteFor(packageName: String) {
        delete(getAll().first().filter { it.packageName == packageName })
    }

    @OptIn(DownloaderHostApi::class)
    suspend fun download(
        downloader: LoadedDownloader,
        data: Parcelable,
        expectedPackageName: String,
        expectedVersion: String?,
        appCompatibilityCheck: Boolean,
        patchesCompatibilityCheck: Boolean,
        onDownload: suspend (downloadProgress: Pair<Long, Long?>) -> Unit,
        onStage: suspend (ApkDownloadStage) -> Unit = {},
        timeoutMillis: Long = 15 * 60 * 1000L,
    ): File {
        // Converted integers cannot contain / or .. unlike the package name or version, so they are safer to use here.
        val relativePath = File(generateUid().toString())
        val saveDir = dir.resolve(relativePath).also { it.mkdirs() }
        val targetFile = saveDir.resolve("base.apk").toPath()

        var committed = false
        try {
            val downloadSize = AtomicLong(0)
            val downloadedBytes = AtomicLong(0)

            try {
                withTimeout(timeoutMillis) {
                    channelFlow {
                        val scope = object : OutputDownloadScope, Scope by downloader.scopeImpl {
                            override suspend fun reportSize(size: Long) {
                                require(size > 0) { "Size must be greater than zero" }
                                require(
                                    downloadSize.compareAndSet(
                                        0,
                                        size
                                    )
                                ) { "Download size has already been set" }
                                send(downloadedBytes.get() to size)
                            }
                        }

                        val downloadContext = currentCoroutineContext()
                        val managedMirror = data is DownloadUrl && downloader.className.contains(".apkmirror.")

                        fun emitProgress(bytes: Long) {
                            downloadContext.ensureActive()
                            val newValue = downloadedBytes.addAndGet(bytes)
                            val totalSize = downloadSize.get()
                            if (!managedMirror) trySend(newValue to totalSize.takeIf { it > 0 })
                        }

                        targetFile.outputStream(StandardOpenOption.CREATE_NEW).buffered().use {
                            val stream = object : FilterOutputStream(it) {
                                override fun write(b: Int) = out.write(b).also { emitProgress(1) }

                                override fun write(b: ByteArray?, off: Int, len: Int) =
                                    out.write(b, off, len).also {
                                        emitProgress(
                                            len.toLong()
                                        )
                                    }
                            }
                            if (managedMirror) {
                                ApkMirrorTransfer.download(data as DownloadUrl, stream,
                                    onProgress = { send(it) }, onStage = onStage)
                            } else {
                                onStage(ApkDownloadStage.DOWNLOADING)
                                downloader.impl.download(scope, data, stream)
                            }
                        }
                        if (!managedMirror) send(downloadedBytes.get() to downloadSize.get().takeIf { it > 0 })
                    }
                        .conflate()
                        .flowOn(Dispatchers.IO)
                        .collect { (downloaded, size) -> onDownload(downloaded to size) }
                }
            } catch (error: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                throw IOException("Download or split merging timed out. Retry, or select a standalone APK from storage.", error)
            }

            if (downloadedBytes.get() < 1) error("Downloader did not download anything.")
            val pkgInfo =
                pm.getPackageInfo(targetFile.toFile()) ?: error("Downloaded APK file is invalid")
            if (pkgInfo.packageName != expectedPackageName) error("Downloaded APK has the wrong package name. Expected: $expectedPackageName, Actual: ${pkgInfo.packageName}")
            expectedVersion?.let {
                if (
                    pkgInfo.versionName != expectedVersion &&
                    (appCompatibilityCheck || patchesCompatibilityCheck)
                ) error("The selected app version ($pkgInfo.versionName) doesn't match the suggested version. Please use the suggested version ($expectedVersion), or adjust your settings by disabling \"Require suggested app version\" and enabling \"Disable version compatibility check\".")
            }

            val previousDirectory = dao.get(pkgInfo.packageName, pkgInfo.versionName!!)?.directory
            withContext(NonCancellable) {
                dao.upsert(
                    DownloadedApp(
                        packageName = pkgInfo.packageName,
                        version = pkgInfo.versionName!!,
                        directory = relativePath,
                    )
                )
                committed = true
            }
            // Commit the replacement before retiring the previous valid APK.
            previousDirectory?.let { dir.resolve(it).deleteRecursively() }
        } catch (e: Exception) {
            if (!committed) saveDir.deleteRecursively()
            throw e
        }

        // Return the Apk file.
        return getApkFileForDir(saveDir)
    }

    suspend fun get(packageName: String, version: String, markUsed: Boolean = false) =
        dao.get(packageName, version)?.also {
            if (markUsed) dao.markUsed(packageName, version)
        }

    suspend fun getAllByPackage(packageName: String) = dao.getAllByPackage(packageName)

    suspend fun getLatestByPackage(packageName: String, markUsed: Boolean = false) =
        dao.getLatestByPackage(packageName)?.also {
            if (markUsed) dao.markUsed(it.packageName, it.version)
        }

    suspend fun delete(downloadedApps: Collection<DownloadedApp>) {
        downloadedApps.forEach {
            dir.resolve(it.directory).deleteRecursively()
        }

        dao.delete(downloadedApps)
    }
}
