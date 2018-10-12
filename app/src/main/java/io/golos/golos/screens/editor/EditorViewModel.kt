package io.golos.golos.screens.editor

import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import io.golos.golos.BuildConfig.DEBUG_EDITOR
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoriesFeed
import io.golos.golos.screens.story.model.*
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.isComment
import io.golos.golos.utils.isNullOrEmpty
import timber.log.Timber
import java.util.*

sealed class EditorTitle

data class TitleTextField(val text: String, val isEditable: Boolean, val hintId: Int?) : EditorTitle()

data class PostEditorTitle(val title: TitleTextField) : EditorTitle()

data class RootCommentEditorTitle(val title: TitleTextField, val subtitleText: String) : EditorTitle()

data class AnswerOnCommentEditorTitle(val author: String, val timeStamp: Long, val parentComment: List<Row>) : EditorTitle()

data class EditorState(val error: GolosError? = null,
                       val isLoading: Boolean = false,
                       var completeMessage: Int? = null,
                       val parts: List<EditorPart>,
                       val tags: List<String>)

data class PostSendStatus(val author: String,
                          val blog: String,
                          val permlink: String)

class EditorViewModel : ViewModel(), Observer<StoriesFeed> {
    private val mEditorLiveData = MutableLiveData<EditorState>()
    private val mTitleLiveData = MutableLiveData<EditorTitle>()
    private val postStatusLiveData = MutableLiveData<PostSendStatus>()
    /* val titleMaxLength = 255
     val postMaxLength = 100 * 1024
     val commentMaxLength = 16 * 1024*/
    private val mTextProcessor = TextProcessor
    private val mRepository = Repository.get
    private var mRootStory: StoryWithComments? = null
    private var mWorkingItem: GolosDiscussionItem? = null
    private var mDraftsPersister: DraftsPersister? = null
    private var mHtmlHandler: HtmlHandler? = null
    private var wasSent = false


    val editorLiveData: LiveData<EditorState> = mEditorLiveData
    val titleLiveData: LiveData<EditorTitle> = mTitleLiveData

    var mode: EditorMode? = null
        set(value) {

            field = value
            val feedType = field?.feedType
            val rootStoryId = field?.rootStoryId
            val workingItemId = field?.workingItemId
            if (feedType == null && rootStoryId == null && workingItemId == null) {//root rootWrapper editor
                mDraftsPersister?.getDraft(mode ?: return) { items, title, tags ->
                    if (items.isNotEmpty()) {
                        mTitleLiveData.value = PostEditorTitle(TitleTextField(title, true, R.string.enter_post_title))
                        mEditorLiveData.value = EditorState(null, false, null, items, tags)
                    } else {
                        mTitleLiveData.value = PostEditorTitle(TitleTextField("", true, R.string.enter_post_title))
                        mEditorLiveData.value = EditorState(parts = mTextProcessor.getInitialState(), tags = listOf())
                    }
                }
            }
        }

    fun onCreate(persister: DraftsPersister, htmlHandler: HtmlHandler) {
        this.mDraftsPersister = persister
        mHtmlHandler = htmlHandler
    }

    fun onStart() {
        val feedType = mode?.feedType
        val rootStoryId = mode?.rootStoryId
        val workingItemId = mode?.workingItemId
        if (feedType != null && rootStoryId != null && workingItemId != null) {

            Repository
                    .get
                    .getStories(feedType, mode?.storyFilter)
                    .observeForever(this)
        }
    }

    fun onStop() {
        val feedType = mode?.feedType

        Repository
                .get
                .getStories(feedType ?: return, mode?.storyFilter)
                .removeObserver(this)
    }


    override fun onChanged(t: StoriesFeed?) {

        if (mWorkingItem != null || t == null) return

        mRootStory = t.items.findLast { it.rootStory.id == mode?.rootStoryId }
        mRootStory?.let {
            mWorkingItem = if (it.rootStory.id == mode?.workingItemId) it.rootStory
            else {
                it.getFlataned().findLast { it.id == mode?.workingItemId }
            }
        }

        val editorType = mode?.editorType ?: return
        val isParentComment = mWorkingItem?.isComment() == true

        fun createBoldText(forString: String): Editable {
            val ssb = SpannableStringBuilder.valueOf(forString)
            ssb.setSpan(StyleSpan(Typeface.BOLD), 0, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return ssb
        }


        if (editorType == EditorActivity.EditorType.CREATE_COMMENT) {
            mDraftsPersister?.getDraft(mode ?: return) { items, _, tags ->

                mWorkingItem?.let { workingItem ->
                    mTitleLiveData.value = if (isParentComment) AnswerOnCommentEditorTitle(workingItem.author, workingItem.created, workingItem.parts)
                    else RootCommentEditorTitle(TitleTextField(workingItem.title, false, null), workingItem.author)

                    val parts = if (items.isNotEmpty() && !(items.size == 1 && items[0] is EditorTextPart && (items[0] as EditorTextPart).text.toString().trim().isEmpty())) items
                    else mTextProcessor.getInitialState(if (isParentComment) createBoldText("@${workingItem.author}  ") else null)

                    mEditorLiveData.value = EditorState(parts = parts,
                            tags = tags)
                }
            }
        } else if (editorType == EditorActivity.EditorType.EDIT_POST || editorType == EditorActivity.EditorType.EDIT_COMMENT) {
            mWorkingItem?.let {
                val parts: List<EditorPart> = (if (it.parts.isEmpty()) StoryParserToRows.parse(it, skipHtmlClean = true) else it.parts)
                        .map {
                            when (it) {
                                is TextRow -> EditorTextPart(UUID.randomUUID().toString(),
                                        SpannableStringBuilder.valueOf(mHtmlHandler?.fromHtml(it.text)
                                                ?: ""))
                                is ImageRow -> EditorImagePart(UUID.randomUUID().toString(), "image", it.src)
                            }
                        }
                if (editorType == EditorActivity.EditorType.EDIT_POST) {//edit post
                    mTitleLiveData.value = PostEditorTitle(TitleTextField(it.title, true, R.string.enter_post_title))
                } else {
                    if (isParentComment) {//edit comment to a comment
                        mTitleLiveData.value = AnswerOnCommentEditorTitle(it.author, it.created, it.parts)
                    } else {//edit comment to a post
                        mTitleLiveData.value = RootCommentEditorTitle(TitleTextField(it.title, false, null), it.author)
                    }
                }
                mEditorLiveData.value = EditorState(parts = parts,
                        tags = it.tags.map { LocalizedTag.convertToLocalizedName(it) })

            }
        }
    }

    @MainThread
    fun onUserInput(action: EditorInputAction) {
        if (DEBUG_EDITOR) Timber.e("on user Input action = $action parts = ${editorLiveData.value?.parts}")
        val parts = editorLiveData.value?.parts ?: ArrayList()
        val result = mTextProcessor.processInput(parts, action)
        mEditorLiveData.value = mEditorLiveData.value?.copy(parts = result)
    }

    @MainThread
    fun onTitleChanged(input: String) {
        val title = mTitleLiveData.value as? PostEditorTitle
        title?.let {
            mTitleLiveData.value = it.copy(title = it.title.copy(text = input))
        }
    }

    @MainThread
    fun onTagsChanged(it: List<String>) {
        mEditorLiveData.value = mEditorLiveData.value?.copy(tags = it)
    }

    @MainThread
    fun onTextChanged(parts: List<EditorPart>) {
        if (DEBUG_EDITOR) Timber.e("onTextChanged")
        mEditorLiveData.value = mEditorLiveData.value?.copy(parts = parts)
    }

    @MainThread
    fun onSubmit() {
        if (mode == null) {
            return
        }
        if (!mRepository.isUserLoggedIn()) return

        val editorType = mode?.editorType ?: return
        val parts = editorLiveData.value?.parts ?: return

        if (parts.isEmpty() ||
                (parts.size == 1
                        && parts[0].htmlRepresentation.isEmpty())) {
            mEditorLiveData.value = mEditorLiveData.value?.copy(
                    error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null,
                            localizedMessage = R.string.post_body_must_be_not_empty))
            return
        }

        if (editorType == EditorActivity.EditorType.CREATE_POST || editorType == EditorActivity.EditorType.EDIT_POST) {
            val titleText = (mTitleLiveData.value as? PostEditorTitle)?.title?.text?.toString().orEmpty()
            if (titleText.isEmpty()) {
                mEditorLiveData.value = mEditorLiveData.value?.copy(
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.enter_title))
                return
            }

            if (mEditorLiveData.value?.tags.isNullOrEmpty()) {
                mEditorLiveData.value = mEditorLiveData.value?.copy(
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null,
                                localizedMessage = R.string.at_least_one_tag))
                return
            }
            mEditorLiveData.value = mEditorLiveData.value?.copy(isLoading = true, error = null)

            val listener: (CreatePostResult?, GolosError?) -> Unit = { result, error ->
                if (error == null) {
                    wasSent = true
                    if (mode != null) mDraftsPersister?.deleteDraft(mode!!, {})
                    mEditorLiveData.value = mEditorLiveData.value?.copy(error = null,
                            isLoading = false, completeMessage = R.string.send_post_success)
                    postStatusLiveData.value = PostSendStatus(result?.author
                            ?: "",
                            result?.blog ?: "",
                            result?.permlink ?: "")

                } else {
                    mEditorLiveData.value = mEditorLiveData.value?.copy(error = error,
                            isLoading = false)
                }
            }
            if (editorType == EditorActivity.EditorType.CREATE_POST) {
                mRepository.createPost(titleText,
                        editorLiveData.value?.parts ?: ArrayList(),
                        editorLiveData.value?.tags ?: return,
                        listener)
                try {
                    Answers.getInstance().logCustom(CustomEvent("created post"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (editorType == EditorActivity.EditorType.EDIT_POST) {

                if (mRootStory == null || mWorkingItem == null) return

                mRepository.editPost(titleText,
                        editorLiveData.value?.parts ?: ArrayList(),
                        editorLiveData.value?.tags ?: return,
                        mRootStory?.rootStory ?: return, listener)
                try {
                    Answers.getInstance().logCustom(CustomEvent("edited post"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else if (editorType == EditorActivity.EditorType.CREATE_COMMENT || editorType == EditorActivity.EditorType.EDIT_COMMENT) {

            if (mRootStory == null || mWorkingItem == null) return

            val listener: (CreatePostResult?, GolosError?) -> Unit = { _, error ->
                if (error == null) {
                    wasSent = true
                    if (mode != null)
                        mDraftsPersister?.deleteDraft(mode!!, {})
                    mEditorLiveData.value = mEditorLiveData.value?.copy(error = null,
                            isLoading = false, completeMessage = R.string.send_comment_success)
                } else {
                    mEditorLiveData.value = mEditorLiveData.value?.copy(error = error,
                            isLoading = false)
                }
            }
            mEditorLiveData.value = mEditorLiveData.value?.copy(isLoading = true, error = null)

            if (editorType == EditorActivity.EditorType.CREATE_COMMENT) {
                mRepository.createComment(mWorkingItem ?: return,
                        editorLiveData.value?.parts ?: ArrayList(),
                        listener)
                try {
                    Answers.getInstance().logCustom(CustomEvent("created comment"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (editorType == EditorActivity.EditorType.EDIT_COMMENT) {
                mRepository.editComment(mWorkingItem ?: return,
                        editorLiveData.value?.parts ?: return,
                        listener)

                try {
                    Answers.getInstance().logCustom(CustomEvent("edited comment"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onDestroy() {
        if (!wasSent) mDraftsPersister?.saveDraft(mode ?: return, editorLiveData.value?.parts
                ?: return,
                title = (mTitleLiveData.value as? PostEditorTitle)?.title?.text?.toString().orEmpty(),
                tags = editorLiveData.value?.tags ?: listOf(),
                completionHandler = {})
    }
}



