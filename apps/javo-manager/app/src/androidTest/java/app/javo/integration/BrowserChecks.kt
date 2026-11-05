package app.javo.integration

import android.app.Instrumentation
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import app.revanced.manager.downloader.webview.ApkMirrorPolicy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONArray

internal fun checkBrowser(instrumentation: Instrumentation): JSONArray {
    val done = CountDownLatch(1)
    val failure = AtomicReference<Throwable?>()
    var browser: WebView? = null
    val selected = AtomicReference<String?>()
    val navigated = CountDownLatch(1)
    val base = "https://www.apkmirror.com/apk/google-inc/youtube/youtube-20-40-45-release/"
    val variant = base + "youtube-20-40-45-2-android-apk-download/"
    instrumentation.runOnMainSync {
        browser = WebView(instrumentation.targetContext).apply {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    selected.set(request.url.toString())
                    navigated.countDown()
                    return true
                }
                override fun onPageFinished(view: WebView, url: String) {
                    view.evaluateJavascript(ApkMirrorPolicy.pageScript(true, false)) {
                        view.evaluateJavascript("getComputedStyle(document.getElementById('ad')).display==='none'") { value ->
                            try { check(value == "true") { "Ad container remained visible" } }
                            catch (error: Throwable) { failure.set(error) }
                            done.countDown()
                        }
                    }
                }
            }
            loadDataWithBaseURL(base, """
                <html><head></head><body>
                <div id="ad" class="ad-container">Ad fixture</div>
                <div class="table-row"><a href="${base}bundle-android-apk-download/">20.40.45</a> BUNDLE</div>
                <div class="table-row"><a href="$variant">20.40.45</a> APK</div>
                </body></html>
            """.trimIndent(), "text/html", "UTF-8", null)
        }
    }
    try {
        check(done.await(30, TimeUnit.SECONDS)) { "Browser fixture timed out" }
        failure.get()?.let { throw AssertionError("Browser fixture failed", it) }
        check(navigated.await(10, TimeUnit.SECONDS)) { "No variant navigation occurred" }
        check(selected.get() == variant) { "Standalone variant was not selected: ${selected.get()}" }
        return JSONArray().put("Real WebView hides ad containers and selects the unique standalone APK variant")
    } finally {
        instrumentation.runOnMainSync { browser?.destroy() }
    }
}
