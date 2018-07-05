/*
 * Copyright (C) 2015 Matthew Lee
 * Copyright (C) 2013-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2013-2015 Juha Kuitunen
 * Copyright (C) 2013 Mohammed Lakkadshaw
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.golos.golos.screens.editor.knife

import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StrikethroughSpan
import io.golos.golos.screens.editor.isLastCharLineBreaker
import io.golos.golos.screens.editor.printAllSpans
import org.xml.sax.XMLReader
import timber.log.Timber.e
import timber.log.Timber.i

class KnifeTagHandler(private val spanFactory: SpanFactory?) : Html.TagHandler {


    private class Li

    private class Strike

    private class Header

    private class Quote

    private data class NumberedList(val index: Int)

    private var indexOfNumberedList = 0

    private var isOrderedListOpened = false
    private var isUnOrderedListOpened = false

    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
      //  e("tag = $tag opening = $opening output = $output length = ${output.length}")
        if (opening) {
            if (tag == "ol") {
                indexOfNumberedList = 0
                isOrderedListOpened = true
            }else if (tag == "ul"){
                isUnOrderedListOpened = true
            }

            if (tag.equals(LI, ignoreCase = true) && isUnOrderedListOpened) {

                if (!output.isLastCharLineBreaker()) {
                    output.append("\n")
                }
                start(output, Li())
            } else if (tag == LI && isOrderedListOpened) {
                if (!output.isLastCharLineBreaker()) {
                    output.append("\n")
                }
                start(output, NumberedList(++indexOfNumberedList))
            }
            else if (tag.equals(QUOTE, ignoreCase = true)) {
                if (!output.isLastCharLineBreaker()) {
                    output.append("\n")
                }
                start(output, Quote())
            }  else if (tag.equals(STRIKETHROUGH_S, ignoreCase = true) || tag.equals(STRIKETHROUGH_STRIKE, ignoreCase = true) || tag.equals(STRIKETHROUGH_DEL, ignoreCase = true)) {
                start(output, Strike())
            } else if (tag == HEADER) {
                start(output, Header())
            }

        } else {//closing tags
            if (tag == "ol") {
                indexOfNumberedList = 0
                isOrderedListOpened = false
            }else if (tag == "ul"){
                isUnOrderedListOpened = false
            }

            if (tag.equals(LI, ignoreCase = true) && isUnOrderedListOpened) {
                if (!output.isLastCharLineBreaker()) {
                    output.append("\n")
                }
                end(output, Li::class.java, spanFactory?.produceOfType(KnifeBulletSpan::class.java)
                        ?: KnifeBulletSpan(0, 0, 0))

            } else if (tag.equals(LI, ignoreCase = true) && isOrderedListOpened) {
                if (!output.isLastCharLineBreaker()) {
                    output.append("\n")
                }
                end(output, NumberedList::class.java, spanFactory?.produceOfType(NumberedMarginSpan::class.java)
                        ?: NumberedMarginSpan(10, 6, 1))

            } else if (tag.equals(QUOTE, ignoreCase = true)) {
                if (!output.isLastCharLineBreaker()) {
                    output.append("\n")
                }
                end(output, Quote::class.java, spanFactory?.produceOfType(KnifeQuoteSpan::class.java)
                        ?: KnifeQuoteSpan(0, 0, 0))

            } else if (tag.equals(STRIKETHROUGH_S, ignoreCase = true) || tag.equals(STRIKETHROUGH_STRIKE, ignoreCase = true) || tag.equals(STRIKETHROUGH_DEL, ignoreCase = true)) {
                end(output, Strike::class.java, StrikethroughSpan())
            } else if (tag == HEADER) {
                end(output, Header::class.java, spanFactory?.produceOfType(AbsoluteSizeSpan::class.java)
                        ?: AbsoluteSizeSpan(44))
            }
        }
    }

    private fun start(output: Editable, mark: Any) {
      //  i("start, mark = $mark")
        output.setSpan(mark, output.length, output.length, Spanned.SPAN_MARK_MARK)
    }


    private fun end(output: Editable, kind: Class<*>, vararg replaces: Any) {

        val last = getLast(output, kind)
        output.printAllSpans()
        val start = output.getSpanStart(last)
        val end = output.length

      //  i("end ${replaces.toSet()} start = $start, end = $end kind = $kind")
        output.removeSpan(last)
        val numberedList = if (last is NumberedList) NumberedMarginSpan((replaces[0] as NumberedMarginSpan).leadWidth,
                (replaces[0] as NumberedMarginSpan).gapWidth,
                last.index)
        else null

        if (start != end) {
            for (replace in replaces) {
                if (numberedList != null) {
                    output.setSpan(numberedList, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else {
                    if (replace is LeadingMarginSpan) {
                        output.setSpan(replace, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    } else {
                        output.setSpan(replace, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                }

            }
        }
    }

    companion object {
        const val LI = "li"
        const val QUOTE = "custom_quote"
        const val HEADER = "custom_h"
        const val STRIKETHROUGH_S = "s"
        const val STRIKETHROUGH_STRIKE = "strike"
        const val STRIKETHROUGH_DEL = "del"


        private fun getLast(text: Editable, kind: Class<*>): Any? {
            val spans = text.getSpans(0, text.length, kind)

            if (spans.isEmpty()) {
                return null
            } else {
                for (i in spans.size downTo 1) {
                    if (text.getSpanFlags(spans[i - 1]) == Spannable.SPAN_MARK_MARK) {
                        return spans[i - 1]
                    }
                }
                return null
            }
        }
    }
}
