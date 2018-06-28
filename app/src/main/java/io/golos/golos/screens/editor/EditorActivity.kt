package io.golos.golos.screens.editor

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.Selection
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import android.text.style.URLSpan
import android.widget.Button
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.golos.golos.R
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.editor.knife.KnifeBulletSpan
import io.golos.golos.screens.editor.knife.KnifeQuoteSpan
import io.golos.golos.screens.editor.knife.KnifeURLSpan
import io.golos.golos.screens.editor.knife.NumberedMarginSpan
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.widgets.dialogs.LinkDialogInterface
import io.golos.golos.screens.widgets.dialogs.SendLinkDialog
import io.golos.golos.utils.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream


/**
 *
 * **/
data class EditorMode(@JsonProperty("title")
                      val title: String = "",
                      @JsonProperty("subtitle")
                      val subtitle: String = "",
                      @JsonProperty("editorType")
                      val editorType: EditorActivity.EditorType,
                      @JsonProperty("rootStoryId")
                      val rootStoryId: Long? = null,
                      @JsonProperty("workingItemId")
                      val workingItemId: Long? = null,
                      @JsonProperty("storyFilter")
                      val storyFilter: StoryFilter? = null,
                      @JsonProperty("feedType")
                      val feedType: FeedType? = null)

class EditorActivity : GolosActivity(), EditorAdapterInteractions,
        EditorFooter.TagsListener,
        EditorBottomViewHolder.BottomButtonClickListener,
        LinkDialogInterface,
        Observer<EditorState> {
    enum class EditorType {
        CREATE_POST, CREATE_COMMENT, EDIT_POST, EDIT_COMMENT
    }

    private lateinit var mRecycler: RecyclerView
    private lateinit var mToolbar: Toolbar
    private lateinit var mAdapter: EditorAdapter
    private lateinit var mTitle: EditorTitle
    private lateinit var mFooter: EditorFooter
    private lateinit var mViewModel: EditorViewModel
    private lateinit var mSubmitBtn: Button
    private lateinit var mBottomButtons: EditorBottomViewHolder
    private var mMode: EditorMode? = null
    private var mProgressDialog: Dialog? = null
    private val mHandler = Handler()
    private var mSavedCursor: Pair<Int, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_editor)
        mRecycler = findViewById(R.id.recycler)
        mRecycler.layoutManager = MyLinearLayoutManager(this)
        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener({ finish() })
        mToolbar = findViewById(R.id.toolbar)
        mTitle = findViewById(R.id.title)
        mFooter = findViewById(R.id.footer)
        mSubmitBtn = findViewById(R.id.submit_btn)
        mAdapter = EditorAdapter(interactor = this)
        mRecycler.adapter = mAdapter
        mRecycler.isNestedScrollingEnabled = false
        (mRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        mBottomButtons = EditorBottomViewHolder(this)
        mBottomButtons.bottomButtonClickListener = this

        mViewModel = ViewModelProviders.of(this)[EditorViewModel::class.java]
        val mapper = ObjectMapper()
        mSubmitBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, getVectorDrawable(R.drawable.ic_send_blue_white_24dp), null)
        mapper.registerModule(KotlinModule())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        val modeString = intent.getStringExtra(MODE_TAG)
        if (modeString == null) finish()
        mMode = jacksonObjectMapper().readValue(modeString, EditorMode::class.java)

        if (mMode?.editorType == EditorType.CREATE_POST) {
            mSubmitBtn.text = getString(R.string.publish)
        } else if (mMode?.editorType == EditorType.CREATE_COMMENT) {
            if (mMode?.rootStoryId == mMode?.workingItemId) {
                mSubmitBtn.text = getString(R.string.to_comment)
            } else {
                mSubmitBtn.text = getString(R.string.answer)
            }
        } else if (mMode?.editorType == EditorType.EDIT_POST) {
            mSubmitBtn.text = getString(R.string.edit)
        }


        mViewModel.mode = mMode
        mViewModel.editorLiveData.observe(this, this)
        mTitle.state = EditorTitleState(mMode?.title
                ?: "", mMode?.editorType == EditorType.CREATE_POST || mMode?.editorType == EditorType.EDIT_POST,
                {
                    mViewModel.onTitleChanged(it)
                }, if (mMode?.editorType == EditorType.CREATE_COMMENT) mMode?.subtitle
                ?: "" else "",

                isHidden = isTitleHidden())

        mFooter.state = EditorFooterState(mMode?.editorType == EditorType.CREATE_POST || mMode?.editorType == EditorType.EDIT_POST,
                TagsStringValidator(object : StringSupplier {
                    override fun get(resId: Int, args: String?): String {
                        return getString(resId, args)
                    }
                }),
                ArrayList(),
                this)
        mToolbar.title = if (mMode?.editorType == EditorType.CREATE_POST || mMode?.editorType == EditorType.CREATE_COMMENT)
            resources.getString(R.string.text) else resources.getString(R.string.comment)


        mSubmitBtn.setOnClickListener({
            mViewModel.onSubmit()
            mRecycler.hideKeyboard()
        })
    }

    override fun onChanged(it: EditorState?) {
        if (it == null) return
        mAdapter.parts = ArrayList(it.parts)
        it.error?.let {
            if (it.localizedMessage != null) mRecycler.showSnackbar(it.localizedMessage)
            else if (it.nativeMessage != null) mRecycler.showSnackbar(it.nativeMessage)
        }
        if (it.isLoading) {
            if (mProgressDialog == null) mProgressDialog = showProgressDialog()
        } else {
            mProgressDialog?.let {
                it.dismiss()
                mProgressDialog = null
            }
        }
        if (mMode?.editorType == EditorType.CREATE_POST) {
            mTitle.state = EditorTitleState(it.title,
                    mTitle.state.isTitleEditable,
                    mTitle.state.onTitleChanges,
                    mTitle.state.subtitle,
                    mTitle.state.isHidden)
        }
        mFooter.state = EditorFooterState(mMode?.editorType == EditorType.CREATE_POST || mMode?.editorType == EditorType.EDIT_POST,
                mFooter.state.tagsValidator,
                it.tags.toArrayList(),
                mFooter.state.tagsListener)
        it.completeMessage?.let {
            mRecycler.showSnackbar(it)
            Handler().postDelayed({
                finish()
            }, 30)
        }
    }

    override fun onLinkSubmit(linkName: String, linkAddress: String) {
        Timber.e("onLinkSubmit linkName = $linkName linkAddress = $linkAddress")
        if (linkAddress.isNullOrEmpty()) return
        val linkAddress = if (linkAddress.matches(Regexps.anyImageLink)) linkAddress else formatUrl(linkAddress)

        val spannableString = SpannableStringBuilder.valueOf(linkName)
        spannableString.setSpan(KnifeURLSpan(linkAddress,
                getColorCompat(R.color.blue_light), true),
                0,
                linkName.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        val focusedPart = mViewModel.editorLiveData.value?.parts?.findLast { it.isFocused() } as? EditorTextPart
        val startPointer = focusedPart?.startPointer ?: 0
        val endPointer = focusedPart?.endPointer ?: 0

        focusedPart ?: return

        if (startPointer == endPointer) {
            if (focusedPart.text.isWithinWord(startPointer)) {
                val startOfWord = focusedPart.text.getStartOfWord(startPointer)
                val endOfWord = focusedPart.text.getEndOfWord(startPointer)
                if (!checkStartAndEnd(startOfWord, endOfWord)) return
                if ((endOfWord + 1) > focusedPart.text.length) return
                focusedPart.text.removeUrlSpans(startOfWord, endOfWord)
                focusedPart.text.setSpan(KnifeURLSpan(linkAddress,
                        getColorCompat(R.color.blue_light), true), startOfWord, endOfWord + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                mViewModel.onUserInput(EditorInputAction.InsertAction(
                        EditorTextPart(text = spannableString)))
            }
        } else {
            focusedPart.text.removeUrlSpans(startPointer, endPointer)
            focusedPart.text.setSpan(KnifeURLSpan(linkAddress,
                    getColorCompat(R.color.blue_light), true), startPointer, endPointer, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            mViewModel.onTextChanged(mViewModel.editorLiveData.value?.parts ?: return)
            validateBottomButtonSelections(mViewModel.editorLiveData.value?.parts
                    ?: return)
            mSavedCursor = null

        }
        mAdapter.onRequestFocus(mRecycler)
    }

    override fun onDismissLinkDialog() {
        mRecycler.isFocusable = true
        mAdapter.onRequestFocus(mRecycler)
        mSavedCursor = null
    }

    override fun onCursorChange(parts: List<EditorPart>) {
        Timber.e("onCursorChange mSavedCursor = $mSavedCursor")
        if (mSavedCursor != null && mSavedCursor!!.first > -1 && mSavedCursor!!.second > -1) {
            val focusedPart = parts.findLast { it.isFocused() } as? EditorTextPart
            if (focusedPart != null && mSavedCursor!!.second <= focusedPart.text.length) {
                //for unknown reason cursor
                // begin chaotically float in edit text, while edititng it inners, sh we fix it in place during edit
                Timber.e("changing selection")
                Selection.setSelection(
                        focusedPart.text, mSavedCursor!!.first,
                        mSavedCursor!!.second)
            }
        }
        validateBottomButtonSelections(parts)
    }

    private fun validateBottomButtonSelections(parts: List<EditorPart>) {
        Timber.e("validateBottomButtonSelections saved cursor = $mSavedCursor")
        mHandler.removeCallbacksAndMessages(null)//discard scheduled changes, only apply for fresh one selection
        val focusedPart = parts.findLast { it.isFocused() } as? EditorTextPart
        Timber.e("focusedPart = $focusedPart")

        if (focusedPart != null) {
            mHandler.postDelayed({
                val focusedPart = mViewModel.editorLiveData.value?.parts?.findLast { it.isFocused() } as? EditorTextPart
                        ?: return@postDelayed

                val start = focusedPart.startPointer
                val endpointer = focusedPart.endPointer

                val allSpans = focusedPart.text.getEditorUsedSpans(start, endpointer)
                if (allSpans.isEmpty()) {//text has no modifiers
                    mBottomButtons.unSelectAll()
                } else {
                    allSpans.onEach {
                        mBottomButtons.setSelected(it, true)//select all found modifiers in bottom bar
                    }.let {
                        EditorTextModifier.remaining(allSpans)
                    }.forEach {
                        mBottomButtons.setSelected(it, false)//unselect all remainings modifiers in bottom bar
                    }
                }
            }, 250)
        }
    }

    override fun onClick(clickedButton: EditorTextModifier, allButtons: Set<EditorTextModifier>) {
        val selectedModifiers = mBottomButtons.getSelectedModifier()
        val focusedPart = mViewModel.editorLiveData.value?.parts?.findLast { it.isFocused() } as? EditorTextPart
        val startPointer = focusedPart?.startPointer ?: 0
        val endPointer = focusedPart?.endPointer ?: 0
        mAdapter.textModifiers = allButtons
        focusedPart?.text?.printStyleSpans()

        Timber.e("on click ${focusedPart}, selected ${mBottomButtons.getSelectedModifier()}")
        when (clickedButton) {
            EditorTextModifier.LINK -> {
                focusedPart ?: return
                if (!selectedModifiers.contains(EditorTextModifier.LINK)) {
                    if (startPointer == endPointer) {

                        val fr: SendLinkDialog

                        fr = if (startPointer == endPointer) {// if no text selected and user taped on "link" button
                            if (focusedPart.text.isWithinWord(startPointer)) {
                                Timber.e("focusedPart.text.isWithinWord(startPointer)")
                                val startOfWord = focusedPart.text.getStartOfWord(startPointer)
                                val endOfWord = focusedPart.text.getEndOfWord(startPointer)
                                if (!checkStartAndEnd(startOfWord, endOfWord)) return
                                SendLinkDialog.getInstance(focusedPart.text.substring(startOfWord, endOfWord))
                            } else {
                                SendLinkDialog.getInstance()
                            }

                        } else {

                            val text = focusedPart.text.subSequence(startPointer, endPointer)
                            mSavedCursor = Pair(startPointer, endPointer)
                            SendLinkDialog.getInstance(text.toString())
                        }
                        fr.show(supportFragmentManager, null)
                    }
                } else {//if user selected text with url in it
                    Timber.e("removing url span")
                    val spans =
                            focusedPart.text.getSpans(startPointer, endPointer, URLSpan::class.java)
                    spans.forEach {
                        focusedPart.text.removeSpan(it)
                    }
                    validateBottomButtonSelections(mViewModel.editorLiveData.value?.parts
                            ?: return)
                }
            }
            EditorTextModifier.INSERT_IMAGE -> {
                val readExternalPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED
                if (readExternalPermission) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        intent.type = "image/jpeg"
                        startActivityForResult(intent, PICK_IMAGE_ID)
                    } else {
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "image/*"
                        startActivityForResult(intent, PICK_IMAGE_ID)
                    }

                } else {
                    ActivityCompat.requestPermissions(this, Array(1, { android.Manifest.permission.READ_EXTERNAL_STORAGE }), READ_EXTERNAL_PERMISSION)
                }
            }
            EditorTextModifier.QUOTATION_MARKS -> {
                if (focusedPart == null) return
                mSavedCursor = Pair(startPointer, endPointer)
                if (!mBottomButtons.getSelectedModifier().contains(EditorTextModifier.QUOTATION_MARKS)) {
                    Timber.e("adding quotation marks")
                    if (startPointer != endPointer) {
                        //if we selected smth
                        focusedPart.text.insert(startPointer, "\"")
                        focusedPart.text.insert(endPointer + 1, "\"")
                        mSavedCursor = Pair(startPointer + 1, endPointer + 1)//insert signs and move cursor + 1

                    } else {
                        //startPointer == endPointer
                        if (focusedPart.text.isWithinWord(startPointer)) {
                            val wordStart = focusedPart.text.getStartOfWord(startPointer)
                            val wordEnd = focusedPart.text.getEndOfWord(endPointer)
                            Timber.e("inserting quotation marks before and after word")
                            if (!checkStartAndEnd(wordStart, wordEnd)) return
                            focusedPart.text.insert(wordStart, "\"")
                            focusedPart.text.insert(wordEnd + 2, "\"")
                            mSavedCursor = Pair(startPointer + 2, endPointer + 2)
                        } else {
                            Timber.e("somewhere in whitespace")
                            focusedPart.text.insert(startPointer, "\"\"")
                            focusedPart.startPointer += 1
                            focusedPart.endPointer += 1
                            mSavedCursor = Pair(startPointer + 1, endPointer + 1)
                        }
                    }
                } else {
                    //startPointer == endPointer
                    Timber.e("deleting quotation marks")
                    val wordStart = focusedPart.text.getStartOfWord(startPointer)
                    val wordEnd = focusedPart.text.getEndOfWord(startPointer)
                    Timber.e("we selected smth with quotation marks")
                    if (!checkStartAndEnd(wordStart, wordEnd)) return
                    if (wordStart == wordEnd) {
                        Timber.e("word of 0 - length, this should not happen")
                        return
                    }
                    if ((wordStart + 1) > focusedPart.text.length || wordEnd > focusedPart.text.length) {
                        Timber.e("wordStart = $wordStart && wordEnd = $wordEnd and it length > ${focusedPart.text}")
                        return
                    }
                    val selected = focusedPart.text.subSequence(wordStart + 1, wordEnd)
                    focusedPart.text.replace(wordStart, wordEnd + 1, selected)
                    mSavedCursor = Pair(startPointer - 1, endPointer - 1)

                }
                onCursorChange(mViewModel.editorLiveData.value?.parts ?: return)
                mSavedCursor = null
                mViewModel.onTextChanged(mViewModel.editorLiveData.value?.parts ?: return)
            }
            EditorTextModifier.STYLE_BOLD, EditorTextModifier.TITLE -> {
                if (focusedPart == null) return

                mSavedCursor = Pair(startPointer, endPointer)

                val span: MetricAffectingSpan = if (clickedButton == EditorTextModifier.TITLE)
                    AbsoluteSizeSpan(getDimen(R.dimen.font_medium).toInt())
                else newBoldSpan()

                val needsCursorForwarding =
                        processMetricAffection(span, focusedPart.text, startPointer, endPointer)

                if (needsCursorForwarding) {
                    Timber.e("needsCursorForwarding")
                    focusedPart.startPointer += 1
                    focusedPart.endPointer += 1
                    mSavedCursor = Pair(focusedPart.startPointer, focusedPart.endPointer)
                }
                onCursorChange(mViewModel.editorLiveData.value?.parts ?: return)
                mSavedCursor = null
                mViewModel.onTextChanged(mViewModel.editorLiveData.value?.parts ?: return)
            }
            EditorTextModifier.QUOTATION, EditorTextModifier.LIST_BULLET, EditorTextModifier.LIST_NUMBERED -> {
                focusedPart?.text ?: return
                processLeadingMarginSpan(if (clickedButton == EditorTextModifier.QUOTATION)
                    KnifeQuoteSpan(getColorCompat(R.color.blue_light), 4, 4)
                else if (clickedButton == EditorTextModifier.LIST_BULLET)
                    KnifeBulletSpan(getColorCompat(R.color.blue_light), 4, 12)
                else NumberedMarginSpan(12, 18, 1),
                        focusedPart.text, startPointer, endPointer)
            }
        }
    }

    private fun processLeadingMarginSpan(leadingSpan: LeadingMarginSpan,
                                         editable: Editable, startPointer: Int, endPointer: Int) {
        Timber.e("processLeadingMarginSpan $leadingSpan ")
        if (mBottomButtons.isSelected(leadingSpan)) {
            //adding quotation spans
            editable.getSpans(startPointer, endPointer, LeadingMarginSpan::class.java).forEach { editable.removeSpan(it) }
            if (startPointer != endPointer) {
                var placeToInsertEndBreak = endPointer
                var placeToInsertStartBreak = startPointer

                if (!editable.isPreviousCharLineBreak(placeToInsertStartBreak)) {
                    editable.insert(placeToInsertStartBreak, "\n")
                    placeToInsertEndBreak += 1
                }
                editable.insert(placeToInsertEndBreak, "\n")
                editable
                        .setSpan(leadingSpan,
                                startPointer,
                                endPointer,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                var pointerToParagraph = editable.getStartAndEndPositionOfLinePointed(startPointer)
                if (pointerToParagraph.second == 0) {
                    Timber.e("wrong paragraph end position - 0 text = ${editable}")
                    return
                }
                if (pointerToParagraph.first == pointerToParagraph.second) {
                    editable.insert(pointerToParagraph.second, " ")
                    pointerToParagraph = Pair(pointerToParagraph.first, pointerToParagraph.second + 1)
                }

                editable
                        .setSpan(leadingSpan,
                                pointerToParagraph.first,
                                pointerToParagraph.second,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
        } else {
            //deleting quotation spans
            val spans = editable.getSpans(startPointer, endPointer, leadingSpan::class.java)
            spans.forEach {
                editable.removeSpan(it)
            }
        }
    }


    /**
     * @return if true - needs to move cursor forward
     * */

    private fun processMetricAffection(styleSpan: MetricAffectingSpan,
                                       editable: Editable,
                                       startPointer: Int,
                                       endPointer: Int): Boolean {
        Timber.e("processMetricAffection styleSpan = $styleSpan editable = $editable, its lenth = ${editable.length} startPointer = $startPointer endPointer = $endPointer")
        if (mBottomButtons.isSelected(styleSpan)) {//if user tapped at Bold, but text not bold

            Timber.e("adding spans")
            if (startPointer != endPointer)//if we selected text
            {

                editable
                        .setSpan(styleSpan, startPointer, endPointer, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)


            } else if (startPointer == endPointer)//selected some point at editable
            {

                if (editable.isStartOf(startPointer)) {
                    Timber.e("we at the start of editable")
                    editable.setSpan(styleSpan, startPointer, endPointer, INCLUSIVE_INCLUSIVE)
                } else if (editable.isEndOf(startPointer)) {
                    Timber.e("we at the end of editable")
                    editable.setSpan(styleSpan, startPointer, endPointer, INCLUSIVE_INCLUSIVE)
                } else if (editable.isWithinWord(startPointer))//if we somewhere on in the middle
                {
                    Timber.e("we are within word")
                    val startOfWord = editable.getStartOfWord(startPointer)
                    val endOfWord = editable.getEndOfWord(startPointer)

                    if (!checkStartAndEnd(startOfWord, endOfWord)) return false


                    editable.setSpan(styleSpan, startOfWord, endOfWord + 1, EXCLUSIVE_EXCLUSIVE)
                } else {
                    Timber.e("we are ath some sort of whitespace")
                    editable.setSpan(styleSpan, startPointer, endPointer, EXCLUSIVE_INCLUSIVE)
                }
            }

        } else {//deleting style
            Timber.e("deleting spans")
            val trimmedLength = editable.trimEnd().length

            val spans = editable
                    .getSpans(startPointer, endPointer, styleSpan::class.java)

            if (startPointer != endPointer || (startPointer == endPointer && editable.isWithinWord(startPointer))) {
                Timber.e("removing span at the word")

                var wordStart = if (startPointer != endPointer) startPointer else editable.getStartOfWord(startPointer)
                var endOfWord = if (startPointer != endPointer) endPointer else editable.getEndOfWord(startPointer) + 1
                if (startPointer == endPointer) {
                    wordStart -= 1
                    endOfWord += 1
                }

                spans.forEach {
                    var spanStart = editable.getSpanStart(it)
                    var spanEnd = editable.getSpanEnd(it)
                    if (spanStart < 0) spanStart = 0
                    if (spanEnd < 0) spanEnd = editable.length
                    val spanFlag = editable.getSpanFlags(it)

                    Timber.e("startReal = ${editable.getSpanStart(it)}" +
                            " endReal = ${editable.getSpanEnd(it)} styledSpans = " +
                            editable.getSpans(0, editable.length, styleSpan::class.java).toStringCustom())
                    Timber.e("startSpan = $spanStart endSpan = $spanEnd wordStart = $wordStart wordEnd = $endOfWord")

                    editable.removeSpan(it)

                    if (spanStart <= wordStart && spanStart != wordStart) //copy pre spans, wrapping word, but avoiding 0-length spans
                        editable.setSpan(it, spanStart, wordStart, spanFlag)
                    if (spanEnd >= endOfWord && (endOfWord != spanEnd || startPointer == trimmedLength))//copy post spans, wrapping word, but avoiding 0-length spans
                    {
                        editable.setSpan(it.copy(it)
                                ?: return false, endOfWord, spanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    }
                }

            } else {
                //if startPointer == endPointer

                if (editable.isPositionNextToWord(startPointer) || editable.isPositionNextToWhiteSpace(startPointer)) {
                    Timber.e("isPositionNextToWord || isPositionNextToWhiteSpace")
                    //if we are behind text, and span stretched for us
                    if (spans.isEmpty()) return false

                    spans.forEach {
                        val spanStart = editable.getSpanStart(it)
                        var spanEnd = editable.getSpanEnd(it)
                        if (spanStart == spanEnd) editable.removeSpan(it)
                        else {

                            editable.removeSpan(it)
                            if (editable.isPositionNextToWhiteSpace(startPointer)) spanEnd -= 1
                            editable.setSpan(it, spanStart, spanEnd, EXCLUSIVE_EXCLUSIVE)

                            return false
                        }

                    }
                } else {
                    spans.forEach {
                        Timber.e("removing span")
                        editable.removeSpan(it)
                    }
                }
            }
        }
        return false
    }

    private fun isTitleHidden(): Boolean {
        if (mMode?.editorType == EditorType.CREATE_COMMENT && mMode?.rootStoryId != mMode?.workingItemId)//it is reply on some content
            return true
        if (mMode?.editorType == EditorType.EDIT_COMMENT) return true//it is edit of comment
        if (mMode?.editorType == EditorType.CREATE_COMMENT && mMode?.title?.isEmpty() == true)//it is create root comment, but title is empty
            return true
        return false
    }

    override fun onTagsSubmit(tags: List<String>) {
        mViewModel.onTagsChanged(tags)
    }

    override fun onEdit(parts: List<EditorPart>) {
        mViewModel.onTextChanged(parts)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_ID) {
            val handler = Handler(Looper.getMainLooper())
            if (data != null) {
                Thread(Runnable {
                    try {
                        val inputStream = contentResolver.openInputStream(data.data)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap == null) handler.post {
                            mToolbar.showSnackbar(R.string.wrong_image)
                            return@post
                        }
                        val f = File(cacheDir, System.currentTimeMillis().toString() + ".jpg")
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(f))
                        resizeToSize(f)
                        handler.post {
                            mViewModel
                                    .onUserInput(EditorInputAction.InsertAction(EditorImagePart(imageName = f.name,
                                            imageUrl = "file://${f.absolutePath}")))
                            mAdapter.onRequestFocus(mRecycler)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        handler.post {
                            mToolbar.showSnackbar(R.string.wrong_image)
                        }
                    }
                }).start()
            }
        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == PICK_IMAGE_ID)
            mAdapter.onRequestFocus(mRecycler)
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_EXTERNAL_PERMISSION &&
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) mBottomButtons.performClick(EditorTextModifier.INSERT_IMAGE)
    }


    override fun onPhotoDelete(image: EditorImagePart, parts: List<EditorPart>) {
        mViewModel.onUserInput(EditorInputAction.DeleteAction(parts.indexOf(image)))
    }

    companion object {
        @JvmStatic
        private val PICK_IMAGE_ID = nextInt()
        @JvmStatic
        private val READ_EXTERNAL_PERMISSION = nextInt()
        val MODE_TAG = "MODE_TAG"

        fun startRootCommentEditor(ctx: Context,
                                   rootStory: StoryWithComments,
                                   feedType: FeedType,
                                   storyFilter: StoryFilter?) {
            val mapper = jacksonObjectMapper()
            val intent = Intent(ctx, EditorActivity::class.java)
            intent.putExtra(MODE_TAG, mapper.writeValueAsString(EditorMode(rootStory.rootStory()!!.title,
                    rootStory.rootStory()?.author ?: return,
                    EditorType.CREATE_COMMENT,
                    rootStory.rootStory()?.id ?: return,
                    rootStory.rootStory()?.id ?: return,
                    storyFilter,
                    feedType)))
            ctx.startActivity(intent)
        }

        fun startAnswerOnCommentEditor(ctx: Context,
                                       rootStory: StoryWithComments,
                                       commentToAnswer: GolosDiscussionItem,
                                       feedType: FeedType,
                                       storyFilter: StoryFilter?) {
            val mapper = jacksonObjectMapper()
            val intent = Intent(ctx, EditorActivity::class.java)
            intent.putExtra(MODE_TAG, mapper.writeValueAsString(EditorMode(rootStory.rootStory()?.title
                    ?: return,
                    rootStory.rootStory()?.author ?: return,
                    EditorType.CREATE_COMMENT,
                    rootStory.rootStory()?.id ?: return,
                    commentToAnswer.id,
                    storyFilter,
                    feedType)))
            ctx.startActivity(intent)
        }

        fun startPostCreator(ctx: Context,
                             title: String) {
            val intent = Intent(ctx, EditorActivity::class.java)
            val mode = EditorMode(title, editorType = EditorType.CREATE_POST)
            val string = jacksonObjectMapper().writeValueAsString(mode)
            intent.putExtra(MODE_TAG, string)
            ctx.startActivity(intent)
        }

        fun startEditPostOrComment(ctx: Context,
                                   rootStory: StoryWithComments,
                                   itemToEdit: GolosDiscussionItem,
                                   feedType: FeedType,
                                   storyFilter: StoryFilter?) {
            val intent = Intent(ctx, EditorActivity::class.java)
            intent.putExtra(MODE_TAG, mapper.writeValueAsString(EditorMode(rootStory.rootStory()?.title
                    ?: return,
                    rootStory.rootStory()?.author ?: return,
                    if (itemToEdit.isRootStory) EditorType.EDIT_POST else EditorType.EDIT_COMMENT,
                    rootStory.rootStory()?.id ?: return,
                    itemToEdit.id,
                    storyFilter,
                    feedType)))
            ctx.startActivity(intent)
        }
    }
}
