package io.golos.golos.screens.editor

import android.text.Editable
import android.text.SpannableStringBuilder

val emptySpan = SpannableStringBuilder.valueOf("")

fun Editable.slice(slicePoint: Int): Pair<Editable, Editable> {
    return Pair(subSequence(0, slicePoint) as Editable, subSequence(slicePoint, length) as Editable)
}

fun Editable.prepend(text: CharSequence) = insert(0, text)

fun String.asSpannable() = SpannableStringBuilder.valueOf(this)