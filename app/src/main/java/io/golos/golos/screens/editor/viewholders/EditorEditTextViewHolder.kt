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
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.screens.widgets.SelectionAwareEditText


class EditorEditTextViewHolder(@LayoutRes res: Int, parent: ViewGroup) :
        GolosViewHolder(res, parent), TextWatcher, View.OnFocusChangeListener, SelectionAwareEditText.SelectionListener {
    var mEditText: SelectionAwareEditText = itemView.findViewById(R.id.et)

    init {
        mEditText.addTextChangedListener(this)
        mEditText.setSelectionListener(this)
    }

    var state: EditorAdapterTextPart? = null
        set(value) {
            if (mEditText.text.toString() != value?.textPart?.text) {
                mEditText.setText(value?.textPart?.text)
            }
            if (value?.textPart?.pointerPosition != null && value.textPart.pointerPosition != mEditText.selectionEnd) {
                mEditText.setSelection(value.textPart.pointerPosition!!)

            } else if (value?.textPart?.pointerPosition == null) mEditText.isSelected = false
            if (mEditText.onFocusChangeListener != this) {
                mEditText.onFocusChangeListener = this
            }
            field = value
        }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (v != null && v == mEditText) {
            state?.textPart?.pointerPosition = if (hasFocus) mEditText.selectionEnd else null
            state?.let {
                it.onFocusChanged(this, hasFocus)
            }
        }
    }

    override fun afterTextChanged(s: Editable?) {
        if (state == null) return
        state?.textPart?.text = s?.toString() ?: ""
        state?.textPart?.pointerPosition = mEditText.selectionEnd
        state!!.onNewText(this, state!!.textPart)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun onSelectionChanged(end: Int) {
        state?.textPart?.pointerPosition = end
        state?.let {
            it.onNewText(this, state!!.textPart)
        }
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