package io.golos.golos.screens.editor

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.text.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import io.golos.golos.R

data class EditorTitleState(var title: CharSequence = "",
                            val isTitleEditable: Boolean = false,
                            val onTitleChanges: (CharSequence) -> Unit = {},
                            val onEnter: () -> Unit = {},
                            val subtitle: CharSequence? = null,
                            val isHidden: Boolean = false)

class EditorTitle : FrameLayout, TextWatcher, InputFilter {

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

    private var mTitleEt: EditText
    private var mSubtitleText: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.v_editor_title, this)
        mTitleEt = findViewById(R.id.title_et)
        mSubtitleText = findViewById(R.id.subtitle_text)
        mTitleEt.filters = arrayOf<InputFilter>(this, InputFilter.LengthFilter(255))
    }

    var state: EditorTitleState = EditorTitleState()
        set(value) {
            if (field == value) {
                return
            }
            field = value
            mTitleEt.isEnabled = field.isTitleEditable
            if (mTitleEt.text.toString() != value.title.toString()) mTitleEt.setText(value.title.toString())
            mSubtitleText.text = field.subtitle
            mSubtitleText.visibility = if (mSubtitleText.text.isEmpty()) View.GONE else View.VISIBLE
            mTitleEt.removeTextChangedListener(this)
            mTitleEt.addTextChangedListener(this)
            if (value.isHidden) {
                this.visibility = View.GONE
            } else {
                this.visibility = View.VISIBLE
            }
            mTitleEt.setSelection(field.title.length)
        }

    override fun afterTextChanged(s: Editable?) {
        state = EditorTitleState(s
                ?: "", state.isTitleEditable, state.onTitleChanges, state.onEnter, state.subtitle)
        state.onTitleChanges.invoke(state.title)
    }


    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence? {
        source ?: return null
        if (!source.contains('\n')) return null
        var positionOfLineBreak = source.indexOf('\n')
        val ssb = SpannableStringBuilder.valueOf(source)
        while (positionOfLineBreak > -1) {
            ssb.delete(positionOfLineBreak, positionOfLineBreak + 1)
            positionOfLineBreak = ssb.indexOf('\n')
        }
        state.onEnter.invoke()
        return ssb
    }
}

