package io.golos.golos.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.support.annotation.DimenRes
import android.support.annotation.WorkerThread
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import io.golos.golos.BuildConfig.DEBUG_EDITOR
import io.golos.golos.screens.editor.knife.KnifeBulletSpan
import io.golos.golos.screens.editor.knife.KnifeQuoteSpan
import io.golos.golos.screens.editor.knife.KnifeURLSpan
import io.golos.golos.screens.editor.knife.NumberedMarginSpan
import io.golos.golos.utils.sizeInKb
import io.golos.golos.utils.toArrayList
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream


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

    Timber.e("compareGolosSpannables, first = $firstSpannables second $secondSpannables")

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

fun Context.getDimen(@DimenRes resid: Int): Float {
    return resources.getDimension(resid)
}

fun CharSequence.isPreviousCharLineBreak(pointToChar: Int): Boolean {
    return length > 1 && pointToChar != 0 && pointToChar <= length && this[pointToChar - 1] == '\n'
}

fun CharSequence.isPreviousCharWhiteSpaceThenLineBreak(pointToChar: Int): Boolean {
    return length > 2 && pointToChar > 2 && pointToChar <= length && this[pointToChar - 1] == ' ' && isPreviousCharLineBreak(pointToChar - 1)
}

fun Spanned.trimStartAndEnd(): Spanned {
    if (this is SpannableStringBuilder) {
        while (!this.isEmpty() && (this[0].isWhitespace() || this[0].toShort() == 8205.toShort())) delete(0, 1)
        while (!this.isEmpty() && (this[lastIndex].isWhitespace() || this[0].toShort() == 8205.toShort())) delete(lastIndex, length)
    }
    return this
}

fun Spannable.removeAllLeadingMarginSpansAt(start: Int, end: Int) {
    getSpans(start, end, LeadingMarginSpan::class.java).forEach {
        removeSpan(it)
    }
}

fun Spannable.changeLeadingSpansFlagParagraphToInclusiveInclusive() {
    getSpans(0, length, LeadingMarginSpan::class.java).forEach {
        val start = getSpanStart(it)
        val end = getSpanEnd(it)
        removeSpan(it)
        setSpan(it, start, end, INCLUSIVE_INCLUSIVE)
    }
}

fun Editable.getEditorUsedSpans(start: Int, end: Int): Set<EditorTextModifier> {
    if (start > end) return setOf()
    if (end > length) return setOf()
    val out = hashSetOf<EditorTextModifier>()
    val all = getSpans(start, end, Any::class.java)

    (0 until all.size).forEach {
        val span = all[it]
        if (span::class.java in urlSpans) {
            if (isSpanTouchesEndPointWithExclusiveSpan(span, start) || isSpanTouchesEndPointWithExclusiveSpan(span, end) && start == end) {
                //if selection touches span border with exlclusive border, not touch it
            } else {
                out.add(EditorTextModifier.LINK)
            }
        } else if (span is StyleSpan) {

            if (span.style == Typeface.BOLD) {
                if (isSpanTouchesEndPointWithExclusiveSpan(span, start) || isSpanTouchesEndPointWithExclusiveSpan(span, end) && start == end) {
                    //if selection touches span border with exlclusive border, not touch it
                } else {
                    out.add(EditorTextModifier.STYLE_BOLD)
                }
            }

        } else if (span is AbsoluteSizeSpan) {
            if (isSpanTouchesEndPointWithExclusiveSpan(span, start) || isSpanTouchesEndPointWithExclusiveSpan(span, end) && start == end) {
                //if selection touches span border with exlclusive border, not touch it
            } else {
                out.add(EditorTextModifier.TITLE)
            }
        } else if (span is KnifeQuoteSpan) out.add(EditorTextModifier.QUOTATION)
        else if (span is KnifeBulletSpan) out.add(EditorTextModifier.LIST_BULLET)
        else if (span is NumberedMarginSpan) out.add(EditorTextModifier.LIST_NUMBERED)
    }
    if (start != end && (
                    (start > 0 && this[start - 1] == '"') &&
                            (end < length && this[end] == '"'))) out.add(EditorTextModifier.QUOTATION_MARKS)
    else if (start == end && isWordWrappedByQuotationMarks(start)) out.add(EditorTextModifier.QUOTATION_MARKS)
    return out

}

fun Editable.getPreviousPositionIsNumericList(pointerToPosition: Int): NumberedMarginSpan? {
    if (length > 1 && pointerToPosition > 1) {
        getSpans(pointerToPosition - 1, pointerToPosition - 1, NumberedMarginSpan::class.java)
                .firstOrNull { return it }
    }
    return null
}

fun Spannable.isSpanTouchesEndPointWithExclusiveSpan(span: Any, endPoint: Int): Boolean {
    val spanFlags = getSpanFlags(span)
    val spanEnd = getSpanEnd(span)
    return spanEnd == endPoint && (spanFlags == INCLUSIVE_EXCLUSIVE || spanFlags == EXCLUSIVE_EXCLUSIVE)
}

fun Spannable.isSpanTouchesStartPointWithExclusiveSpan(span: Any, startPoint: Int): Boolean {
    val spanFlags = getSpanFlags(span)
    val spanStart = getSpanStart(span)
    return spanStart == startPoint && (spanFlags == EXCLUSIVE_INCLUSIVE || spanFlags == EXCLUSIVE_EXCLUSIVE)
}

fun checkStartAndEnd(start: Int, end: Int): Boolean {
    if (start > end) {
        Timber.e("start > end, this should not happen")
        return false
    }
    if (start == -1) {
        Timber.e("start == - 1, this should not happen")
        return false
    }
    if (end == -1) {
        Timber.e("end == - 1, this should not happen")
        return false
    }

    return true
}

fun CharSequence.isWordWrappedByQuotationMarks(pointerToWord: Int): Boolean {
    if (length == 0) return false
    if (!isWithinWord(pointerToWord)) return false

    val startOfWord = getStartOfWord(pointerToWord)
    val endOfWord = getEndOfWord(pointerToWord)

    if (!checkStartAndEnd(startOfWord, endOfWord)) return false

    return this[startOfWord] == '"' && this[endOfWord] == '"'
}

fun Editable.getZeroWhiteSpace() = "\u2063"

fun Char.isQuotationMark() = this == '"'

//|a a|b ab| _|sdg 123_456
fun CharSequence.isWithinWord(pointer: Int): Boolean {
    return if (length == 0) return false
    else if (pointer != length && this[pointer].isPartOfWord()) return true
    else if (pointer > 0 && pointer < length && this[pointer].isPartOfWord()) return true
    else return false

}

fun CharSequence.getLineOfWordPosition(wordPosition: Int): Int {
    if (length == 0) return 0
    val textParts = split("\n")
    var pointerToTextPart = 0
    var currentLength = 0
    textParts.forEachIndexed({ index, string ->
        currentLength += string.length
        if (wordPosition >= currentLength) {
            pointerToTextPart = index
        }
    })
    return pointerToTextPart
}

fun Editable.printAllSpans() {
    if (!DEBUG_EDITOR) return
    getSpans(0, length, Any::class.java).forEach {
        Timber.e("span = $it, start = ${getSpanStart(it)} end = ${getSpanEnd(it)}")
    }
}

fun CharSequence.getParagraphBounds(pointerPosition: Int): Pair<Int, Int> {
    if (length == 0) return 0 to 0

    val textParts = split("\n")
    if (pointerPosition == 0) return 0 to textParts[0].length
    if (textParts.size == 1) return 0 to textParts[0].length
    var out = 0 to 0
    var currentEnd = 0
    var currentStartLength = 0
    textParts.forEachIndexed({ index, string ->
        currentEnd += string.length
        if (pointerPosition <= currentEnd) {
            return currentStartLength to currentEnd
        }
        currentStartLength += string.length
        currentEnd += 1
        currentStartLength += 1
    })
    return out
}

fun Char.isWordBreaker() = !this.isLetter() && !this.isDigit() && this != '"'

fun Char.isPartOfWord() = !isWordBreaker()


fun Editable.printStyleSpans(start: Int = 0, end: Int = length) {
    if (!DEBUG_EDITOR) return
    getSpans(start, end, StyleSpan::class.java).forEach {
        Timber.e("span = $it \n start = ${getSpanStart(it)} end = ${getSpanEnd(it)} " +
                "flag is ${getSpanFlags(it)}")
    }
}


fun Editable.removeUrlSpans(start: Int, end: Int) {
    val spans = getSpans(start, end, Any::class.java)
    spans.forEach {
        if (it in urlSpans) removeSpan(it)
    }
}

fun CharSequence.getStartOfWord(pointerToTheMiddleOfWord: Int): Int {
    if (!isWithinWord(pointerToTheMiddleOfWord)) return -1
    var pointerToTheMiddleOfWord = pointerToTheMiddleOfWord
    if (length == 0) return 0
    if (pointerToTheMiddleOfWord >= length) pointerToTheMiddleOfWord = lastIndex
    if (this[pointerToTheMiddleOfWord] == '"') pointerToTheMiddleOfWord -= 1

    return (pointerToTheMiddleOfWord downTo 0)
            .find {
                val char = this[it]
                val isBreaker = char.isWordBreaker()

                if (isBreaker) {
                    return it + 1
                } else false
            }
            ?: 0
}

fun CharSequence.isEndOf(pointerToPosition: Int): Boolean {
    return pointerToPosition >= this.trimEnd().length
}

fun CharSequence.isStartOf(pointerToPosition: Int): Boolean {
    return pointerToPosition == 0
}

fun CharSequence.isEndOfLine(pointerToPosition: Int): Boolean {
    val isEnd = pointerToPosition >= this.trimEnd().length
    if (isEnd) return true
    return this[pointerToPosition - 1].isWordBreaker()
}


fun CharSequence.isLastCharLineBreaker() = length > 0 && this[lastIndex] == '\n'


fun Spanned.printLeadingMarginSpans(pointerToPosition: Int) {
    if (!DEBUG_EDITOR) return
    getSpans(pointerToPosition, length, LeadingMarginSpan::class.java)
            .forEach {
                Timber.e("$it start = ${getSpanStart(it)} end = ${getSpanEnd(it)} , flag  = ${getSpanFlags(it)}")
            }
}

fun CharSequence.isCharPointBreaker(charPosition: Int) =
        length > 0
                && charPosition < length
                && this[charPosition] == '\n'

fun CharSequence.getEndOfWord(pointerToTheMiddleOfWord: Int): Int {
    if (!isWithinWord(pointerToTheMiddleOfWord)) return -1
    var pointerToTheMiddleOfWord = pointerToTheMiddleOfWord
    if (length == 0) return 0
    if (pointerToTheMiddleOfWord >= length) pointerToTheMiddleOfWord = lastIndex

    return (pointerToTheMiddleOfWord until length)
            .find {
                val char = this[it]
                val isBreaker = char.isWordBreaker()
                if (isBreaker) {
                    return it - 1
                } else false
            }
            ?: lastIndex
}

fun EditorBottomViewHolder.isSelected(styleSpan: MetricAffectingSpan): Boolean {
    val modifiers = getSelectedModifier()
    modifiers.forEach {
        if (it == EditorTextModifier.STYLE_BOLD && styleSpan is StyleSpan && styleSpan.style == Typeface.BOLD) return true
        if (it == EditorTextModifier.TITLE && styleSpan is AbsoluteSizeSpan) return true
    }
    return false
}

fun EditorBottomViewHolder.isSelected(leadingMarginSpan: LeadingMarginSpan): Boolean {
    val modifiers = getSelectedModifier()
    modifiers.forEach {
        if (it == EditorTextModifier.QUOTATION && leadingMarginSpan is KnifeQuoteSpan) return true
        if (it == EditorTextModifier.LIST_BULLET && leadingMarginSpan is KnifeBulletSpan) return true
        if (it == EditorTextModifier.LIST_NUMBERED && leadingMarginSpan is NumberedMarginSpan) return true
    }
    return false
}

fun CharSequence.isPositionNextToWord(pointerToPosition: Int) = length > 1
        && pointerToPosition <= length
        && this[pointerToPosition - 1].isPartOfWord()

fun CharSequence.isPositionNextToWhiteSpace(pointerToPosition: Int) = length > 0
        && pointerToPosition <= length
        && pointerToPosition > 0
        && this[pointerToPosition - 1].isWordBreaker()

private val urlSpans = setOf(KnifeURLSpan::class.java)

private val appSpannables = setOf(
        URLSpan::class.java,
        KnifeURLSpan::class.java,
        KnifeBulletSpan::class.java,
        KnifeQuoteSpan::class.java,
        StyleSpan::class.java)

fun newBoldSpan() = StyleSpan(Typeface.BOLD)

fun MetricAffectingSpan.copy(span: MetricAffectingSpan): MetricAffectingSpan? {
    if (this is StyleSpan && span is StyleSpan) return StyleSpan(span.style)
    if (this is AbsoluteSizeSpan && span is AbsoluteSizeSpan) return AbsoluteSizeSpan(span.size)
    return null
}

const val INCLUSIVE_INCLUSIVE = 18
const val EXCLUSIVE_EXCLUSIVE = 33
const val INCLUSIVE_EXCLUSIVE = 17
const val EXCLUSIVE_INCLUSIVE = 34



