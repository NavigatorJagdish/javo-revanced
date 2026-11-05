package app.javo.integration

import android.content.Context
import app.revanced.manager.R
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.downloader.*
import app.revanced.manager.network.downloader.ApkDownloadStage
import app.revanced.manager.network.downloader.LoadedDownloader
import java.io.File
import java.net.InetAddress
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.*
import org.json.JSONArray
import org.koin.core.context.GlobalContext

@OptIn(DownloaderHostApi::class)
internal suspend fun checkHttpsDownload(context: Context, bundle: File, target: File, folder: File, certificate: ByteArray): JSONArray {
    val keys = KeyStore.getInstance("PKCS12").apply {
        certificate.inputStream().use { load(it, "fixture-test".toCharArray()) }
    }
    val km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
        init(keys, "fixture-test".toCharArray())
    }
    val tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(keys) }
    val tls = SSLContext.getInstance("TLS").apply { init(km.keyManagers, tm.trustManagers, null) }
    val server = tls.serverSocketFactory.createServerSocket(0, 4, InetAddress.getByName("127.0.0.1")) as SSLServerSocket
    val failure = AtomicReference<Throwable?>()
    val worker = Thread {
        try {
            repeat(2) { index ->
                server.accept().use { client ->
                    client.soTimeout = 30_000
                    val reader = client.getInputStream().bufferedReader()
                    val path = checkNotNull(reader.readLine()).split(' ')[1]
                    val headers = mutableListOf<String>()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        headers.add(line)
                    }
                    check(headers.any { it.equals("Cookie: session=fixture", true) })
                    val out = client.getOutputStream()
                    if (index == 0) {
                        check(path == "/redirect")
                        out.write("HTTP/1.1 302 Found\r\nLocation: /bundle\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                    } else {
                        check(path == "/bundle")
                        // No Content-Length exercises EOF completion over an actual HTTPS connection.
                        out.write("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nConnection: close\r\n\r\n".toByteArray())
                        bundle.inputStream().use { it.copyTo(out) }
                    }
                    out.flush()
                }
            }
        } catch (error: Throwable) { failure.set(error) }
    }.apply { isDaemon = true; start() }
    val original = HttpsURLConnection.getDefaultSSLSocketFactory()
    try {
        HttpsURLConnection.setDefaultSSLSocketFactory(tls.socketFactory)
        val scope = object : Scope {
            override val hostPackageName = context.packageName
            override val downloaderPackageName = context.packageName
            override val dataDir = folder
        }
        val plugin = Downloader<android.os.Parcelable>(R.string.app_name) {
            get { _, _ -> null }
            download { _, _ -> error("Host APKMirror path was bypassed") }
        }.build(scope, context, context.resources)
        val loaded = LoadedDownloader(context.packageName, "fixture.apkmirror.Downloader", "APKMirror test", scope, plugin)
        val stages = mutableListOf<ApkDownloadStage>()
        var progress: Pair<Long, Long?>? = null
        val file = GlobalContext.get().get<DownloadedAppRepository>().download(
            loaded, DownloadUrl("https://localhost:${server.localPort}/redirect", mapOf("Cookie" to "session=fixture")),
            "com.google.android.youtube", "fixture-1", true, true,
            onDownload = { progress = it }, onStage = { stages.add(it) }
        )
        check(stages == listOf(ApkDownloadStage.DOWNLOADING, ApkDownloadStage.MERGING, ApkDownloadStage.WRITING))
        check(progress?.first == bundle.length() && progress?.second == null)
        java.util.zip.ZipFile(file).use { check(it.getEntry("assets/javo-transfer-stress.bin").size == 32L * 1024 * 1024) }
        file.copyTo(target, overwrite = true)
        worker.join(10_000)
        check(!worker.isAlive)
        failure.get()?.let { throw AssertionError("HTTPS fixture server failed", it) }
        return JSONArray().put("HTTPS redirect, session cookie, missing length, 32 MiB APKM transfer and merge completed through repository")
    } finally {
        HttpsURLConnection.setDefaultSSLSocketFactory(original)
        server.close()
    }
}
