package app.revanced.manager.video

import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat as AndroidMediaFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.URI
import javax.net.ssl.HttpsURLConnection

internal object VideoDownload {
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    private const val VIDEO_LIMIT = 2L * 1024 * 1024 * 1024
    private const val METADATA_LIMIT = 8 * 1024 * 1024
    private val videoId = Regex("[A-Za-z0-9_-]{11}")
    private val youtubeHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be")
    private val extractorHosts = youtubeHosts + setOf("youtubei.googleapis.com", "www.youtube-nocookie.com", "s.ytimg.com")
    private val engine by lazy { NewPipe.init(YouTubeConnection()); ServiceList.YouTube }

    enum class Format(val extension: String, val mimeType: String) {
        VIDEO("mp4", "video/mp4"),
        AUDIO("m4a", "audio/mp4"),
    }

    data class FileSource(val url: URI, val title: String, val youtube: Boolean, val format: Format)

    fun input(text: String): URI {
        val value = Regex("https://[^\\s<>]+").find(text.trim().take(8192))?.value
            ?: throw IllegalArgumentException("Paste a YouTube video link or a direct HTTPS MP4 or M4A link.")
        val uri = checkedUri(value)
        if (uri.host.lowercase() !in youtubeHosts) {
            require(Format.entries.any { uri.path.orEmpty().endsWith("." + it.extension, true) }) {
                "A direct link must end in .mp4 or .m4a."
            }
            return uri
        }
        val path = uri.path.orEmpty()
        val id = when {
            uri.host.equals("youtu.be", true) -> path.removePrefix("/")
            path == "/watch" -> Uri.parse(uri.toString()).getQueryParameter("v")
            path.startsWith("/shorts/") -> path.removePrefix("/shorts/")
            else -> null
        }
        require(id != null && videoId.matches(id)) { "Choose a single YouTube video or Short." }
        return URI.create("https://www.youtube.com/watch?v=$id")
    }

    suspend fun resolve(text: String, format: Format): FileSource = withContext(Dispatchers.IO) {
        val uri = input(text)
        if (uri.host !in youtubeHosts) {
            require(uri.path.endsWith("." + format.extension, true)) { "Choose the format that matches the link." }
            return@withContext FileSource(uri, uri.path.substringAfterLast('/').dropLast(4), false, format)
        }
        val extractor = engine.getStreamExtractor(uri.toString())
        extractor.fetchPage()
        val stream = when (format) {
            Format.VIDEO -> extractor.videoStreams.filter {
                it.isUrl && !it.isVideoOnly && it.format == MediaFormat.MPEG_4 &&
                    it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
            }.maxByOrNull { it.height }
            Format.AUDIO -> extractor.audioStreams.filter {
                it.isUrl && it.format == MediaFormat.M4A && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
            }.maxByOrNull { it.averageBitrate }
        } ?: throw IOException("This video does not offer a downloadable ${format.extension.uppercase()} stream.")
        val url = checkedUri(stream.content)
        require(isGoogleVideo(url.host)) { "The video service returned an unexpected file host." }
        FileSource(url, extractor.name, true, format)
    }

    fun filename(title: String, format: Format): String {
        val name = title.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().trim('.').take(90)
        return (name.ifBlank { "Javo media" }) + "." + format.extension
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun newDownload(context: Context, source: FileSource): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename(source.title, source.format))
            put(MediaStore.MediaColumns.MIME_TYPE, source.format.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create the file in Downloads.")
    }

    suspend fun save(context: Context, source: FileSource, chosen: Uri?, progress: (Long, Long?) -> Unit) =
        withContext(Dispatchers.IO) {
            val createdInDownloads = chosen == null
            val destination = chosen ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) newDownload(context, source)
                else throw IOException("Choose a file location.")
            val resolver = context.contentResolver
            var completed = false
            try {
                open(source.url, allowedHost = { !source.youtube || isGoogleVideo(it) }).use { connection ->
                    val reply = connection.value
                    if (reply.responseCode != 200) throw IOException("Video source returned HTTP ${reply.responseCode}.")
                    val size = reply.contentLengthLong.takeIf { it > 0 }
                    if (size != null && size > VIDEO_LIMIT) throw IOException("The video exceeds 2 GiB.")
                    DataInputStream(reply.inputStream.buffered()).use { input ->
                        val header = ByteArray(12).also(input::readFully)
                        if (!header.copyOfRange(4, 8).contentEquals("ftyp".toByteArray(Charsets.US_ASCII))) {
                            throw IOException("The response is not an MP4 file.")
                        }
                        checkNotNull(resolver.openOutputStream(destination, "w")).buffered().use { output ->
                            output.write(header)
                            var bytes = header.size.toLong()
                            val buffer = ByteArray(64 * 1024)
                            var lastUpdate = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                bytes += count
                                if (bytes > VIDEO_LIMIT) throw IOException("The video exceeds 2 GiB.")
                                output.write(buffer, 0, count)
                                if (bytes - lastUpdate >= 256 * 1024) {
                                    progress(bytes, size)
                                    lastUpdate = bytes
                                }
                            }
                            if (size != null && bytes != size) throw IOException("The video transfer ended early.")
                            progress(bytes, size)
                        }
                    }
                }
                validateTracks(context, destination, source.format)
                if (createdInDownloads && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val ready = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                    check(resolver.update(destination, ready, null, null) == 1) { "Could not publish the saved video." }
                }
                completed = true
                destination
            } finally {
                if (!completed) runCatching { resolver.delete(destination, null, null) }
            }
        }

    private fun validateTracks(context: Context, uri: Uri, format: Format) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val tracks = (0 until extractor.trackCount).mapNotNull {
                extractor.getTrackFormat(it).getString(AndroidMediaFormat.KEY_MIME)
            }
            val video = tracks.any { it.startsWith("video/") }
            val audio = tracks.any { it.startsWith("audio/") }
            if (!audio || video != (format == Format.VIDEO)) {
                throw IOException("The file does not contain the requested audio and video tracks.")
            }
        } finally {
            extractor.release()
        }
    }

    private fun checkedUri(address: String): URI {
        val uri = URI.create(address)
        require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null && uri.port in listOf(-1, 443)) { "Use an HTTPS video link." }
        return uri
    }

    private fun isGoogleVideo(host: String): Boolean =
        host.equals("googlevideo.com", true) || host.lowercase().endsWith(".googlevideo.com")

    private class Connection(val value: HttpsURLConnection) : AutoCloseable {
        override fun close() = value.disconnect()
    }

    private fun open(
        initial: URI,
        method: String = "GET",
        headers: Map<String, List<String>> = emptyMap(),
        body: ByteArray? = null,
        allowedHost: (String) -> Boolean,
    ): Connection {
        var uri = initial
        var requestMethod = method
        repeat(6) {
            require(allowedHost(uri.host.lowercase())) { "The video request left its trusted service." }
            val connection = uri.toURL().openConnection() as HttpsURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 12_000
                connection.readTimeout = 20_000
                connection.requestMethod = requestMethod
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept-Encoding", "identity")
                headers.forEach { (key, values) ->
                    if (key.lowercase() in setOf("accept", "accept-language", "content-type", "origin", "referer", "user-agent") ||
                        key.startsWith("X-YouTube-", ignoreCase = true) ||
                        key.equals("X-Goog-Visitor-Id", ignoreCase = true)) {
                        values.firstOrNull()?.let { connection.setRequestProperty(key, it) }
                    }
                }
                if (requestMethod == "POST" && body != null) {
                    connection.doOutput = true
                    connection.setFixedLengthStreamingMode(body.size)
                    connection.outputStream.use { it.write(body) }
                }
                val code = connection.responseCode
                if (code !in setOf(301, 302, 303, 307, 308)) return Connection(connection)
                val location = connection.getHeaderField("Location") ?: throw IOException("Invalid video redirect.")
                uri = checkedUri(uri.resolve(location).toString())
                if (code == 303 || (code in setOf(301, 302) && requestMethod == "POST")) requestMethod = "GET"
                connection.disconnect()
            } catch (error: Exception) {
                connection.disconnect()
                throw error
            }
        }
        throw IOException("Too many video redirects.")
    }

    private class YouTubeConnection : Downloader() {
        override fun execute(request: Request): Response {
            val uri = checkedUri(request.url())
            open(uri, request.httpMethod(), request.headers(), request.dataToSend(), extractorHosts::contains).use {
                val connection = it.value
                val code = connection.responseCode
                val response = (if (code < 400) connection.inputStream else connection.errorStream)
                    ?.use { stream ->
                        val result = ByteArrayOutputStream()
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = stream.read(buffer)
                            if (read < 0) break
                            if (result.size() + read > METADATA_LIMIT) throw IOException("Video metadata is too large.")
                            result.write(buffer, 0, read)
                        }
                        result.toString("UTF-8")
                    }.orEmpty()
                return Response(code, connection.responseMessage, connection.headerFields.filterKeys { it != null },
                    response, connection.url.toString())
            }
        }
    }
}
