package app.revanced.manager.data.platform

import android.app.Application
import android.content.Context
import java.io.File
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.nio.file.Files

class Filesystem(private val app: Application) {
    val contentResolver = app.contentResolver // TODO: move Content Resolver operations to here.

    /**
     * A directory that gets cleared when the app restarts.
     * Do not store paths to this directory in a parcel.
     */
    val tempDir: File = app.getDir("ephemeral", Context.MODE_PRIVATE).apply {
        deleteRecursively()
        mkdirs()
    }

    /**
     * A directory for storing temporary files related to UI.
     * This is the same as [tempDir], but does not get cleared on system-initiated process death.
     * Paths to this directory can be safely stored in parcels.
     */
    val uiTempDir: File = File(app.filesDir, "ui_ephemeral").apply { mkdirs() }

    /** Only the document chosen in Android's picker is copied into the app's storage. */
    suspend fun importPatchOption(uri: Uri): String = withContext(Dispatchers.IO) {
        val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            ?.replace(Regex("[^A-Za-z0-9._-]"), "_")?.take(120)
            ?.takeUnless { it.isBlank() || it == "." || it == ".." } ?: "selected-file"
        val root = File(app.filesDir, "patch_options").apply { mkdirs() }
        val folder = Files.createTempDirectory(root.toPath(), "option-").toFile()
        val output = folder.resolve(name)
        var copied = false
        try {
            checkNotNull(contentResolver.openInputStream(uri)).use { input ->
                output.outputStream().buffered().use { target ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 64L * 1024 * 1024) { "The selected file exceeds 64 MiB." }
                        target.write(buffer, 0, count)
                    }
                }
            }
            copied = true
            output.absolutePath
        } finally {
            if (!copied) { output.delete(); folder.delete() }
        }
    }
}
