package io.golos.golos.screens.story

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.golos.golos.R


/**
 * Created by yuri on 07.11.17.
 */

class GlideImageGetter(target: TextView,val width: Int) : Html.ImageGetter {

    private var textView: TextView = target
    private val mGlide = Glide.with(textView)

    override fun getDrawable(source: String): Drawable {
        val drawable = URLDrawable(textView.resources, width)

        mGlide
                .asBitmap()
                .apply(RequestOptions().override(textView.width, textView.width))
                .load(source)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                        resource?.let { drawable.bitmap = it }
                        val rect = Rect()
                        textView.getGlobalVisibleRect(rect)
                        textView.invalidate(rect)
                        textView.setText(textView.text)
                    }
                })
        return drawable
    }

    class URLDrawable(val resources: Resources, val width: Int) : BitmapDrawable(resources,
            BitmapFactory.decodeResource(resources, android.R.drawable.screen_background_light_transparent)) {
        var drawable: Drawable

        init {
            val optns = BitmapFactory.Options()
            optns.inJustDecodeBounds = true
            BitmapFactory.decodeResource(resources, R.drawable.error, optns)
            var inHeight = optns.outHeight
            var inWidth = optns.outWidth
            while (inWidth > width) {
                inHeight /= 2
                inWidth /= 2
            }
            val targetRation = width.toDouble() / inWidth
            optns.inSampleSize = calculateInSampleSize(optns, width, (targetRation * inHeight).toInt())

            val outBitmap = BitmapFactory.decodeResource(resources, R.drawable.error)
            val outDrawable = BitmapDrawable(resources, outBitmap)
            outDrawable.setBounds(0, 0, width, (targetRation * inHeight).toInt())
            setBounds(0, 0, width, (targetRation * inHeight).toInt())
            drawable = outDrawable
        }

        fun setBitmap(btmp: Bitmap) {
            var inHeight = btmp.height
            var inWidth = btmp.width
            while (inWidth > width) {
                inHeight /= 2
                inWidth /= 2
            }
            val targetRation = width.toDouble() / inWidth
            val targetHeight = (targetRation * inHeight).toInt()
            val outBitmap = Bitmap.createScaledBitmap(btmp, width, targetHeight, true)
            val outDrawable = BitmapDrawable(resources, outBitmap)
            drawable = outDrawable
            outDrawable.setBounds(0, 0, width, targetHeight)
            setBounds(0, 0, width, targetHeight)
        }

        fun calculateInSampleSize(
                options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {

                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }

        override fun draw(canvas: Canvas) {
            drawable.draw(canvas)
        }
    }
}