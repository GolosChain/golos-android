package io.golos.golos.screens.webview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.Nullable
import com.amplitude.api.Amplitude
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity

/**
 * Created by yuri on 13.12.17.
 */
class WebViewActivity : GolosActivity(), AdvancedWebView.ProgressListener, AdvancedWebView.Listener {
    private lateinit var mProgressBar: ProgressBar
    private val REGISTRATION_URL = "https://reg.golos.io/"
    private val WELCOME_URL = "https://golos.io/welcome"
    @Nullable
    private var mLastLoadedUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_webview)
        val webview = findViewById<AdvancedWebView>(R.id.webview)
        mProgressBar = findViewById(R.id.progress_bar)
        webview.setProgressListener(this)
        webview.setListener(this, this)
        if (intent.hasExtra(TAG_URI)) {
            webview.loadUrl(intent.getStringExtra(TAG_URI))
        }

    }

    override fun onPageStarted(url: String?, favicon: Bitmap?) {

    }

    override fun onPageFinished(url: String?) {
        if (mLastLoadedUrl != null) onPageClose()
        onPageOpen(url)
        mLastLoadedUrl = url
    }

    override fun onPageError(errorCode: Int, description: String?, failingUrl: String?) {

    }

    override fun onDownloadRequested(url: String?, suggestedFilename: String?, mimeType: String?, contentLength: Long, contentDisposition: String?, userAgent: String?) {

    }

    override fun onExternalPageRequest(url: String?) {

    }

    override fun onProgress(progress: Int) {
        mProgressBar.progress = progress
        if (progress > 99) mProgressBar.visibility = View.INVISIBLE
        else mProgressBar.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        onPageClose()
    }

    private fun onPageClose() {
        mLastLoadedUrl ?: return
        if (mLastLoadedUrl?.contains(REGISTRATION_URL) == true) {
            Amplitude.getInstance().logEvent("android_registration_screen1_close")
        } else if (mLastLoadedUrl?.contains(WELCOME_URL) == true) {
            Amplitude.getInstance().logEvent("android_welcome_screen1_close")
        }
    }

    private fun onPageOpen(url: String?) {
        url ?: return
        if (url.contains(REGISTRATION_URL)) {
            Amplitude.getInstance().logEvent("android_registration_screen1_open")
        } else if (url.contains(WELCOME_URL)) {
            Amplitude.getInstance().logEvent("android_welcome_screen1_open")
        }
    }

    companion object {
        private val TAG_URI = "TAG_URI"
        fun start(ctx: Context, uri: String) {
            val i = Intent(ctx, WebViewActivity::class.java)
            i.putExtra(TAG_URI, uri)
            ctx.startActivity(i)
        }
    }
}