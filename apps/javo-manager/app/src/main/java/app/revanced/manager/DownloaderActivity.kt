package app.revanced.manager

import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.downloader.webview.WebViewFragment


@OptIn(DownloaderHostApi::class)
class DownloaderActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = FragmentContainerView(this).apply {
            // The fragment manager requires an ID to work.
            id = R.id.fragment_container
        }
        setContentView(
            view,
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )

        if (intent.getStringExtra("FRAGMENT_CLASS_NAME") != WebViewFragment::class.java.name) {
            finish()
            return
        }
        val args = intent.getBundleExtra("FRAGMENT_ARGS")
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.fragment_container, WebViewFragment::class.java, args)
            }
        }
    }
}
