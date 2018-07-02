package io.golos.golos.screens.editor.viewholders

import android.content.Context
import android.support.annotation.LayoutRes
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import android.text.style.QuoteSpan
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import io.golos.golos.R
import io.golos.golos.screens.editor.*
import io.golos.golos.screens.editor.knife.KnifeBulletSpan
import io.golos.golos.screens.editor.knife.NumberedMarginSpan
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.screens.widgets.SelectionAwareEditText
import timber.log.Timber


class EditorEditTextViewHolder(@LayoutRes res: Int, parent: ViewGroup) :
        GolosViewHolder(res, parent), TextWatcher, View.OnFocusChangeListener, SelectionAwareEditText.SelectionListener {
    private var mEditText: SelectionAwareEditText = itemView.findViewById(R.id.et)
    private val whiteSpace = Character.valueOf('\u0020')
    private var ignoreTextChange = false
    private var ignoreOnBind = false
    private var mLastText = ""

    init {
        mEditText.setSelectionListener(this)
        mEditText.addTextChangedListener(this)
        mEditText.setEditableFactory(object : Editable.Factory() {
            override fun newEditable(source: CharSequence?): Editable {
                source ?: return "".asSpannable()
                if (source is Editable) return source
                return super.newEditable(source)
            }
        })
    }

    var state: EditorAdapterTextPart? = null
        set(value) {
            if (value == null) return
            if (ignoreOnBind) {
                field = value
                return
            }
            Timber.e("setting new state ${value.textPart}")
            ignoreTextChange = true
            mEditText.setSelectionListener(null)

            var textChanged = false

            val textPart = value.textPart
            if (!compareGolosSpannables(textPart.text, mEditText.text)) {
                Timber.e("setting new text \nold = ${mEditText.text}" +
                        " new = ${textPart}")
                mEditText.setTextKeepState(textPart.text)



                textChanged = true
                var start = value.textPart.startPointer
                var end = value.textPart.endPointer
                mEditText.post {
                    Timber.e("setting new selection state = ${value.textPart}\n start = ${start} " +
                            "end = ${end}")

                    if (start > mEditText.text.length) start = mEditText.length()
                    if (end > mEditText.text.length) end = mEditText.length()

                    if (start > -1 && end > -1)
                        mEditText.setSelection(start, end)
                }
            }
            if (textPart.isFocused() && !textChanged) {
                if (textPart.startPointer != mEditText.selectionStart || textPart.endPointer != mEditText.selectionEnd) {
                    Timber.e("selection not matches")
                    mEditText.post {

                        mEditText.setSelectionListener(null)

                        var selectionstart = if (textPart.startPointer > textPart.text.length) textPart.text.length else textPart.startPointer
                        var selectionEnd = if (textPart.endPointer > textPart.text.length) textPart.text.length else textPart.endPointer
                        selectionstart = if (selectionstart == -1) textPart.text.length else selectionstart
                        selectionEnd = if (selectionEnd == -1) textPart.text.length else selectionEnd

                        mEditText.setSelection(selectionstart, selectionEnd)

                        mEditText.setSelectionListener(this)
                    }
                }
            }

            if (mEditText.onFocusChangeListener != this) {
                mEditText.onFocusChangeListener = this
            }

            if (value.showHint) {
                val hint = itemView.context.getText(R.string.enter_text)
                if (mEditText.hint != hint) {
                    mEditText.hint = hint
                }
            }
            Timber.e("end on bind")
            ignoreTextChange = false
            mEditText.setSelectionListener(this)
            field = value
        }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {

        Timber.e("onFocusChange")
        if (v != null && v == mEditText) {
            val currentState = state ?: return

            if (mEditText.text.isEmpty()) mEditText.text.append(" ")


            if (hasFocus) {
                if (mEditText.selectionStart < 0) mEditText.setSelection(0, 0)
                currentState.textPart.startPointer = mEditText.selectionStart
                currentState.textPart.endPointer = mEditText.selectionEnd
                currentState.onFocusChanged(this, true)
            } else {
                currentState.textPart.startPointer = EditorPart.CURSOR_POINTER_NOT_SELECTED
                currentState.textPart.endPointer = EditorPart.CURSOR_POINTER_NOT_SELECTED
                currentState.onFocusChanged(this, false)
            }
        }
    }

    override fun afterTextChanged(s: Editable?) {
        val currentState = state ?: return
        s ?: return

        Timber.e("afterTextChanged s = $s , its length is ${s.length} selected mods = ${currentState.modifiers},\n" +
                "selection = ${mEditText.selectionEnd}")

        ignoreOnBind = true


        val spans = s.getSpans(mEditText.selectionStart, mEditText.selectionEnd, MetricAffectingSpan::class.java)
        if (mEditText.text.isNotEmpty()) {
            spans.forEach {
                val spanStart = s.getSpanStart(it)
                val spanEnd = s.getSpanEnd(it)
                val spanFlag = s.getSpanFlags(it)
                if (spanStart == spanEnd && (spanStart - spanEnd == 0) && spanFlag != EXCLUSIVE_INCLUSIVE) {
                    s.removeSpan(it)
                    s.setSpan(it, spanStart - 1, spanEnd, spanFlag)
                }
            }
        }

        val selection = mEditText.selectionEnd



        s.printLeadingMarginSpans(mEditText.selectionEnd)
        Timber.e("mEditText.selectionStart = ${mEditText.selectionStart}")
        if (isLastEditAppendedSmth() && s.isPreviousCharLineBreak(selection)) {
            Timber.e("isPreviousCharLineBreaker")
            val spans = s.getSpans(mEditText.selectionStart,
                    selection,
                    LeadingMarginSpan::class.java)
            spans.forEach {
                Timber.e("moving leading margin spans backwards")
                val spanStart = s.getSpanStart(it)
                val spanEnd = s.getSpanEnd(it)
                if (spanStart == spanEnd) s.insert(spanEnd, " ")
                s.removeSpan(it)
                if (it is QuoteSpan) s.setSpan(it, spanStart, selection - 1, INCLUSIVE_INCLUSIVE)
                else if (s.isPreviousCharLineBreak(selection - 1) && (it is KnifeBulletSpan || it is NumberedMarginSpan)) {
                    //two ore more line breaks, don't add additional bullet spans
                    Timber.e("two ore more line breaks")
                } else if ((it is KnifeBulletSpan || it is NumberedMarginSpan)) {
                    s.setSpan(it, spanStart, mEditText.selectionEnd - 1, INCLUSIVE_INCLUSIVE)

                    Timber.e("adding  additional $it span mEditText.selectionStart = ${mEditText.selectionStart}" +
                            "mEditText.selectionEnd = ${mEditText.selectionEnd} ")
                    val newSpan = when (it) {
                        is KnifeBulletSpan -> KnifeBulletSpan(it.bulletColor, it.bulletRadius, it.bulletGapWidth)
                        is NumberedMarginSpan -> it.nextIndex()
                        else -> Any()
                    }
                    var spanEnd = selection
                    if (s.isWithinWord(selection)) {
                        spanEnd = s.getParagraphBounds(selection).second
                    }
                    s.setSpan(newSpan,
                            mEditText.selectionStart,
                            spanEnd,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                }
            }
        }

        s.printStyleSpans(mEditText.selectionStart, mEditText.selectionEnd)


        if (mEditText.selectionStart > currentState.textPart.startPointer) currentState.textPart.startPointer = mEditText.selectionStart
        if (mEditText.selectionEnd > currentState.textPart.endPointer) currentState.textPart.endPointer = mEditText.selectionEnd

        val newState = currentState.textPart.setText(s)
        state = EditorAdapterTextPart(newState,
                currentState.onFocusChanged,
                currentState.onNewText,
                currentState.onCursorChange,
                currentState.showHint,
                currentState.modifiers)
        ignoreOnBind = false
        if (!ignoreTextChange) currentState.onNewText.invoke(this, state?.textPart ?: return)
        mLastText = s.toString()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {


    }

    public fun reSetText() {
        ignoreOnBind = true

        val text = SpannableStringBuilder(mEditText.text)
        mEditText.setTextKeepState(text)

        ignoreOnBind = false
    }

    private fun isLastEditAppendedSmth() = mLastText.length < mEditText.text.length

    override fun onSelectionChanged(start: Int, end: Int) {
        val currentState = state ?: return
        val spans = mEditText.text.getSpans(start, end, LeadingMarginSpan::class.java)
        if (spans.size > 1) {
            Timber.e("removing leading spans")
            val classOfFirstSpan = spans[0]::class.java
            val listOfSameSpans = classOfFirstSpan to arrayListOf(spans[0])
            spans.indices.forEach {
                //glueing neighbor spans
                if (it != 0) {
                    if (spans[it]::class.java == classOfFirstSpan) {
                        listOfSameSpans.second.add(spans[it])
                    } else {
                        mEditText.text.removeSpan(spans[it])
                    }
                }
            }
            if (listOfSameSpans.second.size > 1) {
                val spanStart = listOfSameSpans.second.map { mEditText.text.getSpanStart(it) }.min()
                        ?: 0
                val spanEnd = listOfSameSpans.second.map { mEditText.text.getSpanEnd(it) }.max()
                        ?: 0
                if (spanEnd >= spanStart) {
                    listOfSameSpans.second.forEach { mEditText.text.removeSpan(it) }
                    mEditText.text.setSpan(listOfSameSpans.second.first(), spanStart, spanEnd, INCLUSIVE_INCLUSIVE)
                }

            }
        }
        Timber.e("onSelectionChanged start = $start end = $end")
        if (start == -1 || end == -1) {
            currentState.textPart.startPointer = EditorPart.CURSOR_POINTER_NOT_SELECTED
            currentState.textPart.endPointer = EditorPart.CURSOR_POINTER_NOT_SELECTED
        } else {
            currentState.textPart.startPointer = start
            currentState.textPart.endPointer = end
        }
        currentState.onCursorChange.invoke(this, currentState.textPart)
    }

    fun shouldShowKeyboard() {
        Timber.e("shouldShowKeyboard")
        if (mEditText.isFocused) return

        if (!mEditText.isFocused) mEditText.requestFocus()
        mEditText.postDelayed({
            Timber.e("showing keyboard")
            mEditText.setSelection(getStartSelection(), getEndSelection())
            mEditText.requestFocus()
            val inputMethodManager = mEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT)

        }, 50)
    }

    private fun getStartSelection(state: EditorAdapterTextPart? = this.state): Int {
        return when {
            state == null -> 0
            state.textPart.startPointer == -1 -> 0
            state.textPart.startPointer > state.textPart.text.length -> state.textPart.text.length
            else -> state.textPart.startPointer
        }
    }

    private fun getEndSelection(state: EditorAdapterTextPart? = this.state): Int {
        return when {
            state == null -> 0
            state.textPart.endPointer == -1 -> 0
            state.textPart.endPointer > state.textPart.text.length -> state.textPart.text.length
            else -> state.textPart.endPointer
        }
    }
}