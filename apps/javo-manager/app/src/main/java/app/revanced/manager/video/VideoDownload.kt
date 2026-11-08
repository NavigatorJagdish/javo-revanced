package app.revanced.manager.video

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat as AndroidMediaFormat
import android.media.MediaMuxer
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
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.nio.ByteBuffer
import javax.net.ssl.HttpsURLConnection

internal object VideoDownload {
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    private const val VIDEO_LIMIT = 2L * 1024 * 1024 * 1024
    private const val METADATA_LIMIT = 8 * 1024 * 1024
    private val videoId = Regex("[A-Za-z0-9_-]{11}")
    private val youtubeHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be")
    private val extractorHosts = youtubeHosts + setOf("youtubei.googleapis.com", "www.youtube-nocookie.com", "s.ytimg.com")
    private val engine by lazy { NewPipe.init(YouTubeConnection()); ServiceList.YouTube }

    internal fun youtubeExtractor(uri: URI) = engine.getStreamExtractor(uri.toString())

    enum class Format(val extension: String, val mimeType: String) {
        VIDEO("mp4", "video/mp4"),
        AUDIO("m4a", "audio/mp4"),
    }

    data class FileSource(
        val url: URI, val title: String, val youtube: Boolean, val format: Format,
        val companionAudio: URI? = null,
    )

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
        val extractor = youtubeExtractor(uri)
        extractor.fetchPage()
        fun audio() = extractor.audioStreams.filter {
            it.isUrl && it.format == MediaFormat.M4A && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                it.codec.orEmpty().startsWith("mp4a")
        }.maxByOrNull { it.averageBitrate }
        fun mediaUrl(address: String): URI = checkedUri(address).also {
            require(isGoogleVideo(it.host)) { "The video service returned an unexpected file host." }
        }
        val combined = when (format) {
            Format.VIDEO -> extractor.videoStreams.filter {
                it.isUrl && !it.isVideoOnly && it.format == MediaFormat.MPEG_4 &&
                    it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
            }.maxByOrNull { it.height }
            Format.AUDIO -> audio()
        }
        if (combined != null) return@withContext FileSource(mediaUrl(combined.content), extractor.name, true, format)
        if (format == Format.VIDEO) {
            val video = extractor.videoOnlyStreams.filter {
                it.isUrl && it.format == MediaFormat.MPEG_4 && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                    it.codec.orEmpty().startsWith("avc1") && it.height in 1..1080
            }.maxByOrNull { it.height }
            val sound = if (video != null) audio() else null
            if (video != null && sound != null) {
                return@withContext FileSource(mediaUrl(video.content), extractor.name, true, format, mediaUrl(sound.content))
            }
        }
        throw IOException("This video does not offer a downloadable ${format.extension.uppercase()} stream.")
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
                checkNotNull(resolver.openOutputStream(destination, "w")).buffered().use { output ->
                    if (source.companionAudio != null) transferSplit(context, source, output, progress)
                    else transfer(source, output, progress)
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

    private suspend fun transferSplit(
        context: Context, source: FileSource, output: OutputStream, progress: (Long, Long?) -> Unit,
    ) {
        val scratch = java.nio.file.Files.createTempDirectory(context.cacheDir.toPath(), "javo-media-").toFile()
        val video = File(scratch, "video.mp4")
        val audio = File(scratch, "audio.m4a")
        val combined = File(scratch, "combined.mp4")
        try {
            video.outputStream().buffered().use { transfer(source, it, { bytes, _ -> progress(bytes, null) }) }
            val videoSize = video.length()
            val audioSource = source.copy(url = checkNotNull(source.companionAudio), format = Format.AUDIO, companionAudio = null)
            audio.outputStream().buffered().use {
                transfer(audioSource, it, { bytes, total -> progress(videoSize + bytes, total?.plus(videoSize)) },
                    limit = VIDEO_LIMIT - videoSize)
            }
            muxMp4(video, audio, combined)
            check(combined.length() in 13..VIDEO_LIMIT) { "The combined video exceeds the file limit." }
            combined.inputStream().buffered().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
            }
            progress(combined.length(), combined.length())
        } finally {
            combined.delete()
            audio.delete()
            video.delete()
            scratch.delete()
        }
    }

    private suspend fun muxMp4(video: File, audio: File, output: File) {
        val readers = listOf(MediaExtractor(), MediaExtractor())
        try {
            val muxer = MediaMuxer(output.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            try {
                val targetTracks = listOf(video to "video/avc", audio to "audio/mp4a-latm").mapIndexed { i, (file, mime) ->
                    val reader = readers[i]
                    reader.setDataSource(file.path)
                    val track = (0 until reader.trackCount).firstOrNull {
                        reader.getTrackFormat(it).getString(AndroidMediaFormat.KEY_MIME) == mime
                    } ?: throw IOException("A compatible media track is missing.")
                    reader.selectTrack(track)
                    muxer.addTrack(reader.getTrackFormat(track))
                }
                val data = ByteBuffer.allocateDirect(8 * 1024 * 1024)
                val info = MediaCodec.BufferInfo()
                muxer.start()
                val samples = intArrayOf(0, 0)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val videoTime = readers[0].sampleTime
                    val audioTime = readers[1].sampleTime
                    if (videoTime < 0 && audioTime < 0) break
                    val i = if (videoTime >= 0 && (audioTime < 0 || videoTime <= audioTime)) 0 else 1
                    val reader = readers[i]
                    val flags = reader.sampleFlags
                    check(flags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED == 0) { "Protected media cannot be saved." }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        check(reader.sampleSize in 1..data.capacity().toLong()) { "A media sample exceeds the size limit." }
                    }
                    data.clear()
                    val count = reader.readSampleData(data, 0)
                    check(count in 1..data.capacity()) { "Could not read a media sample." }
                    val keyFrame = if (flags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    info.set(0, count, reader.sampleTime, keyFrame)
                    muxer.writeSampleData(targetTracks[i], data, info)
                    samples[i]++
                    reader.advance()
                }
                check(samples.all { it > 0 }) { "The video or audio track is empty." }
                muxer.stop()
            } finally {
                muxer.release()
            }
        } finally {
            readers.forEach { it.release() }
        }
    }

    private suspend fun transfer(
        source: FileSource, output: OutputStream, progress: (Long, Long?) -> Unit, limit: Long = VIDEO_LIMIT,
    ) {
        var bytes = 0L
        var size: Long? = null
        var tag: String? = null
        var retries = 0
        var lastUpdate = 0L
        val buffer = ByteArray(64 * 1024)
        fun write(data: ByteArray, count: Int) {
            try {
                output.write(data, 0, count)
            } catch (error: IOException) {
                // A file-write failure can leave an unknown offset; retry network reads only.
                throw IllegalStateException("Could not write the destination file.", error)
            }
        }
        while (true) {
            currentCoroutineContext().ensureActive()
            val offset = bytes
            val headers = if (offset == 0L) emptyMap() else buildMap {
                put("Range", listOf("bytes=$offset-"))
                tag?.let { put("If-Range", listOf(it)) }
            }
            try {
                open(source.url, headers = headers, allowedHost = { !source.youtube || isGoogleVideo(it) }).use { conn ->
                    val reply = conn.value
                    if (offset == 0L) {
                        check(reply.responseCode == 200) { "Media source returned HTTP ${reply.responseCode}." }
                        size = reply.contentLengthLong.takeIf { it > 0 }
                        check((size ?: 0) <= limit) { "The media exceeds the file size limit." }
                        tag = reply.getHeaderField("ETag")?.takeIf { it.startsWith("\"") && it.endsWith("\"") }
                    } else {
                        val total = checkNotNull(size)
                        check(reply.responseCode == 206 &&
                            reply.getHeaderField("Content-Range") == "bytes $offset-${total - 1}/$total" &&
                            (tag == null || tag == reply.getHeaderField("ETag"))) { "Media source could not resume safely." }
                    }
                    DataInputStream(reply.inputStream.buffered()).use { input ->
                        if (offset == 0L) {
                            val header = ByteArray(12).also(input::readFully)
                            check(header.copyOfRange(4, 8).contentEquals("ftyp".toByteArray(Charsets.US_ASCII))) {
                                "The response is not MP4 media."
                            }
                            write(header, header.size)
                            bytes += header.size
                        }
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            check(bytes + count <= limit && (size == null || bytes + count <= size!!)) {
                                "The media exceeded its declared size."
                            }
                            write(buffer, count)
                            bytes += count
                            if (bytes - lastUpdate >= 256 * 1024) {
                                progress(bytes, size)
                                lastUpdate = bytes
                            }
                        }
                        if (size != null && bytes != size) throw IOException("The media transfer ended early.")
                    }
                }
                progress(bytes, size)
                return
            } catch (error: IOException) {
                val canResume = size?.let { bytes in 1L until it } == true && (source.youtube || tag != null)
                if (++retries > 2 || (bytes != 0L && !canResume)) throw error
            }
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
                    if (key.lowercase() in setOf("accept", "accept-language", "content-type", "origin", "referer", "user-agent", "range", "if-range") ||
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
