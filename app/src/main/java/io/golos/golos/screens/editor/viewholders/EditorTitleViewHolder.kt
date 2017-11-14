package io.golos.golos.screens.editor.viewholders

import android.support.annotation.LayoutRes
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.screens.editor.EditorAdapterHeader

class EditorTitleViewHolder(@LayoutRes res: Int, parent: ViewGroup) : GolosViewHolder(res, parent), TextWatcher {
    private var mTitleEt: EditText = itemView.findViewById(R.id.title_et)
    private var mSubtitleText: TextView = itemView.findViewById(R.id.subtitle_text)
    var state: EditorAdapterHeader = EditorAdapterHeader()
        set(value) {
            field = value
            mTitleEt.isEnabled = field.isTitleEditable
            if (mTitleEt.text.toString() != field.title.toString().toUpperCase()) {
                mTitleEt.setText(field.title.toString().toUpperCase())
            }
            mSubtitleText.text = field.subtitle
            mSubtitleText.visibility = if (mSubtitleText.text.isEmpty()) View.GONE else View.VISIBLE
            mTitleEt.removeTextChangedListener(this)
            mTitleEt.addTextChangedListener(this)
        }

    override fun afterTextChanged(s: Editable?) {
        state = EditorAdapterHeader(s ?: "", state.isTitleEditable, state.onTitleChanges, state.subtitle)
        state.onTitleChanges.invoke(state.title)
    }


    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }
}