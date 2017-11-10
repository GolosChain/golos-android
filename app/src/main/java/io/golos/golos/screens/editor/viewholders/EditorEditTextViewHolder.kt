package io.golos.golos.screens.editor.viewholders

import android.support.annotation.LayoutRes
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import io.golos.golos.R
import io.golos.golos.screens.GolosViewHolder
import io.golos.golos.screens.editor.EditorAdapterTextPart
import timber.log.Timber
import timber.log.Timber.i

class EditorEditTextViewHolder(@LayoutRes res: Int, parent: ViewGroup) : GolosViewHolder(res, parent), TextWatcher, View.OnFocusChangeListener {
    private var mEditText: EditText = itemView.findViewById(R.id.et)

    init {
        mEditText.addTextChangedListener(this)
    }

    var state = EditorAdapterTextPart()
        set(value) {
            field = value
            if (mEditText.text.toString() != value.text.toString()) {
                mEditText.setText(value.text)
            }
            if (field.currentCursorPosition != null && field.currentCursorPosition != mEditText.selectionStart) {
                mEditText.setSelection(field.currentCursorPosition!!)
            } else if (field.currentCursorPosition == null) mEditText.isSelected = false
            if (mEditText.onFocusChangeListener != this) {
                mEditText.onFocusChangeListener = this
            }
        }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (v != null && v == mEditText) {
            state = EditorAdapterTextPart(state.id, mEditText.text, null, state.onFocusChanged, state.onNewText)
            state.onFocusChanged(hasFocus)
        }
    }

    override fun afterTextChanged(s: Editable?) {
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        state = EditorAdapterTextPart(state.id, s ?: "", start + count, state.onFocusChanged, state.onNewText)
        state.onNewText(state.id, state.text)
    }
}