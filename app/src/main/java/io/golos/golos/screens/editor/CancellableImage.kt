package io.golos.golos.screens.editor

import android.content.Context
import android.content.res.ColorStateList
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import com.bumptech.glide.Glide
import io.golos.golos.R

/**
 * Created by yuri yurivladdurain@gmail.com on 24/10/2017.
 *
 */


class CancellableImage @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private val mImage: AppCompatImageView
    private val mProgressBar: ProgressBar
    private val mCancelButton: ImageButton
    var onImageRemove: (String?) -> Unit = {}
    var state: CancellableImageState? = null
        get
        set(value) {
            if (value == null) {
                mImage.setImageBitmap(null)
                mImage.visibility = View.GONE
                mProgressBar.visibility = View.GONE
                mCancelButton.visibility = View.GONE
            } else {
                mImage.visibility = View.VISIBLE
                Glide.with(context).load(value.imageSrc).into(mImage)
                if (value.isLoading) {
                    mProgressBar.visibility = View.VISIBLE
                    mImage.supportImageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray))
                } else {
                    mProgressBar.visibility = View.GONE
                    mImage.supportBackgroundTintList = null
                }
            }
        }

    init {
        addView(LayoutInflater.from(context).inflate(R.layout.vh_imagewrapper, this, true))
        mImage = findViewById(R.id.image)
        mProgressBar = findViewById(R.id.progress)
        mCancelButton = findViewById(R.id.btn_remove)
        mCancelButton.setOnClickListener {
            onImageRemove.invoke(state?.imageSrc)
            state = null
        }
        state = null
    }
}

class CancellableImageState(val isLoading: Boolean, val imageSrc: String)



