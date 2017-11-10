package io.golos.golos.screens

/**
 * Created by yuri on 02.11.17.
 */
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Html

class EmptyImageGetter : Html.ImageGetter {

    override fun getDrawable(source: String): Drawable {
        return TRANSPARENT_DRAWABLE
    }

    companion object {
        private val TRANSPARENT_DRAWABLE = ColorDrawable(Color.TRANSPARENT)
    }
}