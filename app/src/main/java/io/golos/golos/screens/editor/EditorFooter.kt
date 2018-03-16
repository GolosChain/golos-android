package io.golos.golos.screens.editor

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.utils.StringValidator
import io.golos.golos.utils.nextInt


data class EditorFooterState(val showTagsEditor: Boolean = false,
                             val tagsValidator: io.golos.golos.utils.StringValidator? = null,
                             val tags: ArrayList<String> = ArrayList(),
                             val tagsListener: EditorFooter.TagsListener? = null)

class EditorFooter : FrameLayout {
    private var mTagsLayout: ViewGroup
    private var mAddBtn: View
    private var mErrorTv: TextView
    private var mAddTagsText: TextView
    private var mValidator: StringValidator = object : StringValidator {
        override fun validate(input: String): Pair<Boolean, String> = Pair(true, "")
    }

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
        mErrorTv = findViewById(R.id.error_text)
        mAddTagsText = findViewById(R.id.add_tags_label)
    }

    var state: EditorFooterState = EditorFooterState()
        set(value) {
            if (field == value) {
                return
            }
            field = value
            value.tagsValidator?.let { mValidator = field.tagsValidator as StringValidator }
            if (value.showTagsEditor) {
                mTagsLayout.visibility = View.VISIBLE
                mErrorTv.visibility = View.VISIBLE
            } else {
                mTagsLayout.visibility = View.GONE
                mAddTagsText.visibility = View.GONE
                mErrorTv.visibility = View.GONE
            }
            if (value.tags.size != (mTagsLayout.childCount - 1)) {
                mTagsLayout.removeView(mAddBtn)
                if (mTagsLayout.childCount > value.tags.size) {
                    mTagsLayout.removeViews(value.tags.size, mTagsLayout.childCount - value.tags.size)
                    (0 until mTagsLayout.childCount).forEach {
                        (mTagsLayout.getChildAt(it) as? EditText)?.setText(value.tags[it])
                    }
                } else {
                    (mTagsLayout.childCount until value.tags.size).forEach {
                        val newView = inflateNewEditText(value.tags[it])
                        mTagsLayout.addView(newView)
                    }
                    (0 until mTagsLayout.childCount).forEach {
                        val et = (mTagsLayout.getChildAt(it) as EditText)
                        if (et.text.toString() != value.tags[it]) {
                            et.setText(value.tags[it])
                        }
                    }
                }
                mTagsLayout.addView(mAddBtn)
            }

            if (!mAddBtn.hasOnClickListeners())
                mAddBtn.setOnClickListener {
                    if (mTagsLayout.childCount < 6) {
                        if (mTagsLayout.childCount == 1) {
                            mTagsLayout.removeView(mAddBtn)
                            val newView = inflateNewEditText()
                            mTagsLayout.addView(newView)
                            mTagsLayout.addView(mAddBtn)
                            newView.requestFocus()
                            mErrorTv.text = ""
                        } else {
                            val prelast = mTagsLayout.getChildAt(mTagsLayout.childCount - 2) as EditText
                            if (!prelast.text.isEmpty()) {
                                val validatorOut = mValidator.validate(prelast.text.toString())
                                if (validatorOut.first) {
                                    mTagsLayout.removeView(mAddBtn)
                                    val newView = inflateNewEditText()
                                    mTagsLayout.addView(newView)
                                    mTagsLayout.addView(mAddBtn)
                                    newView.requestFocus()
                                    mErrorTv.text = ""
                                } else {
                                    mErrorTv.text = validatorOut.second
                                }
                            }
                        }
                    } else {
                        mErrorTv.text = resources.getString(R.string.to_much_tags)
                    }
                }
        }

    private fun inflateNewEditText(startText: String? = null): EditText {
        val view = LayoutInflater.from(context).inflate(R.layout.v_editor_footer_tag_et, mTagsLayout, false) as EditText
        view.id = nextInt()
        if (startText != null) view.setText(startText)
        view.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null) onTextChanged(p0.toString())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0?.length == 0 && p1 == 0 && p3 == 0) {
                    onDeleteEt(view)
                }
            }
        })
        view.setOnEditorActionListener({ _, _, _ ->
            if (view.text.isNotEmpty()) {
                mAddBtn.callOnClick()
            }
            true
        })
        view.isFocusableInTouchMode = true
        return view
    }

    private fun onDeleteEt(et: EditText) {
        mTagsLayout.removeView(et)
    }

    private fun onTextChanged(text: String) {
        val tagsList = (0 until mTagsLayout.childCount - 1)
                .map { (mTagsLayout.getChildAt(it) as EditText).text.toString() }
                .toList()
        state.tagsListener?.onTagsSubmit(tagsList)
    }


    interface TagsListener {
        fun onTagsSubmit(tags: List<String>)
    }
}

