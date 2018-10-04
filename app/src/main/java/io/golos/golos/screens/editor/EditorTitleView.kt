package io.golos.golos.screens.editor

import android.annotation.TargetApi
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.golos.golos.R
import io.golos.golos.screens.story.adapters.OnImagePopup
import io.golos.golos.screens.story.adapters.StoryAdapter
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.utils.hoursElapsedFromTimeStamp
import io.golos.golos.utils.setFullAnimationToViewGroup
import io.golos.golos.utils.setViewGone
import io.golos.golos.utils.setViewVisible

data class EditorTitleState(val type: EditorTitle,
                            val onTitleChanges: (String) -> Unit = {},
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
    private val mCutView: ConstraintLayout
    private val mFoldButton: ImageButton
    private val mRecycler: androidx.recyclerview.widget.RecyclerView
    private val mAdapter: StoryAdapter
    private val mShade: View
    private val mAuthorTv: TextView
    private val mCommentTimeStamp: TextView


    init {
        LayoutInflater.from(context).inflate(R.layout.v_editor_title, this)

        mTitleEt = findViewById(R.id.title_et)
        mSubtitleText = findViewById(R.id.subtitle_text)
        mCutView = findViewById(R.id.cut)
        mFoldButton = findViewById(R.id.fold_btn)
        mRecycler = findViewById(R.id.title_recycler)
        mShade = findViewById(R.id.shade)
        mAdapter = StoryAdapter(resources.getDimension(R.dimen.material_big).toInt(), object : OnImagePopup {
            override fun onLinkSave(row: ImageRow) {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText(row.src, row.src)
            }

            override fun onImageSave(row: ImageRow) {
                (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(DownloadManager.Request(Uri.parse(row.src)).apply {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                })
            }
        })
        mCommentTimeStamp = findViewById(R.id.time_elapsed)
        mAuthorTv = findViewById(R.id.author_tv)
        mCutView.setFullAnimationToViewGroup()
        mRecycler.adapter = mAdapter
        mRecycler.setHasFixedSize(false)


        mTitleEt.filters = arrayOf(this, InputFilter.LengthFilter(255))
        mTitleEt.addTextChangedListener(this)
        mFoldButton.tag = false
        mFoldButton.setOnClickListener {
            val isUnfolded = (mFoldButton.tag as? Boolean) ?: return@setOnClickListener

            if (isUnfolded) {
                foldComment()
            } else {
                unfoldComment()
            }
        }
        mRecycler.isScrollContainer = true
    }

    private fun foldComment() {
        val recyclerLayoutParams = (mRecycler.layoutParams as ConstraintLayout.LayoutParams)
        val foldButtonLo = (mFoldButton.layoutParams as ConstraintLayout.LayoutParams)
        recyclerLayoutParams.constrainedHeight = true
        mFoldButton.tag = false
        mFoldButton.setImageResource(R.drawable.ic_chevron_down_12dp_gray_editor)
        foldButtonLo.topToBottom = 0
        foldButtonLo.bottomToBottom = mRecycler.id
        mShade.setViewVisible()
        mFoldButton.requestLayout()
    }

    private fun unfoldComment() {
        val recyclerLayoutParams = (mRecycler.layoutParams as ConstraintLayout.LayoutParams)
        val foldButtonLo = (mFoldButton.layoutParams as ConstraintLayout.LayoutParams)
        recyclerLayoutParams.constrainedHeight = false
        mFoldButton.tag = true
        mFoldButton.setImageResource(R.drawable.ic_chevron_up_12dp_gray_editor)
        mShade.setViewGone()
        foldButtonLo.bottomToBottom = 0
        foldButtonLo.topToBottom = mRecycler.id
        mFoldButton.requestLayout()
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
                    val currentText = mTitleEt.text.toString()
                    if (currentText != type.title.text) mTitleEt.setTextKeepState(type.title.text)
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
                    if (type.parentComment.size == 1 && type.parentComment[0] is TextRow && (type.parentComment[0] as TextRow).text.length < 300) {
                        mFoldButton.setViewGone()
                        mShade.setViewGone()
                        (mRecycler.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMaxHeight = 0
                        mRecycler.requestLayout()
                    } else {
                        foldComment()
                    }
                    mAuthorTv.text = type.author
                    val hoursAgo = type.timeStamp.hoursElapsedFromTimeStamp()
                    when {
                        hoursAgo == 0 -> mCommentTimeStamp.text = resources.getString(R.string.less_then_hour_ago)
                        hoursAgo < 24 -> mCommentTimeStamp.text = "$hoursAgo ${resources.getQuantityString(R.plurals.hours, hoursAgo.toInt())} ${resources.getString(R.string.ago)}"
                        else -> {
                            val daysAgo = hoursAgo / 24
                            mCommentTimeStamp.text = "$daysAgo ${resources.getQuantityString(R.plurals.days, daysAgo.toInt())} ${resources.getString(R.string.ago)}"
                        }
                    }
                    mAdapter.items = type.parentComment

                }
            }
        }

    override fun afterTextChanged(s: Editable?) {
        state?.onTitleChanges?.invoke(s?.toString() ?: return)
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

