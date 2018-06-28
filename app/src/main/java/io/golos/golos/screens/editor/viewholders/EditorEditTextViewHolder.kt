package io.golos.golos.screens.editor.viewholders

import android.support.annotation.LayoutRes
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.view.View
import android.view.ViewGroup
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
        // mEditText.onCreateInputConnection(object : EditorInfo() {}.also { it.inputType = EditorInfo.TYPE_NULL })
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

            if (mEditText.text.length == 0 || mEditText.text[mEditText.text.lastIndex] != whiteSpace) {
                Timber.e("edittext is empty or last is not whitespace")

                //    mEditText.text.append(whiteSpace)
                //    mEditText.setSelection(mEditText.selectionStart - 1)
            }


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

        Timber.e("afterTextChanged s = $s  selected mods = ${currentState.modifiers}")

        //      if (s.length == 1)s.setSpan(newBoldSpan(),1,1, Spannable.SPAN_INCLUSIVE_INCLUSIVE)

        ignoreOnBind = true
/*
        if (s.isNotEmpty()) {
            val spans = s.getSpans(mEditText.selectionStart - 1, mEditText.selectionEnd, MetricAffectingSpan::class.java)
            if (s.isNotEmpty() && s.length > mLastText.length) {
                spans.forEach {
                    val start = s.getSpanStart(it)
                    val end = s.getSpanEnd(it)
                    if (start == end && start > 0) {
                        Timber.e("adding missing span")
                        s.removeSpan(it)
                        s.setSpan(it, start - 1, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    }
                }
            }
        }*/
        s.printLeadingMarginSpans(mEditText.selectionEnd)
        if (isLastEditAppendedSmth() && s.isLastCharLineBreaker()) {
            val spans = s.getSpans(s.lastIndex, s.lastIndex, LeadingMarginSpan::class.java)
            spans.forEach {
                Timber.e("moving quote span backwards")
                val spanStart = s.getSpanStart(it)
                val spanEnd = s.getSpanEnd(it)
                if (spanStart == spanEnd) s.insert(spanEnd, " ")
                s.removeSpan(it)
                if (it is QuoteSpan) s.setSpan(it, spanStart, spanEnd - 1, s.getSpanFlags(it))
                if (s.isPreviousCharLineBreak(s.lastIndex) && (it is KnifeBulletSpan || it is NumberedMarginSpan)) {
                    //two ore more line breaks, don't add additional bullet spans
                    Timber.e("two ore more line breaks")
                } else if ((it is KnifeBulletSpan || it is NumberedMarginSpan) && s.isEndOfLine(mEditText.selectionStart)) {
                    s.setSpan(it, spanStart, spanEnd - 1, s.getSpanFlags(it))

                    Timber.e("adding  additional $it span mEditText.selectionStart = ${mEditText.selectionStart}" +
                            "s.length = ${s.length} ")
                    val newSpan = when (it) {
                        is KnifeBulletSpan -> KnifeBulletSpan(it.bulletColor, it.bulletRadius, it.bulletGapWidth)
                        is NumberedMarginSpan -> NumberedMarginSpan(it.leadWidth, it.gapWidth, it.index + 1)
                        else -> Any()
                    }
                    s.setSpan(newSpan,
                            mEditText.selectionStart,
                            s.length,
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

    private fun isLastEditAppendedSmth() = mLastText.length < mEditText.text.length

    override fun onSelectionChanged(start: Int, end: Int) {
        val currentState = state ?: return

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
        /*  Timber.e("shouldShowKeyboard ${state?.textPart}")
          if (!mEditText.isFocused) mEditText.requestFocus()
          mEditText.postDelayed({
              Timber.e("showing keyboard")
              mEditText.setSelection(getStartSelection(), getEndSelection())
              mEditText.requestFocus()
              val inputMethodManager = mEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
              inputMethodManager.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT)

          }, 50)*/
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