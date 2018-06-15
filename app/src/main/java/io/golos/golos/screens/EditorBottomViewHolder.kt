package io.golos.golos.screens

import android.view.View
import io.golos.golos.R
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.utils.mutableMapOf
import timber.log.Timber

enum class EditorBottomButton {
    INSERT_IMAGE, ADD_LINK, LIST_BULLET, LIST_NUMBERED, QUOTATION, QUOTATION_MARKS, TITLE, STYLE_BOLD
}


data class ButtonState(val type: EditorBottomButton, val checked: Boolean)

class EditorBottomViewHolder(activity: EditorActivity) : View.OnClickListener {
    interface BottomButtonClickListener {
        fun onClick(clickedButton: ButtonState, allButtons: Map<EditorBottomButton, ButtonState>)
    }

    private val mInsertImageBtn = activity.findViewById<View>(R.id.btn_insert_image)
    private val mLinkButton = activity.findViewById<View>(R.id.btn_link)
    private val mBoldButton = activity.findViewById<View>(R.id.btn_bold)
    private val mListBullet = activity.findViewById<View>(R.id.btn_list_bullet)
    private val mListNumbered = activity.findViewById<View>(R.id.btn_list_numbered)
    private val mQuoteBtn = activity.findViewById<View>(R.id.btn_quote)
    private val mQuotationsBtn = activity.findViewById<View>(R.id.btn_quotations)
    private val mTitleBtn = activity.findViewById<View>(R.id.btn_title)

    private val mAll = createState()
    var bottomButtonClickListener: BottomButtonClickListener? = null


    init {
        mInsertImageBtn.setOnClickListener(this)
        mLinkButton.setOnClickListener(this)
        mBoldButton.setOnClickListener(this)
        mListBullet.setOnClickListener(this)
        mListNumbered.setOnClickListener(this)
        mQuoteBtn.setOnClickListener(this)
        mQuotationsBtn.setOnClickListener(this)
        mTitleBtn.setOnClickListener(this)

    }

    private fun createState(): MutableMap<EditorBottomButton, ButtonState> {
        return mutableMapOf(EditorBottomButton.values().map { it to ButtonState(it, false) })
    }

    private fun getButtonType(button: EditorBottomButton) = when (button) {
        EditorBottomButton.INSERT_IMAGE -> mInsertImageBtn
        EditorBottomButton.ADD_LINK -> mLinkButton
        EditorBottomButton.LIST_BULLET -> mListBullet
        EditorBottomButton.LIST_NUMBERED -> mListNumbered
        EditorBottomButton.QUOTATION -> mQuoteBtn
        EditorBottomButton.QUOTATION_MARKS -> mQuotationsBtn
        EditorBottomButton.TITLE -> mTitleBtn
        EditorBottomButton.STYLE_BOLD -> mBoldButton
    }

    private fun getViewType(view: View) = when (view) {
        mInsertImageBtn -> EditorBottomButton.INSERT_IMAGE
        mLinkButton -> EditorBottomButton.ADD_LINK
        mListBullet -> EditorBottomButton.LIST_BULLET
        mListNumbered -> EditorBottomButton.LIST_NUMBERED
        mQuoteBtn -> EditorBottomButton.QUOTATION
        mQuotationsBtn -> EditorBottomButton.QUOTATION_MARKS
        mTitleBtn -> EditorBottomButton.TITLE
        mBoldButton -> EditorBottomButton.STYLE_BOLD
        else -> throw IllegalArgumentException("not from this view hierarchy")
    }


    override fun onClick(v: View?) {
        if (v == null) return
        val state = ButtonState(getViewType(v), v.isSelected)
        mAll[state.type] = state
        bottomButtonClickListener?.onClick(state, mAll)
    }

    fun performClick(button: EditorBottomButton) = getButtonType(button).performClick()
}