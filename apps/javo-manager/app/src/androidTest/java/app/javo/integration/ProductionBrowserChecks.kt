@file:OptIn(app.revanced.manager.downloader.DownloaderHostApi::class)
package app.javo.integration

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.core.content.FileProvider
import app.revanced.manager.DownloaderActivity
import app.revanced.manager.downloader.webview.WebViewFragment
import app.revanced.manager.downloader.webview.IWebView
import app.revanced.manager.downloader.webview.IWebViewEvents
import app.revanced.manager.downloader.DownloadUrl
import app.revanced.manager.downloader.Downloader
import app.revanced.manager.downloader.Scope
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.network.downloader.ApkDownloadStage
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.R
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

internal class BrowserHost(private val test:Instrumentation) : AutoCloseable {
    val ready=CountDownLatch(1)
    val pages=LinkedBlockingQueue<String>()
    val downloads=LinkedBlockingQueue<Pair<String,String>>()
    lateinit var controller:IWebView
    val events=object:IWebViewEvents.Stub() {
        override fun ready(value:IWebView) { controller=value;ready.countDown() }
        override fun pageLoad(url:String) { pages.offer(url) }
        override fun download(url:String,mime:String,userAgent:String) { downloads.offer(url to userAgent) }
    }
    val activity:DownloaderActivity
    val fragment:WebViewFragment
    init {
        val parameters=Class.forName("app.revanced.manager.downloader.webview.WebViewFragment\$Parameters")
            .getDeclaredConstructor(String::class.java,IBinder::class.java).newInstance("APKMirror",events) as Parcelable
        val intent=Intent(test.targetContext,DownloaderActivity::class.java).setAction("app.javo.integration.BROWSER")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("FRAGMENT_CLASS_NAME",WebViewFragment::class.java.name)
            .putExtra("FRAGMENT_ARGS",Bundle().apply { putParcelable("params",parameters) })
        activity=test.startActivitySync(intent) as DownloaderActivity
        check(ready.await(20,TimeUnit.SECONDS)) { "Manager browser binder was not ready" }
        test.waitForIdleSync()
        fragment=activity.supportFragmentManager.fragments.first() as WebViewFragment
    }
    fun click(tag:String) { test.runOnMainSync { checkNotNull(activity.findViewById<View>(android.R.id.content).findViewWithTag<View>(tag)).also { check(it.isShown) { "Native browser control is hidden: $tag" } }.performClick() } }
    override fun close() { test.runOnMainSync { activity.finish() };test.waitForIdleSync() }
}

internal fun checkProductionBrowser(test:Instrumentation,bundle:File):JSONArray {
    val base="https://www.apkmirror.com/apk/google-inc/youtube/fixture-android-apk-download/"
    val intermediate=base+"download/?key=fixture"
    val final="https://www.apkmirror.com/wp-content/themes/APKMirror/download.php?id=fixture&key=fixture"
    val cdn="https://eb5e7388c3df147b74dd2379b7cf8323.r2.cloudflarestorage.com/downloadprod/fixture/youtube.apk?token=fixture"
    BrowserHost(test).use { host ->
        test.runOnMainSync {
            val browser=host.fragment.webView
            val delegate=browser.webViewClient
            browser.webViewClient=object:WebViewClient() {
                override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest)=delegate.shouldOverrideUrlLoading(view,request)
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse? {
                    val body=when(request.url.toString()) {
                        base -> "<html><head></head><body><a class='downloadButton' target='_blank' onclick='return false' href='$intermediate'>Download APK</a></body></html>"
                        intermediate -> "<html><head></head><body><a id='download-link' target='_blank' onclick='return false' href='$final'>here</a></body></html>"
                        final -> "<html><head><script>location.href='$cdn';</script></head><body>Resolving protected endpoint</body></html>"
                        cdn -> error("Native host incorrectly waited for a WebView binary response")
                        else -> return delegate.shouldInterceptRequest(view,request)
                    }
                    return WebResourceResponse("text/html","UTF-8",ByteArrayInputStream(body.toByteArray()))
                }
                override fun onPageFinished(view:WebView,url:String) { delegate.onPageFinished(view,url) }
            }
        }
        host.controller.load(base)
        check(host.pages.poll(25,TimeUnit.SECONDS)==base)
        test.waitForIdleSync()
        host.click("javo.download")
        val download=checkNotNull(host.downloads.poll(30,TimeUnit.SECONDS)) { "Actual Manager browser did not return its final download URL" }
        check(download.first==cdn && !download.second.contains("; wv"))
        check(host.downloads.poll(1,TimeUnit.SECONDS)==null) { "Duplicate native download" }
    }
    val target=test.targetContext
    val source=File(target.filesDir,"ui_ephemeral/browser-fixture.apkm").also { it.parentFile!!.mkdirs();bundle.copyTo(it,overwrite=true) }
    val uri=FileProvider.getUriForFile(target,target.packageName+".fileprovider",source)
    val monitor=test.addMonitor(IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE);addDataType("*/*") },Instrumentation.ActivityResult(Activity.RESULT_OK,Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)),true)
    try {
        BrowserHost(test).use { host ->
            test.runOnMainSync {
                val delegate=host.fragment.webView.webViewClient
                host.fragment.webView.webViewClient=object:WebViewClient() {
                    override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest)=WebResourceResponse("text/html","UTF-8",ByteArrayInputStream("<html><head></head><body>Pick the downloaded file</body></html>".toByteArray()))
                    override fun onPageFinished(view:WebView,url:String) { delegate.onPageFinished(view,url) }
                }
            }
            host.controller.load(base)
            check(host.pages.poll(25,TimeUnit.SECONDS)==base)
            test.waitForIdleSync()
            host.click("javo.import")
            val selected=checkNotNull(host.downloads.poll(30,TimeUnit.SECONDS)) { "Native file picker result did not return to download flow" }
            check(monitor.hits==1)
            val copied=File(URI(selected.first));check(copied.exists() && copied.length()==source.length())
            val scope=object:Scope {
                override val hostPackageName=target.packageName
                override val downloaderPackageName=target.packageName
                override val dataDir=target.cacheDir
            }
            val plugin=Downloader<Parcelable>(R.string.app_name) {
                get { _,_ -> null };download { _ -> error("Private import was sent to the network plugin") }
            }.build(scope,target,target.resources)
            val loaded=LoadedDownloader(target.packageName,"fixture.apkmirror.Import","APKMirror test",scope,plugin)
            val repository=GlobalContext.get().get<DownloadedAppRepository>()
            val stages=mutableListOf<ApkDownloadStage>()
            runBlocking {
                val output=repository.download(loaded,DownloadUrl(selected.first),"com.google.android.youtube","fixture-1",true,true,{}, { stages.add(it) })
                check(output.exists() && !copied.exists())
                java.util.zip.ZipFile(output).use { check(it.getEntry("assets/javo-transfer-stress.bin").size==32L*1024*1024) }
                check(stages==listOf(ApkDownloadStage.IMPORTING,ApkDownloadStage.MERGING,ApkDownloadStage.WRITING))
                try {
                    repository.download(loaded,DownloadUrl(source.toURI().toString()),"com.google.android.youtube","fixture-1",true,true,{})
                    error("File outside private import directory was accepted")
                } catch (expected:IllegalArgumentException) { check(source.exists()) }
            }
        }
    } finally { test.removeMonitor(monitor);source.delete() }
    return JSONArray().put("Production DownloaderActivity and WebViewFragment native button follow protected endpoint and capture resolved CDN URL without WebView DownloadListener")
        .put("Production document picker copies APKM privately, host validates/merges it, rejects outside paths, and removes temporary import")
}
