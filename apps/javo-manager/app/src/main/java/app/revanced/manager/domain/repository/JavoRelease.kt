// Javo additions 2026-11-03. SPDX-License-Identifier: GPL-3.0-only
package app.revanced.manager.domain.repository
import kotlinx.serialization.Serializable
@Serializable data class JavoEnvelope(val payload: String, val signature: String)
@Serializable data class JavoRelease(
    val schemaVersion: Int, val applicationId: String, val displayName: String,
    val version: String, val versionCode: Long, val minSdk: Int,
    val architectures: List<String>, val channel: String,
    val releasedAt: String, val expiresAt: String, val artifactUrl: String,
    val sha256: String, val signerSha256: String, val releaseNotes: String,
)
