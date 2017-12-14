package io.golos.golos.screens.webview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import timber.log.Timber

/**
 * Created by yuri on 13.12.17.
 */
class WebViewActivity : GolosActivity(), AdvancedWebView.ProgressListener {
    private lateinit var mProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_webview)
        val webview = findViewById<AdvancedWebView>(R.id.webview)
        mProgressBar = findViewById(R.id.progress_bar)
        webview.setProgressListener(this)
        if (intent.hasExtra(TAG_URI)) {
            webview.loadUrl(intent.getStringExtra(TAG_URI))
        }

    }

    override fun onProgress(progress: Int) {
        mProgressBar.progress = progress
        if (progress > 99) mProgressBar.visibility = View.INVISIBLE
        else mProgressBar.visibility = View.VISIBLE
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