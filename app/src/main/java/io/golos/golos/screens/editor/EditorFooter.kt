package io.golos.golos.screens.editor

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.utils.StringValidator

data class EditorFooterState(val showTagsEditor: Boolean = false,
                             val tagsValidator: io.golos.golos.utils.StringValidator? = null,
                             val tags: ArrayList<String> = ArrayList(),
                             val tagsListener: EditorFooter.TagsListener? = null)

class EditorFooter : FrameLayout, TextWatcher {
    private var mTagsLayout: ViewGroup
    private var mAddBtn: View
    private var mTextInputLo: TextInputLayout
    private var mTagsEt: EditText
    private var mAddTagsText: TextView
    private var mValidator: StringValidator = object : StringValidator {
        override fun validate(input: String): Pair<Boolean, String> = Pair(true, "")
    }
    private var mTagsText = ""

    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0)
            : super(context, attrs, defStyleAttr)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
            context: Context,
            attrs: AttributeSet?,
            defStyleAttr: Int,
            defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        LayoutInflater.from(context).inflate(R.layout.v_editor_footer, this)
        mTagsLayout = findViewById(R.id.tags_lo)
        mAddBtn = findViewById(R.id.add_btn)
        mTextInputLo = findViewById(R.id.text_input_lo)
        mTagsEt = findViewById(R.id.tags_et)
        mAddTagsText = findViewById(R.id.add_tags_label)
        mTagsEt.removeTextChangedListener(this)
        mTagsEt.addTextChangedListener(this)

    }

    var state: EditorFooterState = EditorFooterState()
        set(value) {
            field = value
            value.tagsValidator?.let { mValidator = field.tagsValidator as StringValidator }
            if (value.showTagsEditor) {
                mTagsLayout.visibility = View.VISIBLE
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
                        val btn = LayoutInflater.from(context).inflate(R.layout.v_editor_tag_button, mTagsLayout, false) as Button
                        btn.text = s
                        mTagsLayout.addView(btn)
                    } else {

                        if (mTagsLayout.getChildAt(index) is Button) {
                            val btn = mTagsLayout.getChildAt(index) as Button
                            if (btn != mAddBtn) {
                                btn.text = s
                            }
                        }
                    }
                }
            }
            if (mTagsLayout.childCount > value.tags.size) {
                mTagsLayout.removeViews(value.tags.size, mTagsLayout.childCount - value.tags.size)
            }
            mTagsLayout.addView(mAddBtn)

            val tagsText = value.tags.joinToString(" ")
            if (mTagsEt.text.toString() != tagsText) {
                mTagsEt.setText(tagsText)
                mTagsEt.post({ mTagsEt.setSelection(tagsText.length) })
            }
            mAddBtn.setOnClickListener {
                val validatorOut = mValidator.validate(mTagsText)
                if (validatorOut.first) {
                    val tags = ArrayList(mTagsText.trim().replace(Regex("\\s+"), " ").split(" ").map { it.trim() })
                    state = EditorFooterState(state.showTagsEditor, state.tagsValidator, tags)
                    state.tagsListener?.onTagsSubmit(tags)
                }
            }
        }

    override fun afterTextChanged(s: Editable?) {
        mTagsText = s.toString()
        val validatorOut = mValidator.validate(mTagsText)
        s?.let { mTextInputLo.error = if (s.isNotEmpty()) validatorOut.second else "" }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    interface TagsListener {
        fun onTagsSubmit(tags: List<String>)
    }
}

