package app.revanced.manager.domain.repository

import android.app.Application
import android.content.Context
import android.os.Parcelable
import android.net.Uri
import app.revanced.manager.downloader.Downloader
import app.revanced.manager.downloader.webview.WebViewDownloader
import kotlinx.coroutines.flow.flowOf
import app.revanced.manager.R
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.downloader.DownloaderEntity
import app.revanced.manager.data.room.sources.SourceProperties
import app.revanced.manager.domain.manager.SourceManager
import app.revanced.manager.domain.sources.Loader
import app.revanced.manager.domain.sources.LocalSource
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.network.downloader.ParceledDownloaderData
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.downloader.Scope
import app.revanced.manager.network.downloader.DownloaderPackage
import kotlinx.coroutines.flow.map

@OptIn(DownloaderHostApi::class)
class DownloaderRepository(
    app: Application,
    db: AppDatabase
) : SourceManager<DownloaderEntity, DownloaderPackage, List<LoadedDownloader>>(
    emptyList(),
    app.getDir("downloaders", Context.MODE_PRIVATE)
) {
    private val dao = db.downloaderDao()

    override suspend fun dbGetAll() = dao.all()
    override suspend fun dbGetProps(uid: Int) = dao.getProps(uid)
    override suspend fun dbUpsert(entity: DownloaderEntity) = dao.upsert(entity)
    override suspend fun dbRemove(uid: Int) = dao.remove(uid)
    override suspend fun dbReset() = dao.reset()

    private val loader = Loader<DownloaderPackage> {
        error("External downloader plugins are disabled. APKMirror is built into Manager.")
    }

    private val builtIn = run {
        val identity = app.packageName
        val scope = object : Scope {
            override val hostPackageName = identity
            override val downloaderPackageName = identity
            override val dataDir = app.cacheDir
        }
        val impl = WebViewDownloader(R.string.javo_apkmirror_builtin) { pkg, version ->
            if (pkg !in setOf("com.google.android.youtube", "com.google.android.apps.youtube.music")) null
            else Uri.Builder().scheme("https").authority("www.apkmirror.com")
                .appendQueryParameter("s", listOfNotNull(pkg, version).joinToString(" "))
                .build().toString()
        }.build(scope, app, app.resources)
        @Suppress("UNCHECKED_CAST")
        LoadedDownloader(identity, "app.javo.apkmirror.BuiltIn", "APKMirror", scope, impl as Downloader<Parcelable>)
    }

    override fun loadEntity(entity: DownloaderEntity): Source<DownloaderPackage> = with(entity) {
        val file = directoryOf(uid).resolve("downloader.jar")
        val actualName = name.ifEmpty { app.getString(R.string.source_name_fallback) }

        // Treat saved plugin entries as inert data during upgrades.
        return LocalSource(actualName, uid, null, file, loader)
    }

    override fun entityFromProps(
        uid: Int,
        props: SourceProperties
    ) = DownloaderEntity(
        uid,
        name = props.name,
        versionHash = props.versionHash,
        source = props.source,
        autoUpdate = props.autoUpdate,
        releasedAt = props.releasedAt
    )

    override fun realNameOf(loaded: DownloaderPackage) = loaded.name

    override val updateFailed = R.string.downloader_update_failed
    override val updateSuccess = R.string.patches_update_success
    override val updateUnavailable = R.string.patches_update_unavailable
    override val replaceFail = R.string.downloader_replace_fail

    override suspend fun loadDataFromSources(sources: MutableMap<Int, Source<DownloaderPackage>>) =
        sources.values.flatMap { src -> src.loaded?.downloaders.orEmpty() }

    val downloaderSources = store.state.map { it.sources }
    val loadedDownloadersFlow = flowOf(listOf(builtIn))

    fun unwrapParceledData(data: ParceledDownloaderData): Pair<LoadedDownloader, Parcelable> {
        require(data.downloaderPackageName == builtIn.packageName && data.downloaderClassName == builtIn.className) {
            "Choose APKMirror again to start a new download."
        }
        return builtIn to data.unwrapWith(app.classLoader)
    }
}
