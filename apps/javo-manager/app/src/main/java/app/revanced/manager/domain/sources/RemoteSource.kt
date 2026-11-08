package app.revanced.manager.domain.sources

import app.revanced.manager.data.redux.ActionContext
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.APIResponse
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.patcher.patch.PatchBundle
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FilterOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.datetime.LocalDateTime

typealias RemotePatchBundle = RemoteSource<PatchBundle>
typealias JsonPatchBundle = JsonSource<PatchBundle>
typealias APIPatchBundle = APISource<PatchBundle>

sealed class RemoteSource<T>(
    name: String,
    uid: Int,
    protected val versionHash: String?,
    val releasedAt: LocalDateTime?,
    error: Throwable?,
    file: File,
    val endpoint: String,
    val autoUpdate: Boolean,
    loader: Loader<T>
) : Source<T>(name, uid, error, file, loader), KoinComponent {
    data class UpdateResult(val versionHash: String, val releasedAt: LocalDateTime)

    protected val http: HttpService by inject()

    protected abstract suspend fun getLatestInfo(): ReVancedAsset
    abstract fun copy(
        error: Throwable? = this.error,
        name: String = this.name,
        autoUpdate: Boolean = this.autoUpdate,
        versionHash: String? = this.versionHash,
        releasedAt: LocalDateTime? = this.releasedAt
    ): RemoteSource<T>

    override fun copy(error: Throwable?, name: String): RemoteSource<T> =
        copy(error, name, this.autoUpdate, this.versionHash, this.releasedAt)

    private suspend fun download(info: ReVancedAsset) = withContext(Dispatchers.IO) {
        val staged = File.createTempFile("source-", ".download", checkNotNull(file.parentFile))
        try {
            LimitedOutputStream(staged.outputStream().buffered()).use {
                http.streamTo(it) { url(info.downloadUrl) }
            }
            loader.verifyDownload(staged)
            check(staged.setReadOnly()) { "Could not protect the verified patch bundle" }
            Files.move(staged.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            staged.delete()
        }

        UpdateResult(info.version, info.createdAt)
    }

    /**
     * Downloads the latest version regardless if there is a new update available.
     */
    suspend fun ActionContext.downloadLatest() = download(getLatestInfo())
    suspend fun ActionContext.getUpdateInfo() =
        getLatestInfo().takeUnless { hasInstalled() && it.version == versionHash }

    suspend fun ActionContext.update(): UpdateResult? = withContext(Dispatchers.IO) {
        getUpdateInfo()?.let { download(it) }
    }

    companion object {
        const val updateFailMsg = "Failed to update"
    }

    private class LimitedOutputStream(output: OutputStream) : FilterOutputStream(output) {
        private var remaining = 32L * 1024 * 1024

        override fun write(value: Int) {
            reserve(1)
            out.write(value)
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            reserve(length)
            out.write(bytes, offset, length)
        }

        private fun reserve(length: Int) {
            require(length in 0..remaining) { "Remote patch bundle exceeds 32 MiB" }
            remaining -= length
        }
    }
}

class JsonSource<T>(
    name: String,
    uid: Int,
    versionHash: String?,
    releasedAt: LocalDateTime?,
    error: Throwable?,
    file: File,
    endpoint: String,
    autoUpdate: Boolean,
    loader: Loader<T>
) : RemoteSource<T>(name, uid, versionHash, releasedAt, error, file, endpoint, autoUpdate, loader) {
    override suspend fun getLatestInfo() = withContext(Dispatchers.IO) {
        http.request<ReVancedAsset> {
            url(endpoint)
        }.getOrThrow()
    }

    override fun copy(
        error: Throwable?,
        name: String,
        autoUpdate: Boolean,
        versionHash: String?,
        releasedAt: LocalDateTime?
    ) = JsonSource(
        name,
        uid,
        versionHash,
        releasedAt,
        error,
        file,
        endpoint,
        autoUpdate,
        loader
    )
}

class APISource<T>(
    name: String,
    uid: Int,
    versionHash: String?,
    releasedAt: LocalDateTime?,
    error: Throwable?,
    file: File,
    endpoint: String,
    autoUpdate: Boolean,
    loader: Loader<T>,
    private val getUpdate: suspend ReVancedAPI.() -> APIResponse<ReVancedAsset>
) : RemoteSource<T>(name, uid, versionHash, releasedAt, error, file, endpoint, autoUpdate, loader) {
    private val api: ReVancedAPI by inject()

    override suspend fun getLatestInfo() = api.getUpdate().getOrThrow()
    override fun copy(
        error: Throwable?,
        name: String,
        autoUpdate: Boolean,
        versionHash: String?,
        releasedAt: LocalDateTime?
    ) = APISource(
        name,
        uid,
        versionHash,
        releasedAt,
        error,
        file,
        endpoint,
        autoUpdate,
        loader,
        getUpdate
    )
}
