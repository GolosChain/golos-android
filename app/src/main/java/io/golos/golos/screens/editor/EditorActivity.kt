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
import android.graphics.Typeface
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
import android.text.style.*
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

        if (startPointer == endPointer) {
            mViewModel.onUserInput(EditorInputAction.InsertAction(
                    EditorTextPart(text = spannableString)))
        } else {
            focusedPart?.text?.removeUrlSpans(startPointer, endPointer)
            focusedPart?.text?.setSpan(KnifeURLSpan(linkAddress,
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
        if (mSavedCursor != null) {
            val focusedPart = parts.findLast { it.isFocused() } as? EditorTextPart
            if (focusedPart != null && mSavedCursor!!.second <= focusedPart.text.length) Selection.setSelection(//for unknown reason cursor
                    // begin chaotically float in edit text, while edititng it inners, sh we fix it in place during edit
                    focusedPart.text, mSavedCursor!!.first,
                    mSavedCursor!!.second)
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


                focusedPart.text.printStyleSpans()

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
                if (!selectedModifiers.contains(EditorTextModifier.LINK)) {
                    val fr: SendLinkDialog
                    fr = if (startPointer == endPointer) {// if no text selected and user taped on "link" button
                        SendLinkDialog.getInstance()
                    } else {
                        focusedPart ?: return
                        val text = focusedPart.text.subSequence(startPointer, endPointer)
                        mSavedCursor = Pair(startPointer, endPointer)
                        SendLinkDialog.getInstance(text.toString())
                    }
                    fr.show(supportFragmentManager, null)
                } else {//if user selected text with url in it
                    /* mAdapter.textModifiers = selectedModifiers - EditorTextModifier.LINK*/
                    focusedPart ?: return
                    val spans =
                            focusedPart.text.getSpans(startPointer, endPointer, URLSpan::class.java)
                    Timber.e("befor removing spans, ${focusedPart.text.getSpans(startPointer, endPointer, URLSpan::class.java).toStringCustom()}")
                    spans.forEach {
                        focusedPart.text.removeSpan(it)
                    }
                    Timber.e("after removing spans, ${focusedPart.text.getSpans(startPointer, endPointer, URLSpan::class.java).toStringCustom()}")
                    mViewModel.onTextChanged(mViewModel.editorLiveData.value?.parts ?: return)
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

                if (startPointer != endPointer) {//if we selected word
                    if (!mBottomButtons.getSelectedModifier().contains(EditorTextModifier.QUOTATION_MARKS)) {
                        focusedPart.text.insert(startPointer, "\"")
                        focusedPart.text.insert(endPointer + 1, "\"")
                        mSavedCursor = Pair(startPointer + 1, endPointer + 1)//insert signs and move cursor + 1
                        val spans = focusedPart.text.getSpans(startPointer, endPointer, MetricAffectingSpan::class.java)
                        spans.forEach {

                        }
                    } else {//if we selected word with quotations marks
                        val selected = focusedPart.text.subSequence(startPointer, endPointer)
                        Timber.e("selected = $selected")
                        mSavedCursor = Pair(startPointer, endPointer - 1)
                        focusedPart.text.replace(startPointer - 1, endPointer + 1, selected)

                        mSavedCursor = Pair(startPointer - 1, endPointer - 1)
                    }
                } else {//if we selected nothing
                    if ((startPointer == focusedPart.text.length || startPointer == 0)
                            && !(focusedPart.text.isInTheWord(startPointer))) {//start end of text, but previous not quote marks
                        focusedPart.text.insert(startPointer, "\"")
                        focusedPart.text.insert(endPointer + 1, "\"")
                        mSavedCursor = Pair(startPointer + 1, endPointer + 1)
                    } else if (focusedPart.text.isInTheWord(startPointer)) {//if we somewhere in the word
                        if (allButtons.contains(EditorTextModifier.QUOTATION_MARKS)) {// if it is with quotation marks
                            Timber.e("deleting quotation marks in word")
                            val startOfWord = focusedPart.text.getStartOfWord(startPointer - 1)
                            val endOfWord = focusedPart.text.getEndOfWord(startPointer - 1)
                            val selected = focusedPart.text.subSequence(startOfWord, endOfWord + 1)
                            Timber.e("replacing subsequence = $selected")
                            focusedPart.text.replace(startOfWord - 1, endOfWord + 2, selected)

                            mSavedCursor = Pair(startPointer - 1, endPointer - 1)
                        } else {
                            Timber.e("at the middle of the word")
                            val startOfWord = focusedPart.text.getStartOfWord(startPointer - 1)
                            val endOfWord = focusedPart.text.getEndOfWord(startPointer - 1)
                            val textLength = focusedPart.text.length
                            focusedPart.text.insert(startOfWord, "\"")
                            focusedPart.text.insert(endOfWord + 2, "\"")
                            mSavedCursor = if (startPointer == textLength) Pair(startPointer + 2, endPointer + 2)
                            else Pair(startPointer + 1, endPointer + 1)
                        }
                    }
                }


                onCursorChange(mViewModel.editorLiveData.value?.parts ?: return)
                mSavedCursor = null
                mViewModel.onTextChanged(mViewModel.editorLiveData.value?.parts ?: return)
            }
            EditorTextModifier.STYLE_BOLD, EditorTextModifier.TITLE -> {
                if (focusedPart == null) return

                mSavedCursor = Pair(startPointer, endPointer)

                val span: MetricAffectingSpan = if (clickedButton == EditorTextModifier.TITLE) AbsoluteSizeSpan(getDimen(R.dimen.font_big).toInt())
                else StyleSpan(Typeface.BOLD)

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
                if (editable.isWordWrappedByQuotationMarks(startPointer)) {
                    placeToInsertStartBreak -= 1
                    placeToInsertEndBreak += 1
                }
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

    private fun processMetricAffection(styleSpan: MetricAffectingSpan, editable: Editable, startPointer: Int, endPointer: Int): Boolean {
        if (mBottomButtons.isSelected(styleSpan)) {//if user tapped at Bold, but text not bold

            Timber.e("adding spans")
            if (startPointer != endPointer)//if we selected text
            {
                if (editable.isWordWrappedByQuotationMarks(startPointer)) {
                    editable
                            .setSpan(styleSpan, startPointer - 1, endPointer + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else {
                    editable
                            .setSpan(styleSpan, startPointer, endPointer, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

            } else if (startPointer == endPointer
                    && editable.isEndOfEditable(startPointer))//if we are athe the end
            {
                Timber.e("we at the end")
                editable.append('\u0020')
                editable.setSpan(styleSpan, startPointer, endPointer, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            } else if (startPointer == endPointer && startPointer != editable.length)//if we somewhere on in the middle
            {
                if (startPointer == 0 ||
                        /**if we at first char*/
                        (startPointer < editable.length && editable[startPointer] == ' ') || // if we at start/end of word
                        editable[startPointer - 1] == ' ') {
                    Timber.e("focused start/end of text")
                    editable.setSpan(styleSpan, startPointer, endPointer, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                } else {//if we are at the the middle of the word
                    Timber.e("we at the middle of word")
                    val startOfWord = editable.getStartOfWord(startPointer)
                    val endOfWord = editable.getEndOfWord(startPointer) + 1
                    if (startOfWord == -1 || endPointer == -1) {
                        editable.setSpan(styleSpan,
                                startOfWord,
                                endOfWord,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE)

                    } else if (editable.isWordWrappedByQuotationMarks(startPointer)) {
                        Timber.e("word wrapped in quotation marks")
                        editable.setSpan(styleSpan,
                                editable.getStartOfWord(startPointer) - 1,
                                editable.getEndOfWord(startPointer) + 2,
                                Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                    } else if (!editable.isInTheWord(startPointer)) {
                        editable.setSpan(styleSpan,
                                editable.getStartOfWord(startPointer),
                                editable.getEndOfWord(startPointer),
                                Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                    } else {
                        if (editable.isInTheWord(startPointer)) {
                            editable.setSpan(styleSpan,
                                    editable.getStartOfWord(startPointer),
                                    editable.getEndOfWord(startPointer),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        } else {
                            editable.setSpan(styleSpan,
                                    startPointer,
                                    startPointer,
                                    Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                        }

                    }
                }
            }
        } else {//deleting style
            val trimmedLength = editable.trimEnd().length

            Timber.e("length is ${editable.length} trimmedLength = $trimmedLength")
            val spans = editable
                    .getSpans(startPointer, endPointer, styleSpan::class.java)
                    .filter {
                        if (it is StyleSpan)
                            it.style == Typeface.BOLD
                        else it is AbsoluteSizeSpan
                    }

            if (startPointer == endPointer && (startPointer != 0 && (editable.length == startPointer
                            || trimmedLength + 1 == startPointer || trimmedLength == startPointer))) {
                //if we are behind text, and span stretched for us
                if (spans.isEmpty()) return false
                Timber.e("behind text")

                val span = spans[0]
                var spanStart = editable.getSpanStart(span)
                val spanEnd = editable.getSpanEnd(span)
                if (spanStart == spanEnd) spanStart -= 1

                editable.removeSpan(span)
                Timber.e(" focusedPart.text[spanEnd - 1] ${editable[spanEnd - 1]}")

                if (editable[endPointer - 1] != ' ') {//_d|_
                    editable.insert(endPointer + 1, " ")
                }
                editable.setSpan(span, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                return true

            } else if (editable.isInTheWord(startPointer)) {//if we somewhere in the word
                Timber.e("removing span at the word")

                var wordStart = editable.getStartOfWord(startPointer - 1)
                var endOfWord = editable.getEndOfWord(startPointer - 1) + 1
                if (editable.isInTheWord(startPointer)) {
                    wordStart -= 1
                    endOfWord += 1
                }

                val neededSpans = editable.getSpans(wordStart, endOfWord, styleSpan::class.java)
                        .filter {
                            Timber.e(it.toString())
                            if (it is StyleSpan)
                                it.style == Typeface.BOLD
                            else it is AbsoluteSizeSpan
                        }

                neededSpans.forEach {
                    var spanStart = editable.getSpanStart(it)
                    var spanEnd = editable.getSpanEnd(it)
                    if (spanStart < 0) spanStart = 0
                    if (spanEnd < 0) spanEnd = editable.length

                    Timber.e("startReal = ${editable.getSpanStart(it)}" +
                            " endReal = ${editable.getSpanEnd(it)} styledSpans = " +
                            editable.getSpans(0, editable.length, styleSpan::class.java).toStringCustom())
                    Timber.e("startSpan = $spanStart endSpan = $spanEnd wordStart = $wordStart wordEnd = $endOfWord")

                    editable.removeSpan(it)

                    if (spanStart <= wordStart && spanStart != wordStart) //copy pre spans, wrapping word, but avoiding 0-length spans
                        editable.setSpan(it, spanStart, wordStart, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    if (spanEnd >= endOfWord && (endOfWord != spanEnd || startPointer == trimmedLength))//copy post spans, wrapping word, but avoiding 0-length spans
                        editable.setSpan(it, endOfWord, spanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                }

            } else {
                spans.forEach {
                    Timber.e("removing span")
                    editable.removeSpan(it)
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
