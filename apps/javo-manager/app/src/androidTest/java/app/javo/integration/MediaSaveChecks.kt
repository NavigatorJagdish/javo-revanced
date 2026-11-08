package app.javo.integration

import android.app.Activity
import android.app.Instrumentation
import android.content.ContentUris
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import app.revanced.manager.video.VideoDownload
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.InetAddress
import java.net.URI
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

internal fun checkLiveMedia(test: Instrumentation, link: String) {
    val report = JSONObject().put("success", false).put("managerApkSha256",
        app.javo.security.UpdateSecurity.sha256(java.io.File(test.targetContext.applicationInfo.sourceDir)))
    val ok = runCatching {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        runBlocking {
            for (format in VideoDownload.Format.entries) {
                withTimeout(180_000) {
                    val source = VideoDownload.resolve(link, format)
                    var size = 0L
                    val uri = VideoDownload.save(test.targetContext, source, null) { bytes, _ -> size = bytes }
                    try {
                        check(size > 12)
                        report.put(format.extension, size)
                    } finally { test.targetContext.contentResolver.delete(uri, null, null) }
                }
            }
        }
    }
    report.put("success", ok.isSuccess)
    ok.exceptionOrNull()?.let {
        // Diagnostic output omits request and signed media URLs.
        report.put("failureType", it.javaClass.simpleName)
        report.put("failure", it.message.orEmpty().replace(Regex("https?://\\S+"), "[URL]").take(300))
    }
    test.finish(if (ok.isSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED,
        Bundle().apply { putString("result", report.toString()) })
}

@RequiresApi(Build.VERSION_CODES.Q)
internal suspend fun checkMediaSave(test: Instrumentation): JSONObject {
    val context = test.targetContext
    val report = JSONObject()
    val formats = VideoDownload.Format.entries
    val fixtures = formats.associateWith { format ->
        test.context.assets.open("javo-fixture.${format.extension}").use { it.readBytes() }
    }
    val videoOnly = test.context.assets.open("javo-fixture-video-only.mp4").use { it.readBytes() }
    val keys = KeyStore.getInstance("PKCS12").apply {
        test.context.assets.open("transfer-test.p12").use { load(it, "fixture-test".toCharArray()) }
    }
    val km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
        init(keys, "fixture-test".toCharArray())
    }
    val tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(keys) }
    val tls = SSLContext.getInstance("TLS").apply { init(km.keyManagers, tm.trustManagers, null) }
    val server = tls.serverSocketFactory.createServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
    val failure = AtomicReference<Throwable?>()
    val resumed = java.util.concurrent.atomic.AtomicBoolean(false)
    val worker = Thread {
        try {
            repeat(6) {
                server.accept().use { socket ->
                    socket.soTimeout = 20_000
                    val reader = socket.getInputStream().bufferedReader()
                    val path = checkNotNull(reader.readLine()).split(' ')[1]
                    val headers = buildList {
                        while (true) {
                            val line = reader.readLine()
                            if (line.isNullOrEmpty()) break
                            add(line)
                        }
                    }
                    val data = when (path) {
                        "/video.mp4" -> fixtures.getValue(VideoDownload.Format.VIDEO)
                        "/audio.m4a" -> fixtures.getValue(VideoDownload.Format.AUDIO)
                        "/split-video.mp4" -> videoOnly
                        "/split-audio.m4a" -> fixtures.getValue(VideoDownload.Format.AUDIO)
                        "/invalid.mp4" -> fixtures.getValue(VideoDownload.Format.AUDIO)
                        else -> error("Unexpected test request")
                    }
                    val output = socket.getOutputStream()
                    if (path == "/audio.m4a") {
                        val offset = data.size / 2
                        val etag = "\"javo-fixture-v1\""
                        if (headers.any { it.equals("Range: bytes=$offset-", ignoreCase = true) }) {
                            check(headers.any { it.equals("If-Range: $etag", ignoreCase = true) })
                            output.write(("HTTP/1.1 206 Partial Content\r\nETag: $etag\r\nContent-Length: ${data.size - offset}\r\n" +
                                "Content-Range: bytes $offset-${data.lastIndex}/${data.size}\r\nConnection: close\r\n\r\n").toByteArray())
                            output.write(data, offset, data.size - offset)
                            resumed.set(true)
                        } else {
                            output.write(("HTTP/1.1 200 OK\r\nETag: $etag\r\nContent-Length: ${data.size}\r\n" +
                                "Connection: close\r\n\r\n").toByteArray())
                            output.write(data, 0, offset)
                        }
                    } else {
                        output.write("HTTP/1.1 200 OK\r\nContent-Length: ${data.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        output.write(data)
                    }
                    output.flush()
                }
            }
        } catch (error: Throwable) { failure.set(error) }
    }.apply { isDaemon = true; start() }
    val original = HttpsURLConnection.getDefaultSSLSocketFactory()
    try {
        HttpsURLConnection.setDefaultSSLSocketFactory(tls.socketFactory)
        check(VideoDownload.input("Video https://youtu.be/aqz-KE-bpKQ?si=fixture").query == "v=aqz-KE-bpKQ")
        // Exercise the extractor's real URL parser without calling a third-party service.
        check(VideoDownload.youtubeExtractor(VideoDownload.input("https://youtu.be/aqz-KE-bpKQ")).id == "aqz-KE-bpKQ")
        check(runCatching { VideoDownload.input("https://youtube.com/playlist?list=fixture") }.isFailure)
        check(runCatching { VideoDownload.input("http://example.org/file.mp4") }.isFailure)
        check(runCatching { VideoDownload.resolve("https://example.org/file.mp4", VideoDownload.Format.AUDIO) }.isFailure)
        for (format in formats) {
            val expected = fixtures.getValue(format)
            val path = if (format == VideoDownload.Format.VIDEO) "video.mp4" else "audio.m4a"
            val source = VideoDownload.FileSource(URI("https://localhost:${server.localPort}/$path"), "Javo test", false, format)
            var progress: Pair<Long, Long?>? = null
            val uri = VideoDownload.save(context, source, null) { bytes, total -> progress = bytes to total }
            try {
                check(progress == (expected.size.toLong() to expected.size.toLong()))
                check(context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }.contentEquals(expected))
                context.contentResolver.query(uri, arrayOf("relative_path", "is_pending", "_display_name"), null, null, null)!!.use {
                    check(it.moveToFirst() && it.getString(0) == "Download/" && it.getInt(1) == 0 &&
                        it.getString(2).endsWith("." + format.extension))
                }
                report.put(format.extension, true)
            } finally { context.contentResolver.delete(uri, null, null) }
        }
        val split = VideoDownload.FileSource(URI("https://localhost:${server.localPort}/split-video.mp4"),
            "Javo separate tracks fixture", false, VideoDownload.Format.VIDEO,
            URI("https://localhost:${server.localPort}/split-audio.m4a"))
        val joined = VideoDownload.save(context, split, null) { _, _ -> }
        try {
            check(context.contentResolver.openInputStream(joined)!!.use { it.readBytes().size } > videoOnly.size)
        } finally { context.contentResolver.delete(joined, null, null) }
        val invalid = VideoDownload.FileSource(URI("https://localhost:${server.localPort}/invalid.mp4"),
            "Javo invalid track fixture", false, VideoDownload.Format.VIDEO)
        check(runCatching { VideoDownload.save(context, invalid, null) { _, _ -> } }.isFailure)
        context.contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, arrayOf("_id"),
            "_display_name=?", arrayOf(VideoDownload.filename(invalid.title, invalid.format)), null)!!.use {
            if (it.moveToFirst()) {
                context.contentResolver.delete(ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, it.getLong(0)), null, null)
                error("Invalid media was left in Downloads")
            }
        }
        worker.join(5_000)
        check(!worker.isAlive)
        failure.get()?.let { throw AssertionError("Media fixture server failed", it) }
        check(resumed.get())
        return report.put("invalidFileRemoved", true).put("interruptedAudioResumed", true)
            .put("separateTracksMuxed", true)
            .put("source", "first-party HTTPS fixtures")
    } finally {
        HttpsURLConnection.setDefaultSSLSocketFactory(original)
        server.close()
    }
}
