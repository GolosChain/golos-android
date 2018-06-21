package io.golos.golos.screens.editor.viewholders

import android.content.Context
import android.support.annotation.LayoutRes
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import io.golos.golos.R
import io.golos.golos.screens.editor.EditorAdapterTextPart
import io.golos.golos.screens.editor.EditorPart
import io.golos.golos.screens.editor.compareGolosSpannables
import io.golos.golos.screens.editor.knife.KnifeText
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.screens.widgets.SelectionAwareEditText
import timber.log.Timber
import java.util.*


class EditorEditTextViewHolder(@LayoutRes res: Int, parent: ViewGroup) :
        GolosViewHolder(res, parent), TextWatcher, View.OnFocusChangeListener, SelectionAwareEditText.SelectionListener {
    private var mEditText: KnifeText = itemView.findViewById(R.id.et)

    init {
        mEditText.setSelectionListener(this)
        mEditText.addTextChangedListener(this)
    }

    var state: EditorAdapterTextPart? = null
        set(value) {
            if (value == null) return
            Timber.e("setting new state ${state?.textPart}")
            mEditText.removeTextChangedListener(this)
            mEditText.setSelectionListener(null)

            var textChanged = false

            val textPart = value.textPart
            if (!compareGolosSpannables(textPart.text, mEditText.text)) {
                Timber.e("setting new text \nold = ${mEditText.text}" +
                        " new = ${textPart}")
                mEditText.setTextKeepState(textPart.text)
                textChanged = true
                val start = value.textPart.startPointer
                val end = value.textPart.endPointer
                 mEditText.post {
                     Timber.e("setting new selection state = ${value.textPart}\n start = ${start} " +
                             "end = ${end}")
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
            /* if (mEditText.isFocused) {

                 val modifiers = value.modifiers
                 val selectionStart = mEditText.selectionStart
                 val selectionEnd = mEditText.selectionEnd
                 val oldHash = mEditText.text.hashCode()
                 Timber.e("edittext is focused ${value.textPart.text}\n" +
                         "selectionStart = $selectionStart selectioneEnd = $selectionEnd")
                 if (modifiers.contains(EditorTextModifier.QUOTATION_MARKS)) {
                     if (selectionEnd == selectionEnd) mEditText.text.insert(selectionEnd, "\"\"")
                     else {
                         mEditText.text.insert(selectionStart, "\"")
                         mEditText.text.insert(selectionEnd, "\"")
                     }
                 } else {
                     if (mEditText.text.isNotEmpty() && selectionStart != 0 && mEditText.text[selectionStart - 1] == '"') {
                         mEditText.text.removeRange(selectionStart - 1, selectionStart)
                         Timber.e("deleting pre quotation marj")
                     }
                     if (mEditText.text.isNotEmpty() &&
                             selectionEnd != 0 &&
                             mEditText.text[selectionEnd - 1] == '"') {
                         mEditText.text.removeRange(selectionEnd - 1, selectionEnd)
                         Timber.e("deleting post quotation marj")
                     }
                 }

                 if (!modifiers.contains(EditorTextModifier.LINK)) {
                     Timber.e("unlinking")
                     mEditText.link(null)
                 }

                 val newHash = mEditText.text.hashCode()
                 if (oldHash != newHash) {
                     mEditText.post({
                         Timber.e("modificators changed ")

                         afterTextChanged(mEditText.text)
                         state?.onFocusChanged?.invoke(this, true)
                     })
                 }
             }*/
            Timber.e("end on bind")
            mEditText.addTextChangedListener(this)
            mEditText.setSelectionListener(this)
            field = value
        }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {

        Timber.e("onFocusChange")
        if (v != null && v == mEditText) {
            val currentState = state ?: return

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
        Timber.e("afterTextChanged ${Arrays.toString(s.getSpans(0, mEditText.selectionEnd, Any::class.java))}")

        if (mEditText.selectionStart > currentState.textPart.startPointer) currentState.textPart.startPointer = mEditText.selectionStart
        if (mEditText.selectionEnd > currentState.textPart.endPointer) currentState.textPart.endPointer = mEditText.selectionEnd
        if (compareGolosSpannables(s, currentState.textPart.text)) {
            Timber.e("spannable equal")
            currentState.onNewText.invoke(this, state?.textPart ?: return)
            return
        }
        val newState = currentState.textPart.setText(s)

        state = EditorAdapterTextPart(newState,
                currentState.onFocusChanged,
                currentState.onNewText,
                currentState.onCursorChange,
                currentState.showHint,
                currentState.modifiers)
        currentState.onNewText.invoke(this, state?.textPart ?: return)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

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