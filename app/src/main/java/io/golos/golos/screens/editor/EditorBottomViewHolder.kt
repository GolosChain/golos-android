package io.golos.golos.screens.editor

import android.view.View
import io.golos.golos.R
import io.golos.golos.screens.editor.views.CheckableButton
import java.util.*


data class ButtonState(val type: EditorTextModifier, val checked: Boolean)

class EditorBottomViewHolder(activity: EditorActivity) : View.OnClickListener {

    interface BottomButtonClickListener {
        fun onClick(clickedButton: EditorTextModifier, allButtons: Set<EditorTextModifier>)
    }

    private val mInsertImageBtn = activity.findViewById<View>(R.id.btn_insert_image)
    private val mLinkButton = activity.findViewById<CheckableButton>(R.id.btn_link)
    private val mBoldButton = activity.findViewById<View>(R.id.btn_bold)
    private val mListBullet = activity.findViewById<CheckableButton>(R.id.btn_list_bullet)
    private val mListNumbered = activity.findViewById<CheckableButton>(R.id.btn_list_numbered)
    private val mQuoteBtn = activity.findViewById<View>(R.id.btn_quote)
    private val mQuotationsBtn = activity.findViewById<CheckableButton>(R.id.btn_quotations)
    private val mTitleBtn = activity.findViewById<View>(R.id.btn_title)

    private val mAll = createState()
    var bottomButtonClickListener: BottomButtonClickListener? = null

    fun getSelectedModifier() = mAll.toSet()


    init {
        mInsertImageBtn.setOnClickListener(this)
        mLinkButton.setOnClickListener(this)
        mBoldButton.setOnClickListener(this)
        mListBullet.setOnClickListener(this)
        mListNumbered.setOnClickListener(this)
        mQuoteBtn.setOnClickListener(this)
        mQuotationsBtn.setOnClickListener(this)
        mTitleBtn.setOnClickListener(this)
        mLinkButton.isSelectibilityTurnedOn = false
        mQuotationsBtn.isSelectibilityTurnedOn = false
    }

    private fun createState(): EnumSet<EditorTextModifier> {
        return EnumSet.noneOf(EditorTextModifier::class.java)
    }

    private fun getButtonType(button: EditorTextModifier) = when (button) {
        EditorTextModifier.INSERT_IMAGE -> mInsertImageBtn
        EditorTextModifier.LINK -> mLinkButton
        EditorTextModifier.LIST_BULLET -> mListBullet
        EditorTextModifier.LIST_NUMBERED -> mListNumbered
        EditorTextModifier.QUOTATION -> mQuoteBtn
        EditorTextModifier.QUOTATION_MARKS -> mQuotationsBtn
        EditorTextModifier.TITLE -> mTitleBtn
        EditorTextModifier.STYLE_BOLD -> mBoldButton
    }

    private fun getViewType(view: View) = when (view) {
        mInsertImageBtn -> EditorTextModifier.INSERT_IMAGE
        mLinkButton -> EditorTextModifier.LINK
        mListBullet -> EditorTextModifier.LIST_BULLET
        mListNumbered -> EditorTextModifier.LIST_NUMBERED
        mQuoteBtn -> EditorTextModifier.QUOTATION
        mQuotationsBtn -> EditorTextModifier.QUOTATION_MARKS
        mTitleBtn -> EditorTextModifier.TITLE
        mBoldButton -> EditorTextModifier.STYLE_BOLD
        else -> throw IllegalArgumentException("not from this view hierarchy")
    }


    override fun onClick(v: View?) {
        if (v == null) return
        val type = getViewType(v)
        val isSelected = v.isSelected

        if (!isSelected) mAll.remove(type)
        else mAll.add(type)


        if (isSelected && type == EditorTextModifier.LIST_BULLET) {
            if (mListNumbered.isSelected) {
                mListNumbered.isSelected = false
                mAll.remove(EditorTextModifier.LIST_NUMBERED)
            }
        } else if (isSelected && type == EditorTextModifier.LIST_NUMBERED) {
            if (mListBullet.isSelected) {
                mListBullet.isSelected = false
                mAll.remove(EditorTextModifier.LIST_BULLET)
            }
        }
        bottomButtonClickListener?.onClick(type, mAll)
    }

    fun performClick(button: EditorTextModifier) = getButtonType(button).performClick()

    fun setSelected(button: EditorTextModifier, isSelected: Boolean) {
        if (button == EditorTextModifier.INSERT_IMAGE) return//insert image cannot be selected

        val view = getButtonType(button)
        view.isSelected = isSelected
        if (isSelected) mAll.add(button)
        else mAll.remove(button)
    }

    fun unSelectAll() {
        EditorTextModifier.values().forEach { setSelected(it, false) }

    }
}