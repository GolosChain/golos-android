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
            val textPart = value.textPart
            if (!compareGolosSpannables(textPart.text, mEditText.text)) {
                Timber.e("setting new text \nold = ${mEditText.text}" +
                        " new = ${textPart.text}")
                mEditText.removeTextChangedListener(this)
                mEditText.text = textPart.text
                mEditText.addTextChangedListener(this)
            }
            if (textPart.isFocused()) {
                if (textPart.startPointer != mEditText.selectionStart || textPart.endPointer != mEditText.selectionEnd) {
                    val selectionstart = if (textPart.startPointer > textPart.text.length) textPart.text.length else textPart.startPointer
                    val selectionEnd = if (textPart.endPointer > textPart.text.length) textPart.text.length else textPart.endPointer
                    mEditText.setSelection(if (selectionstart == -1) textPart.text.length else selectionstart,
                            if (selectionEnd == -1) textPart.text.length else selectionEnd)
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
            if (mEditText.isFocused) {
                Timber.e("edittext is focused ${value.textPart.text}")
            }
            field = value
        }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
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

        if (mEditText.selectionStart > currentState.textPart.startPointer) currentState.textPart.startPointer = mEditText.selectionStart
        if (mEditText.selectionEnd > currentState.textPart.endPointer) currentState.textPart.endPointer = mEditText.selectionEnd
        if (compareGolosSpannables(s ?: return, currentState.textPart.text)) {
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
        val part = state?.textPart ?: return
        var selection = if (part.startPointer > part.endPointer) part.startPointer else part.endPointer
        if (selection == -1) selection = part.text.length
        mEditText.requestFocus()
        mEditText.postDelayed({
            mEditText.setSelection(selection, selection)
            mEditText.requestFocus()
            val inputMethodManager = mEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT)

        }, 50)
    }
}