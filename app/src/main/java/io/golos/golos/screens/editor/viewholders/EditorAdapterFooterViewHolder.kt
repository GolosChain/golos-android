package io.golos.golos.screens.editor.viewholders

import android.support.annotation.LayoutRes
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.utils.StringValidator
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.screens.editor.EditorAdapterFooter

class EditorAdapterFooterViewHolder(@LayoutRes res: Int, parent: ViewGroup) : GolosViewHolder(res, parent), TextWatcher {
    private var mTagsLayout: ViewGroup = itemView.findViewById(R.id.tags_lo)
    private var mAddBtn: Button = itemView.findViewById(R.id.add_btn)
    private var mTextInputLo: TextInputLayout = itemView.findViewById(R.id.text_input_lo)
    private var mTagsEt: EditText
    private var mAddTagsText: TextView = itemView.findViewById(R.id.add_tags_label)
    private var mValidator: StringValidator = object : StringValidator {
        override fun validate(input: String): Pair<Boolean, String> = Pair(true, "")
    }
    private var mTagsText = ""

    var state: EditorAdapterFooter = EditorAdapterFooter()
        set(value) {
            val old = EditorAdapterFooter(value.showTagsEditor, value.tagsValidator, value.tags)
            field = value
            value.tagsValidator?.let { mValidator = field.tagsValidator as StringValidator }
            if (value.showTagsEditor) {
                mTagsLayout.visibility = View.VISIBLE
                mAddTagsText.visibility = View.VISIBLE
                mTagsEt.visibility = View.VISIBLE
            } else {
                mTagsLayout.visibility = View.GONE
                mAddTagsText.visibility = View.GONE
                mTagsEt.visibility = View.GONE
            }
            mTagsLayout.removeView(mAddBtn)
            value.tags.forEachIndexed { index, s ->
                if (s.isNotEmpty()) {
                    if (index > mTagsLayout.childCount - 1) {
                        val btn = LayoutInflater.from(itemView.context).inflate(R.layout.v_tag_button, mTagsLayout, false) as Button
                        btn.text = s
                        mTagsLayout.addView(btn)
                    } else {
                        if (mTagsLayout.getChildAt(index) is Button) {
                            val btn = mTagsLayout.getChildAt(index) as Button
                            if (btn != mAddBtn) btn.text
                        }
                    }
                }
            }
            mTagsLayout.addView(mAddBtn)
            val tagsText = value.tags.joinToString(" ")
            if (mTagsEt.text.toString() != tagsText) {
                mTagsEt.setText(tagsText)
                mTagsEt.post({ mTagsEt.setSelection(tagsText.length) })
            }
        }

    override fun afterTextChanged(s: Editable?) {
        mTagsText = s.toString()
        val validatorOut = mValidator.validate(mTagsText)
        s?.let { mTextInputLo.error = if (s.isNotEmpty()) validatorOut.second else "" }
    }

    init {
        mTagsEt = mTextInputLo.findViewById(R.id.tags_et)
        mTagsEt.removeTextChangedListener(this)
        mTagsEt.addTextChangedListener(this)
        mAddBtn.setOnClickListener {
            val validatorOut = mValidator.validate(mTagsText)
            if (validatorOut.first) {
                val tags = ArrayList(mTagsText.trim().replace(Regex("\\s+"), " ").split(" ").map { it.trim() })
                state = EditorAdapterFooter(state.showTagsEditor, state.tagsValidator, tags)
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }
}