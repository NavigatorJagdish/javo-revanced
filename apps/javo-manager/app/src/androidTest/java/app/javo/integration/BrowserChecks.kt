package app.javo.integration

import android.app.Instrumentation
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import app.revanced.manager.downloader.webview.ApkMirrorPolicy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
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
        checkFinalDownload(instrumentation)
        return JSONArray().put("Real WebView hides ad containers and selects the unique standalone APK variant")
            .put("Final download.php link with target=_blank reaches the native WebView download listener")
    } finally {
        instrumentation.runOnMainSync { browser?.destroy() }
    }
}

/** Reproduces the site's second page, which does not use the first page's /download/? URL. */
private fun checkFinalDownload(instrumentation: Instrumentation) {
    val downloaded=CountDownLatch(1)
    val received=AtomicReference<String?>()
    val activated=AtomicBoolean(false)
    val base="https://www.apkmirror.com/apk/google-inc/youtube/youtube-20-40-45-release/variant/download/?key=fixture"
    val file="https://www.apkmirror.com/wp-content/themes/APKMirror/download.php?id=fixture&key=fixture"
    var browser:WebView?=null
    instrumentation.runOnMainSync {
        browser=WebView(instrumentation.targetContext).apply {
            settings.javaScriptEnabled=true
            settings.setSupportMultipleWindows(true)
            webChromeClient=android.webkit.WebChromeClient()
            setDownloadListener { url,_,_,_,_ -> received.set(url);downloaded.countDown() }
            webViewClient=object:WebViewClient() {
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse? {
                    if(request.url.toString()==file) return WebResourceResponse(
                        "application/vnd.android.package-archive",null,200,"OK",
                        mapOf("Content-Disposition" to "attachment; filename=fixture.apk"),ByteArrayInputStream(ByteArray(128)))
                    return null
                }
                override fun onPageFinished(view:WebView,url:String) {
                    if(activated.compareAndSet(false,true)) view.evaluateJavascript(ApkMirrorPolicy.pageScript(true,true),null)
                }
            }
            loadDataWithBaseURL(base,"""<html><head></head><body>
                <a id="download-link" target="_blank" href="$file">here</a>
                </body></html>""","text/html","UTF-8",null)
        }
    }
    try {
        check(downloaded.await(30,TimeUnit.SECONDS)) { "Final APKMirror download link did not reach the download listener" }
        check(received.get()==file) { "Unexpected final download URL" }
    } finally { instrumentation.runOnMainSync { browser?.destroy() } }
}

