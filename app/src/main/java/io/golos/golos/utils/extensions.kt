package io.golos.golos.utils

import android.animation.LayoutTransition
import android.app.ProgressDialog
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.database.Cursor
import android.graphics.PorterDuff
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import com.fasterxml.jackson.databind.JsonNode
import eu.bittrade.libs.steemj.base.models.Account
import eu.bittrade.libs.steemj.communication.CommunicationHandler
import io.golos.golos.R
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by yuri yurivladdurain@gmail.com on 25/10/2017.
 */
object Counter {
    val counter = AtomicInteger(-1)
}

public fun Any.nextInt() = Counter.counter.incrementAndGet()

fun Cursor.getString(columnName: String): String {
    return this.getString(this.getColumnIndex(columnName))
}

fun Cursor.getLong(columnName: String): Long {
    return this.getLong(this.getColumnIndex(columnName))
}

fun Fragment.showProgressDialog(): ProgressDialog {
    val dialog = ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
    dialog.isIndeterminate = true
    dialog.setCancelable(false)
    dialog.show()
    val progress = dialog.findViewById<View>(android.R.id.progress) as ProgressBar
    progress.indeterminateDrawable.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent), PorterDuff.Mode.MULTIPLY)
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