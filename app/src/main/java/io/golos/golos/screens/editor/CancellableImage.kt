package io.golos.golos.screens.editor

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
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
            field = value
            if (value == null) {
                mImage.setImageBitmap(null)
                mImage.visibility = View.GONE
                mProgressBar.visibility = View.GONE
                mCancelButton.visibility = View.GONE
            } else {
                mImage.visibility = View.VISIBLE
                mCancelButton.visibility = View.VISIBLE
                Glide.with(context)
                        .load(field?.imageSrc ?: "")
                        .apply(RequestOptions().fitCenter())
                        .into(mImage)
            }
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.v_cancellable_image, this, true)
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



