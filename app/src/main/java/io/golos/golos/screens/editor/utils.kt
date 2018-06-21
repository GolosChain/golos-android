package io.golos.golos.screens.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.annotation.WorkerThread
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import io.golos.golos.screens.editor.knife.KnifeBulletSpan
import io.golos.golos.screens.editor.knife.KnifeQuoteSpan
import io.golos.golos.screens.editor.knife.KnifeURLSpan
import io.golos.golos.utils.sizeInKb
import io.golos.golos.utils.toArrayList
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList


fun Editable.slice(slicePoint: Int): Pair<Editable, Editable> {
    return Pair(subSequence(0, slicePoint) as Editable, subSequence(slicePoint, length) as Editable)
}

fun Editable.prepend(text: CharSequence) = insert(0, text)

fun String.asSpannable() = SpannableStringBuilder.valueOf(this)

@WorkerThread
public fun resizeToSize(imageFile: File) {

    if (imageFile.sizeInKb() < 800) return
    val optns = BitmapFactory.Options()
    optns.inSampleSize = 2
    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, optns)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(imageFile))
    var step = 1
    while (imageFile.sizeInKb() > 800) {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90 - (step * 10), FileOutputStream(imageFile))
        step++
    }
}

public fun formatUrl(rawUrl: String): String {
    if (rawUrl.isNullOrEmpty()) return rawUrl
    var shownLink = rawUrl
    if (!shownLink.startsWith("http") && !shownLink.contains("www.")) shownLink = "https://www.$shownLink"
    else if (shownLink.startsWith("www.")) shownLink = "https://$shownLink"
    return shownLink
}

fun compareGolosSpannables(first: Editable, second: Editable): Boolean {
    val firstText = first.toString()
    val secondText = second.toString()
    if (firstText != secondText) return false
    val firstSpannables = first.getSpans(0, first.length, Any::class.java).filter {
        it::class.java in appSpannables

    }.toArrayList()

    val secondSpannables = second.getSpans(0, second.length, Any::class.java).filter {
        it::class.java in appSpannables
    }.toArrayList()

    fun isListEquals(first: List<Any>, second: List<Any>): Boolean {
        if (first.size != second.size) return false
        (0 until first.size)
                .forEach {
                    if (first[it] != second[it]) return false
                }
        return true
    }
    return isListEquals(firstSpannables, secondSpannables)
}

fun <T> Array<T>.toArrayList(): ArrayList<T> {
    val list = ArrayList<T>(size)
    forEach {
        list.add(it)
    }
    return list
}

fun <T> Array<T>.toStringCustom(): String {

    val iMax = size - 1
    if (iMax == -1)
        return "[]"

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        b.append(this[i].toString())
        if (i == iMax)
            return b.append(']').toString()
        b.append(", ")
        i++
    }
}

fun Editable.getEditorUsedSpans(start: Int, end: Int): Set<EditorTextModifier> {
    if (start > end) return setOf()
    if (end > length) return setOf()
    val out = hashSetOf<EditorTextModifier>()
    val all = getSpans(start, end, Any::class.java)
    (0 until all.size).forEach {

        if (all[it]::class.java in urlSpans) out.add(EditorTextModifier.LINK)
    }
    if (start != end && (
                    (start > 0 && this[start - 1] == '"') &&
                            (end < length && this[end] == '"'))) out.add(EditorTextModifier.QUOTATION_MARKS)

    Timber.e("getEditorUsedSpans start = $start end = $end , all = ${Arrays.toString(all)}" +
            "\n out = $out")

    return out

}

fun Editable.removeUrlSpans(start: Int, end: Int) {
    val spans = getSpans(start, end, Any::class.java)
    spans.forEach {
        if (it in urlSpans) removeSpan(it)
    }
}

private val urlSpans = setOf(URLSpan::class.java, KnifeURLSpan::class.java)

private val appSpannables = setOf(
        URLSpan::class.java,
        KnifeURLSpan::class.java,
        KnifeBulletSpan::class.java,
        KnifeQuoteSpan::class.java)

