package app.revanced.manager.downloader.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.widget.Toast
import android.widget.TextView
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.downloader.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@OptIn(DownloaderHostApi::class)
@DownloaderHostApi
class WebViewFragment : Fragment(R.layout.webview_fragment) {
    private val vm by viewModels<WebViewModel>()
    lateinit var webView: WebView
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importPickedFile(uri)
    }
    private val args by lazy {
        arguments?.getParcelable<Parameters>(KEY)!!
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().apply {
            enableEdgeToEdge()
            onBackPressedDispatcher.addCallback {
                if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
                else cancelActivity()
            }
            actionBar?.apply {
                title = args.title
                setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)
                setDisplayHomeAsUpEnabled(true)
            }

            addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(
                        menu: Menu,
                        menuInflater: MenuInflater
                    ) {
                        if (vm.mirrorMode) {
                            menu.add(0, 1, 0, "Download APK").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                            menu.add(0, 2, 1, "Ad protection").apply { isCheckable = true; isChecked = vm.blockAds }
                            menu.add(0, 3, 2, "Search APKMirror")
                            menu.add(0, 4, 3, "Open in browser")
                            menu.add(0, 5, 4, "Use downloaded APK")
                        }
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem) =
                        if (menuItem.itemId == android.R.id.home) {
                            cancelActivity()

                            true
                        } else when (menuItem.itemId) {
                            1 -> { requestDownload(); true }
                            2 -> { vm.blockAds = !vm.blockAds; menuItem.isChecked = vm.blockAds; webView.reload(); true }
                            3 -> { webView.loadUrl(vm.originalUrl); true }
                            4 -> { openBrowser(); true }
                            5 -> { chooseFile(); true }
                            else -> false
                        }
                },
                this
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        webView = view.findViewById<WebView>(R.id.webview)
        view.findViewById<View>(R.id.javo_web_download).setOnClickListener { requestDownload() }
        view.findViewById<View>(R.id.javo_web_browser).setOnClickListener { openBrowser() }
        view.findViewById<View>(R.id.javo_web_import).setOnClickListener { chooseFile() }
        vm.onStatus = { message -> view.post { status(message) } }
        vm.onPageReady = { page, address ->
            if (vm.mirrorMode && ApkMirrorPolicy.isMirror(address)) {
                view.findViewById<ProgressBar>(R.id.javo_web_progress).visibility = View.GONE
                if (vm.downloadRequested) requestDownload()
                else {
                    page.evaluateJavascript(ApkMirrorPolicy.pageScript(vm.blockAds, false), null)
                    status("Choose a version, then Download APK. If blocked, Open browser and Use downloaded APK.")
                }
            }
        }

        val events = IWebViewEvents.Stub.asInterface(args.events)!!
        vm.setup(events)

        webView.apply {
            settings.apply {
                cacheMode = WebSettings.LOAD_NO_CACHE
                allowContentAccess = false
                allowFileAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(true)
                domStorageEnabled = true
                javaScriptEnabled = true
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(page: WebView, value: Int) {
                    view.findViewById<ProgressBar>(R.id.javo_web_progress).apply {
                        progress = value; visibility = if (value < 100) View.VISIBLE else View.GONE
                    }
                }
            }
            webViewClient = vm.webViewClient
            setDownloadListener { url, userAgent, _, mimetype, _ ->
                vm.onDownload(url, mimetype.orEmpty(), userAgent ?: settings.userAgentString)
            }
        }

        if (savedInstanceState != null) {
            if (vm.mirrorMode) webView.settings.userAgentString = ApkMirrorPolicy.browserUserAgent(webView.settings.userAgentString)
            vm.browserAgent = webView.settings.userAgentString
            webView.restoreState(savedInstanceState)
            view.findViewById<View>(R.id.javo_web_controls).visibility = if (vm.mirrorMode) View.VISIBLE else View.GONE
        }
        val activity = requireActivity()
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.commands.collect {
                    when (it) {
                        is WebViewModel.Command.Finish -> {
                            activity.setResult(Activity.RESULT_OK)
                            activity.finish()
                        }

                        is WebViewModel.Command.Load -> {
                            vm.originalUrl = it.url
                            vm.mirrorMode = ApkMirrorPolicy.isMirror(it.url)
                            view.findViewById<View>(R.id.javo_web_controls).visibility = if (vm.mirrorMode) View.VISIBLE else View.GONE
                            if (vm.mirrorMode) webView.settings.userAgentString = ApkMirrorPolicy.browserUserAgent(webView.settings.userAgentString)
                            vm.browserAgent = webView.settings.userAgentString
                            activity.invalidateOptionsMenu()
                            webView.loadUrl(ApkMirrorPolicy.startPage(it.url))
                        }
                    }
                }
            }
        }
    }

    private fun status(message: String) {
        view?.findViewById<TextView>(R.id.javo_web_status)?.text = message
    }

    private fun requestDownload() {
        if (!::webView.isInitialized || !vm.mirrorMode) return
        vm.downloadRequested = true
        status("Finding download link…")
        webView.evaluateJavascript(ApkMirrorPolicy.downloadActionScript(vm.blockAds)) { raw ->
            if (!isAdded) return@evaluateJavascript
            runCatching {
                val decoded = JSONTokener(raw).nextValue()
                val action = JSONObject(decoded as? String ?: "{}")
                when (action.optString("kind")) {
                    "download" -> {
                        val url = action.getString("url")
                        if (ApkMirrorPolicy.isMirror(url)) {
                            status("Resolving the APK download address…")
                            // Protected download.php must be visited by the browser before host transfer.
                            webView.loadUrl(url)
                        }
                    }
                    "navigate" -> {
                        val url = action.getString("url")
                        if (ApkMirrorPolicy.isMirror(url)) { status("Opening download page…"); webView.loadUrl(url) }
                    }
                    "waiting" -> status("Wait for the page countdown, then tap Download APK again.")
                    else -> {
                        vm.downloadRequested = false
                        status("No ready link. Complete the site check, or Open browser then Use downloaded APK.")
                    }
                }
            }.onFailure {
                vm.downloadRequested = false
                status("Could not read this page. Open browser, download there, then Use downloaded APK.")
            }
        }
    }

    private fun openBrowser() {
        val address = webView.url?.takeIf(ApkMirrorPolicy::isMirror) ?: vm.originalUrl
        if (!ApkMirrorPolicy.isMirror(address)) return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(address))) }
            .onSuccess { status("Download in your browser, return here, then tap Use downloaded APK.") }
            .onFailure { status("No browser available. Use downloaded APK to select an existing file.") }
    }

    private fun chooseFile() {
        vm.downloadRequested = false
        picker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream", "application/zip", "application/x-apkm"))
    }

    /** The URI comes only from the native document picker; page scripts cannot call this. */
    private fun importPickedFile(uri: Uri) {
        val context = requireContext().applicationContext
        status("Copying selected APK…")
        viewLifecycleOwner.lifecycleScope.launch {
            var temporary: File? = null
            var delivered = false
            try {
                val file = withContext(Dispatchers.IO) {
                    val directory = File(context.cacheDir, "javo-web-imports").also { it.mkdirs() }
                    File.createTempFile("selected-", ".zip", directory).also { output ->
                        temporary = output
                        checkNotNull(context.contentResolver.openInputStream(uri)) { "Cannot read the selected file" }.use { input ->
                            output.outputStream().buffered().use { target ->
                                val buffer = ByteArray(64 * 1024)
                                var bytes = 0L
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    bytes += count
                                    require(bytes <= 2L * 1024 * 1024 * 1024) { "Selected file exceeds 2 GiB" }
                                    target.write(buffer, 0, count)
                                }
                                require(bytes > 0) { "Selected file is empty" }
                            }
                        }
                    }
                }
                status("Checking selected APK…")
                delivered = vm.onImportedFile(Uri.fromFile(file).toString(), webView.settings.userAgentString)
                if (!delivered) status("The download session ended. Close this page and retry.")
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                status("Could not import file: ${error.message ?: "read failed"}")
            } finally { if (!delivered) temporary?.delete() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        vm.onPageReady = null
        vm.onStatus = null
        if (::webView.isInitialized) { webView.stopLoading(); webView.destroy() }
        super.onDestroyView()
    }

    private fun cancelActivity() {
        val activity = requireActivity()
        activity.setResult(Activity.RESULT_CANCELED)
        activity.finish()
    }

    @Parcelize
    internal class Parameters(
        val title: String, val events: IBinder
    ) : Parcelable

    internal companion object {
        const val KEY = "params"
    }
}

@OptIn(DownloaderHostApi::class)
internal class WebViewModel : ViewModel() {
    init {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
        }
    }

    var onPageReady: ((WebView, String) -> Unit)? = null
    var onStatus: ((String) -> Unit)? = null
    @Volatile var browserAgent = ""
    @Volatile var downloadRequested = false
    @Volatile var mirrorMode = false
    @Volatile var blockAds = true
    var originalUrl = ""
    private val downloadStarted = AtomicBoolean(false)
    private val commandChannel = Channel<Command>()
    val commands = commandChannel.receiveAsFlow()

    private var eventBinder: IWebViewEvents? = null
    private val ctrlBinder = object : IWebView.Stub() {
        override fun load(url: String?) {
            viewModelScope.launch {
                commandChannel.send(Command.Load(url!!))
            }
        }

        override fun finish() {
            viewModelScope.launch {
                commandChannel.send(Command.Finish)
            }
        }
    }

    val webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (captureFile(request)) return true
            val blocked = mirrorMode && request.isForMainFrame && !ApkMirrorPolicy.isMirror(request.url.toString())
            if (blocked) onStatus?.invoke("External page blocked. Use Open browser if this link is needed.")
            return blocked
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
            if (request.isForMainFrame) {
                downloadRequested = false
                onStatus?.invoke("Page failed to load. Open browser, download there, then Use downloaded APK.")
            }
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
            if (request.isForMainFrame && response.statusCode >= 400) {
                downloadRequested = false
                onStatus?.invoke("Source returned HTTP ${response.statusCode}. Try Open browser and Use downloaded APK.")
            }
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            if (captureFile(request)) {
                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(byteArrayOf()))
            }
            if (mirrorMode && blockAds && !request.isForMainFrame && ApkMirrorPolicy.isAd(request.url.toString())) {
                return WebResourceResponse("text/plain", "UTF-8", 204, "No Content", emptyMap(), ByteArrayInputStream(byteArrayOf()))
            }
            return null
        }
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            eventBinder?.pageLoad(url)
            if (view != null && url != null) onPageReady?.invoke(view, url)
        }
    }

    private fun captureFile(request: WebResourceRequest): Boolean {
        if (!mirrorMode || !request.isForMainFrame || !ApkMirrorPolicy.isDownloadFile(request.url.toString())) return false
        downloadRequested = false
        emitDownload(request.url.toString(), "application/octet-stream", browserAgent)
        return true
    }

    fun onDownload(url: String, mimeType: String, userAgent: String) {
        if (mirrorMode && !ApkMirrorPolicy.isDownloadFile(url)) {
            downloadRequested = false
            onStatus?.invoke("Protected download needs a full browser. Open browser, then Use downloaded APK.")
            return
        }
        emitDownload(url, mimeType, userAgent)
    }

    fun onImportedFile(url: String, userAgent: String): Boolean = emitDownload(url, "application/octet-stream", userAgent)

    private fun emitDownload(url: String, mimeType: String, userAgent: String): Boolean {
        val binder = eventBinder ?: return false
        if (!downloadStarted.compareAndSet(false, true)) return false
        return try { binder.download(url, mimeType, userAgent); true }
        catch (error: android.os.RemoteException) {
            downloadStarted.set(false)
            onStatus?.invoke("Download session disconnected. Close the page and retry.")
            false
        }
    }

    fun setup(binder: IWebViewEvents) {
        if (eventBinder != null) return
        eventBinder = binder
        binder.ready(ctrlBinder)
    }

    sealed interface Command {
        data class Load(val url: String) : Command
        data object Finish : Command
    }
}
