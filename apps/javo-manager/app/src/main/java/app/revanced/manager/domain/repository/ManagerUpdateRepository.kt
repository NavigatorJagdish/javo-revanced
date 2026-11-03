// Javo modifications 2026-11-03: signed, isolated update feed. GPL-3.0-only
package app.revanced.manager.domain.repository

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import app.javo.security.UpdateSecurity
import app.revanced.manager.BuildConfig
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.getOrThrow
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

class ManagerUpdateRepository(private val http: HttpService, private val app: Application) {
    private val _releasedAt = MutableStateFlow<LocalDateTime?>(null)
    private val _version = MutableStateFlow<String?>(null)
    private val _hasUpdate = MutableStateFlow(false)
    val releasedAt = _releasedAt.asStateFlow()
    val version = _version.asStateFlow()
    val hasUpdate = _hasUpdate.asStateFlow()
    private var verifiedRelease: JavoRelease? = null

    suspend fun refresh(): ReVancedAsset = withContext(Dispatchers.IO) {
        clearState()
        check(BuildConfig.JAVO_UPDATE_URL.isNotEmpty() && BuildConfig.JAVO_UPDATE_KEY.isNotEmpty()) {
            "Javo updates are not published for this development build. See the packaged build report."
        }
        UpdateSecurity.requireHttps(BuildConfig.JAVO_UPDATE_URL)
        val raw = http.request<String> { url(BuildConfig.JAVO_UPDATE_URL) }.getOrThrow()
        require(raw.length <= 270000) { "Update manifest too large" }
        val envelope = Json.decodeFromString<JavoEnvelope>(raw)
        val bytes = UpdateSecurity.verifyPayload(envelope.payload, envelope.signature, BuildConfig.JAVO_UPDATE_KEY)
        val release = Json.decodeFromString<JavoRelease>(bytes.toString(Charsets.UTF_8))
        require(release.schemaVersion == 1 && release.applicationId == BuildConfig.APPLICATION_ID)
        require(release.versionCode > BuildConfig.VERSION_CODE && release.version.isNotBlank()) { "No newer Javo update available" }
        require(release.channel == "stable" && release.minSdk in 1..Build.VERSION.SDK_INT)
        require(release.architectures.any { it == "universal" || it in Build.SUPPORTED_ABIS })
        require(Regex("[0-9a-f]{64}").matches(release.sha256) && Regex("[0-9a-f]{64}").matches(release.signerSha256))
        val now = Instant.now()
        require(!Instant.parse(release.releasedAt).isAfter(now) && Instant.parse(release.expiresAt).isAfter(now)) { "Expired or future-dated update" }
        UpdateSecurity.requireHttps(release.artifactUrl)
        verifiedRelease = release
        _version.value = release.version
        _releasedAt.value = LocalDateTime.parse(release.releasedAt.removeSuffix("Z"))
        _hasUpdate.value = true
        asset(release)
    }

    private fun asset(release: JavoRelease) = ReVancedAsset(
        downloadUrl = release.artifactUrl,
        createdAt = LocalDateTime.parse(release.releasedAt.removeSuffix("Z")),
        description = release.releaseNotes, version = release.version,
    )

    suspend fun getUpdateOrNull(refetch: Boolean = false): ReVancedAsset? =
        if (refetch || verifiedRelease == null) refresh() else verifiedRelease?.let(::asset)

    @Suppress("DEPRECATION")
    fun verifyArtifact(file: File) {
        val release = checkNotNull(verifiedRelease) { "Check the signed Javo manifest before installing" }
        require(Instant.parse(release.expiresAt).isAfter(Instant.now())) { "Update manifest expired" }
        require(UpdateSecurity.sha256(file) == release.sha256) { "APK checksum mismatch" }
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = requireNotNull(app.packageManager.getPackageArchiveInfo(file.absolutePath, flags)) { "Invalid APK" }
        val installed = app.packageManager.getPackageInfo(BuildConfig.APPLICATION_ID, flags)
        require(archive.packageName == BuildConfig.APPLICATION_ID) { "Wrong update package" }
        val code = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        require(code == release.versionCode && archive.versionName == release.version) { "APK version differs from signed metadata" }
        fun signers(info: android.content.pm.PackageInfo): Set<String> {
            val certs = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            return certs.orEmpty().map { UpdateSecurity.sha256(it.toByteArray()) }.toSet()
        }
        val expected = setOf(release.signerSha256)
        require(signers(archive) == expected && signers(installed) == expected) { "APK signing identity differs from installed Javo" }
    }

    fun clearState() {
        verifiedRelease = null
        _releasedAt.value = null
        _version.value = null
        _hasUpdate.value = false
    }
}
