package io.golos.golos.screens.editor

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import android.text.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.screens.story.adapters.StoryAdapter
import io.golos.golos.utils.setViewGone
import io.golos.golos.utils.setViewVisible

data class EditorTitleState(val type: EditorTitle,
                            val onTitleChanges: (CharSequence) -> Unit = {},
                            val onEnter: () -> Unit = {})

class EditorTitleView : FrameLayout, TextWatcher, InputFilter {

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

    private val mTitleEt: EditText
    private val mSubtitleText: TextView
    private val mCutView: View
    private val mFoldButton: View
    private val mRecycler: androidx.recyclerview.widget.RecyclerView
    private val mAdapter: StoryAdapter

    init {
        LayoutInflater.from(context).inflate(R.layout.v_editor_title, this)
        mTitleEt = findViewById(R.id.title_et)
        mSubtitleText = findViewById(R.id.subtitle_text)
        mCutView = findViewById(R.id.cut)
        mFoldButton = findViewById(R.id.fold_btn)
        mRecycler = findViewById(R.id.title_recycler)
        mAdapter = StoryAdapter()
        mRecycler.adapter = mAdapter
        mTitleEt.filters = arrayOf(this, InputFilter.LengthFilter(255))
        mTitleEt.addTextChangedListener(this)
    }

    var state: EditorTitleState? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            val newState = field ?: return
            val type = newState.type
            when (type) {
                is PostEditorTitle -> {
                    mCutView.setViewGone()
                    mSubtitleText.setViewGone()
                    mTitleEt.setViewVisible()
                    mTitleEt.isEnabled = type.title.isEditable
                    if (type.title.hintId != null) mTitleEt.hint = resources.getString(type.title.hintId)
                    mTitleEt.setTextKeepState(type.title.text)

                }
                is RootCommentEditorTitle -> {
                    mCutView.setViewGone()
                    mSubtitleText.setViewVisible()
                    mTitleEt.setViewVisible()
                    mTitleEt.isEnabled = type.title.isEditable
                    if (type.title.hintId != null) mTitleEt.hint = resources.getString(type.title.hintId)
                    mTitleEt.setText(type.title.text)

                    mSubtitleText.text = type.subtitleText
                }
                is AnswerOnCommentEditorTitle -> {
                    mCutView.setViewVisible()
                    mSubtitleText.setViewGone()
                    mTitleEt.setViewGone()
                    mAdapter.items = type.parentComment
                }
            }
        }

    override fun afterTextChanged(s: Editable?) {
        state?.onTitleChanges?.invoke(s ?: return)
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
        state?.onEnter?.invoke()
        return ssb
    }
}

