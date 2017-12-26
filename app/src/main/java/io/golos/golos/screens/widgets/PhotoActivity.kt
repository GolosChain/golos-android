package io.golos.golos.screens.widgets

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ProgressBar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoView
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity


/**
 * Created by yuri on 16.11.17.
 */
class PhotoActivity : GolosActivity() {
    private lateinit var mPhotoView: PhotoView
    private lateinit var mProgress: ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.a_photo)
        mPhotoView = findViewById(R.id.photo)
        mProgress = findViewById(R.id.progress)
        Glide.with(this).load(intent
                .getStringExtra(PHOTO_SRC_TAG))
                .apply(RequestOptions().error(R.drawable.error).centerInside())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        mProgress.visibility = View.GONE
                        return false
                    }
                })
                .into(mPhotoView)
    }

    override fun onBackPressed() {
        supportFinishAfterTransition()
    }

    companion object {
        private val PHOTO_SRC_TAG = "PHOTO_SRC_TAG"
        fun startActivity(context: Context,
                          imageSrc: String) {
            val startIntent = Intent(context, PhotoActivity::class.java)
            startIntent.putExtra(PHOTO_SRC_TAG, imageSrc)
            context.startActivity(startIntent)
        }

        //shared id is "image"
        fun startActivityUsingTransition(context: Context,
                                         sharedView: View,
                                         imageSrc: String) {
            val startIntent = Intent(context, PhotoActivity::class.java)
            startIntent.putExtra(PHOTO_SRC_TAG, imageSrc)
           /* val options = ActivityOptionsCompat.makeSceneTransitionAnimation(context as Activity,
                    sharedView,
                    "profile")*/
            context.startActivity(startIntent)
        }
    }
}