package io.golos.golos.utils

import android.animation.LayoutTransition
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.database.Cursor
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.LayoutRes
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.SearchView
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.fasterxml.jackson.databind.JsonNode
import eu.bittrade.libs.steemj.base.models.Account
import eu.bittrade.libs.steemj.base.models.operations.CommentOperation
import eu.bittrade.libs.steemj.communication.CommunicationHandler
import io.golos.golos.BuildConfig
import io.golos.golos.R
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by yuri yurivladdurain@gmail.com on 25/10/2017.
 */
object Counter {
    val counter = AtomicInteger(-1)
}

public fun Any.nextInt() = Counter.counter.incrementAndGet()

fun Cursor.getString(columnName: String): String? {
    val columnNumber = this.getColumnIndex(columnName)
    if (columnNumber < 0) return null
    return this.getString(this.getColumnIndex(columnName))
}

fun Cursor.getLong(columnName: String): Long {
    val columnNumber = this.getColumnIndex(columnName)
    if (columnNumber < 0) return 0L
    return this.getLong(this.getColumnIndex(columnName))
}

fun Cursor.getBool(columnName: String): Boolean {
    val columnNumber = this.getColumnIndex(columnName)
    if (columnNumber < 0) return false
    return this.getInt(this.getColumnIndex(columnName)) > 0
}

fun Cursor.getInt(columnName: String): Int {
    val columnNumber = this.getColumnIndex(columnName)
    if (columnNumber < 0) return 0
    return this.getInt(this.getColumnIndex(columnName))
}

fun Activity.restart() {
    this.recreate()
}

fun <T : View> ViewGroup.inflate(@LayoutRes layoutResId: Int): T {
    return LayoutInflater.from(this.context).inflate(layoutResId, this, false) as T
}

fun Cursor.getDouble(columnName: String): Double {
    return this.getDouble(this.getColumnIndex(columnName))
}

fun String.asIntentToShowUrl(): Intent {
    val i = Intent(Intent.ACTION_VIEW)
    i.data = Uri.parse(this);
    return i
}

fun TextView.setTextColorCompat(@ColorRes colorId: Int) {
    this.setTextColor(ContextCompat.getColor(this.context, colorId))
}

fun Activity.getColorCompat(@ColorRes coloId: Int): Int {
    return ContextCompat.getColor(this, coloId)
}

fun Fragment.getColorCompat(@ColorRes coloId: Int): Int {
    return ContextCompat.getColor(activity!!, coloId)
}

fun View.getColorCompat(@ColorRes coloId: Int): Int {
    return ContextCompat.getColor(context!!, coloId)
}

fun Fragment.showProgressDialog(): ProgressDialog {
    val dialog = ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
    dialog.isIndeterminate = true
    dialog.setCancelable(false)
    dialog.show()
    val progress = dialog.findViewById<View>(android.R.id.progress) as ProgressBar
    progress.indeterminateDrawable.setColorFilter(ContextCompat.getColor(context!!, R.color.colorAccent), PorterDuff.Mode.MULTIPLY)
    dialog.setTitle(R.string.loading)
    return dialog
}

fun Activity.showProgressDialog(): ProgressDialog {
    val dialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
    dialog.isIndeterminate = true
    dialog.setCancelable(false)
    dialog.show()
    val progress = dialog.findViewById<View>(android.R.id.progress) as ProgressBar
    progress.indeterminateDrawable.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent), PorterDuff.Mode.MULTIPLY)
    dialog.setTitle(R.string.loading)
    return dialog
}

fun ViewGroup.setFullAnimationToViewGroup() {
    val layoutTransition = LayoutTransition()
    layoutTransition.enableTransitionType(LayoutTransition.APPEARING)
    layoutTransition.enableTransitionType(LayoutTransition.DISAPPEARING)
    layoutTransition.enableTransitionType(LayoutTransition.CHANGE_APPEARING)
    layoutTransition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
    layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
    this.layoutTransition = layoutTransition
}

fun Context.hideKeyboard(currentFocus: View) {
    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
}

fun SearchView.setTextColorHint(@ColorRes coloId: Int) {
    try {
        (this.findViewById<EditText>(android.support.v7.appcompat.R.id.search_src_text) as EditText)
                .setHintTextColor(this.getColorCompat(R.color.text_color_white_black))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

val Account.avatarPath: String?
    get() {
        var avatarPath: String? = null
        try {
            if (jsonMetadata != null && jsonMetadata.isNotEmpty()) {
                var node: JsonNode? = CommunicationHandler.getObjectMapper().readTree(jsonMetadata)
                node?.let {
                    avatarPath = node.get("profile")?.get("profile_image")?.asText()
                }
            }

        } catch (e: IOException) {
            println("error parsing metadata " + jsonMetadata)
            e.printStackTrace()
        }
        return avatarPath
    }

val Account.moto: String?
    get() {
        var moto: String? = null
        try {
            if (jsonMetadata != null && jsonMetadata.isNotEmpty()) {
                var node: JsonNode? = CommunicationHandler.getObjectMapper().readTree(jsonMetadata)
                node?.let {
                    moto = node.get("profile")?.get("about")?.asText()
                }
            }

        } catch (e: IOException) {
            println("error parsing metadata " + jsonMetadata)
            e.printStackTrace()
        }
        return moto
    }

fun String.toHtml(): Spanned {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)

    } else {
        return Html.fromHtml(this)
    }
}

fun View.showSnackbar(message: Int) {
    Snackbar.make(this,
            Html.fromHtml("<font color=\"#ffffff\">${resources.getString(message)}</font>"),
            Toast.LENGTH_SHORT).show()
}

fun View.showSnackbar(message: String) {
    Snackbar.make(this,
            Html.fromHtml("<font color=\"#ffffff\">${message}</font>"),
            Toast.LENGTH_SHORT).show()
}

fun View.setViewGone() {
    if (this.visibility != View.GONE) this.visibility = View.GONE
}

fun View.setViewVisible() {
    if (this.visibility != View.VISIBLE) visibility = View.VISIBLE
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(
            Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(this.windowToken, 0)
}

fun View.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInputFromInputMethod(windowToken, 0)
}

fun Context.getVectorDrawable(@DrawableRes resId: Int): Drawable {
    return AppCompatResources.getDrawable(this, resId)!!
}

fun View.getVectorDrawable(@DrawableRes resId: Int): Drawable {
    return AppCompatResources.getDrawable(context, resId)!!
}


fun CommentOperation.getTags(): List<String> {
    val out = ArrayList<String>()
    try {
        if (jsonMetadata != null && jsonMetadata.isNotEmpty()) {
            var node: JsonNode? = CommunicationHandler.getObjectMapper().readTree(jsonMetadata)
            node?.let {
                val tags = node.get("tags")?.asIterable()
                tags?.forEach({
                    out.add(it.asText())
                })
            }
        }

    } catch (e: IOException) {
        println("error parsing metadata " + jsonMetadata)
        e.printStackTrace()
    }
    return out

}

fun <E> List<out E>.toArrayList(): ArrayList<E> {
    return ArrayList(this)
}

fun File.sizeInKb(): Long {
    return length() / 1024
}


public inline fun <E> List<out E>?.isNullOrEmpty(): Boolean = this == null || this.size == 0


public fun <V> bundleOf(vararg pairs: Pair<String, V>): Bundle {
    val b = Bundle()
    pairs.forEach {
        val second = it.second
        when (second) {
            is Short -> b.putShort(it.first, second)
            is Int -> b.putInt(it.first, second)
            is Long -> b.putLong(it.first, second)
            is String -> b.putString(it.first, second)
            is ArrayList<*> -> {
                if (second.isNotEmpty() && second[0] !is String) {
                    throw IllegalArgumentException("only arraylist of strings supported")
                } else {
                    b.putStringArrayList(it.first, second as java.util.ArrayList<String>)
                }
            }
            else -> throw IllegalArgumentException("unsupported")
        }
    }
    return b
}

public fun TextView.setVectorDrawableStart(id: Int) {
    this.setCompoundDrawablesWithIntrinsicBounds(this.getVectorDrawable(id), null, null, null)
}

fun isOnMainThread(): Boolean {
    if (BuildConfig.DEBUG) return true
    return (Looper.getMainLooper() == Looper.myLooper())
}
