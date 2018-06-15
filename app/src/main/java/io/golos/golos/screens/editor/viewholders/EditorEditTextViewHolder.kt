package io.golos.golos.screens.editor.viewholders

import android.content.Context
import android.support.annotation.LayoutRes
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import io.golos.golos.R
import io.golos.golos.screens.editor.EditorAdapterTextPart
import io.golos.golos.screens.editor.EditorPart
import io.golos.golos.screens.editor.knife.KnifeText
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.screens.widgets.SelectionAwareEditText


class EditorEditTextViewHolder(@LayoutRes res: Int, parent: ViewGroup) :
        GolosViewHolder(res, parent), TextWatcher, View.OnFocusChangeListener, SelectionAwareEditText.SelectionListener {
    private var mEditText: KnifeText = itemView.findViewById(R.id.et)

    init {
        mEditText.addTextChangedListener(this)
        mEditText.setSelectionListener(this)
    }

    var state: EditorAdapterTextPart? = null
        set(value) {
            if (value == null) return
            val textPart = value.textPart
            if (mEditText.text != textPart.text) {
                mEditText.setText(value.textPart.text)
            }
            if (textPart.isFocused()) {
                if (textPart.startPointer != mEditText.selectionStart || textPart.endPointer != mEditText.selectionEnd)
                    mEditText.setSelection(textPart.startPointer, textPart.endPointer)
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
            field = value
        }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (v != null && v == mEditText) {
            val currentState = state ?: return

            if (hasFocus) {
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

        state = EditorAdapterTextPart(currentState.textPart.setText(s
                ?: SpannableStringBuilder.valueOf("")),
                currentState.onFocusChanged,
                currentState.onNewText,
                currentState.showHint)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun onSelectionChanged(start: Int, end: Int) {
        val currentState = state ?: return
        currentState.textPart.startPointer = start
        currentState.textPart.endPointer = end
    }

    fun shouldShowKeyboard() {
        mEditText.postDelayed({
            if (mEditText.requestFocus()) {
                val inputMethodManager = mEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }, 400)
    }
}