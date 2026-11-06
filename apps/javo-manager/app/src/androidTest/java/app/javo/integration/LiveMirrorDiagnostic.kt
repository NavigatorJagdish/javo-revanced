@file:OptIn(app.revanced.manager.downloader.DownloaderHostApi::class)
package app.javo.integration

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.TextView
import app.javo.security.UpdateSecurity
import app.revanced.manager.downloader.DownloadConnection
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Opt-in network diagnosis; its outcome is not a fixture test pass. No challenge is automated. */
internal fun diagnoseLiveMirror(test:Instrumentation) {
    val result=JSONObject().put("liveDownloadVerified",false)
    val file=File(test.targetContext.filesDir,"mirror-live-diagnostic.json")
    try {
        BrowserHost(test).use { host ->
            test.runOnMainSync {
                val delegate=host.fragment.webView.webViewClient
                host.fragment.webView.webViewClient=object:android.webkit.WebViewClient() {
                    override fun shouldOverrideUrlLoading(view:WebView,request:android.webkit.WebResourceRequest):Boolean {
                        val blocked=delegate.shouldOverrideUrlLoading(view,request)
                        if(blocked && !app.revanced.manager.downloader.webview.ApkMirrorPolicy.isDownloadFile(request.url.toString())) result.put("blockedNavigation",request.url.buildUpon().clearQuery().fragment(null).build().toString())
                        return blocked
                    }
                    override fun shouldInterceptRequest(view:WebView,request:android.webkit.WebResourceRequest)=delegate.shouldInterceptRequest(view,request)
                    override fun onPageFinished(view:WebView,url:String) { delegate.onPageFinished(view,url) }
                    override fun onReceivedHttpError(view:WebView,request:android.webkit.WebResourceRequest,response:android.webkit.WebResourceResponse) {
                        if(request.isForMainFrame) result.put("pageHttpStatus",response.statusCode)
                        delegate.onReceivedHttpError(view,request,response)
                    }
                }
            }
            host.controller.load("https://www.apkmirror.com/apk/google-inc/youtube/youtube-20-40-45-release/youtube-20-40-45-2-android-apk-download/")
            val page=host.pages.poll(40,TimeUnit.SECONDS)
            result.put("pageFinished",page!=null)
            val dom=CountDownLatch(1)
            test.runOnMainSync {
                result.put("webViewVersion",WebView.getCurrentWebViewPackage()?.versionName)
                result.put("userAgent",host.fragment.webView.settings.userAgentString)
                host.fragment.webView.evaluateJavascript("JSON.stringify({title:document.title,links:document.querySelectorAll('a[href]').length,hasFinal:!!document.getElementById('download-link'),hasButton:!!document.querySelector('.downloadButton')})") { raw ->
                    result.put("page",raw);dom.countDown()
                }
            }
            dom.await(5,TimeUnit.SECONDS)
            host.click("javo.download")
            val download=host.downloads.poll(45,TimeUnit.SECONDS)
            if(download!=null) {
                result.put("nativeUrlReturned",true)
                val headers=mutableMapOf("User-Agent" to download.second)
                test.runOnMainSync {
                    CookieManager.getInstance().getCookie(download.first)?.takeIf { it.isNotBlank() }?.let { headers["Cookie"]=it }
                    (host.pages.lastOrNull() ?: page)?.let { headers["Referer"]=it }
                }
                val output=File(test.targetContext.cacheDir,"live-source.apk")
                val connection=DownloadConnection.connect(download.first,headers)
                result.put("httpStatus",connection.responseCode)
                DownloadConnection.stream(connection).use { input -> output.outputStream().use { input.copyTo(it) } }
                @Suppress("DEPRECATION")
                val info=test.targetContext.packageManager.getPackageArchiveInfo(output.path,0)
                check(info?.packageName=="com.google.android.youtube" && info.versionName=="20.40.45")
                result.put("bytes",output.length()).put("sha256",UpdateSecurity.sha256(output)).put("liveDownloadVerified",true)
                output.delete()
            } else {
                result.put("nativeUrlReturned",false)
                test.runOnMainSync {
                    result.put("visibleStatus",host.activity.findViewById<TextView>(app.revanced.manager.downloader.R.id.javo_web_status).text.toString())
                }
            }
        }
    } catch(error:Throwable) { result.put("failure",error.message?:error.javaClass.simpleName) }
    result.put("diagnosticCompleted",true)
    file.writeText(result.toString(2))
    test.finish(Activity.RESULT_OK,Bundle().apply { putString("diagnostic",result.toString()) })
}
