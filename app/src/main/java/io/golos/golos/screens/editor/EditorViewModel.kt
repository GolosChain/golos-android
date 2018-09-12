package io.golos.golos.screens.editor

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.support.annotation.MainThread
import android.text.SpannableStringBuilder
import io.golos.golos.BuildConfig.DEBUG_EDITOR
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.repository.model.StoriesFeed
import io.golos.golos.screens.story.model.*
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.isNullOrEmpty
import timber.log.Timber
import java.util.*


data class EditorState(val error: GolosError? = null,
                       val isLoading: Boolean = false,
                       var completeMessage: Int? = null,
                       val parts: List<EditorPart>,
                       val title: String,
                       val tags: List<String>)

data class PostSendStatus(val author: String,
                          val blog: String,
                          val permlink: String)

class EditorViewModel : ViewModel(), Observer<StoriesFeed> {
    val editorLiveData = MutableLiveData<EditorState>()
    private val postStatusLiveData = MutableLiveData<PostSendStatus>()
    /* val titleMaxLength = 255
     val postMaxLength = 100 * 1024
     val commentMaxLength = 16 * 1024*/
    private val mTextProcessor = TextProcessor
    private val mRepository = Repository.get
    private var mRootStory: StoryWithComments? = null
    private var mWorkingItem: StoryWrapper? = null
    private var mDraftsPersister: DraftsPersister? = null
    private var mHtmlHandler: HtmlHandler? = null
    private var wasSent = false
    var mode: EditorMode? = null
        set(value) {

            field = value
            val feedType = field?.feedType
            val rootStoryId = field?.rootStoryId
            val workingItemId = field?.workingItemId
            if (feedType == null && rootStoryId == null && workingItemId == null) {//root story editor
                mDraftsPersister?.getDraft(mode ?: return) { items, title, tags ->
                    if (items.isNotEmpty()) {
                        editorLiveData.value = EditorState(null, false, null, items, title, tags)
                    } else {

                        editorLiveData.value = EditorState(parts = mTextProcessor.getInitialState(), title = "", tags = listOf())
                    }
                }
            }
            if (feedType != null && rootStoryId != null && workingItemId != null) {
                Repository
                        .get
                        .getStories(feedType, mode?.storyFilter)
                        .removeObserver(this)
                Repository
                        .get
                        .getStories(feedType, mode?.storyFilter)
                        .observeForever(this)
            }
        }

    fun onCreate(persister: DraftsPersister, htmlHandler: HtmlHandler) {
        this.mDraftsPersister = persister
        mHtmlHandler = htmlHandler
    }


    override fun onChanged(t: StoriesFeed?) {
        if (mWorkingItem != null || t == null) return

        mRootStory = t.items.findLast { it.rootStory()?.id == mode?.rootStoryId }
        mRootStory?.let {
            mWorkingItem = if (it.rootStory()?.id == mode?.workingItemId) it.storyWithState()!!
            else {
                it.getFlataned().findLast { it.story.id == mode?.workingItemId }
            }
        }
        val editorType = mode?.editorType ?: return

        if (editorType == EditorActivity.EditorType.CREATE_COMMENT || editorType == EditorActivity.EditorType.CREATE_POST) {
            mDraftsPersister?.getDraft(mode ?: return) { items, title, tags ->
                if (items.isNotEmpty()) {
                    editorLiveData.value = EditorState(null, false, null, items, title, tags)
                } else {
                    editorLiveData.value = EditorState(parts = mTextProcessor.getInitialState(), title = "", tags = listOf())
                }
            }
        } else if (editorType == EditorActivity.EditorType.EDIT_POST || editorType == EditorActivity.EditorType.EDIT_COMMENT) {
            mWorkingItem?.story?.let {
                val parts: List<EditorPart> = (if (it.parts.isEmpty()) StoryParserToRows.parse(it, skipHtmlClean = true) else it.parts)
                        .map {
                            when (it) {
                                is TextRow -> EditorTextPart(UUID.randomUUID().toString(),
                                        SpannableStringBuilder.valueOf(mHtmlHandler?.fromHtml(it.text)
                                                ?: ""))
                                is ImageRow -> EditorImagePart(UUID.randomUUID().toString(), "image", it.src)
                            }
                        }
                editorLiveData.value = EditorState(parts = parts,
                        title = if (editorType == EditorActivity.EditorType.EDIT_POST) it.title else "",
                        tags = it.tags.map { LocalizedTag.convertToLocalizedName(it) })
            }
        }
    }

    @MainThread
    fun onUserInput(action: EditorInputAction) {
        if (DEBUG_EDITOR) Timber.e("on user Input action = $action parts = ${editorLiveData.value?.parts}")
        val parts = editorLiveData.value?.parts ?: ArrayList()
        val result = mTextProcessor.processInput(parts, action)
        editorLiveData.value = EditorState(parts = result,
                title = editorLiveData.value?.title ?: "",
                tags = editorLiveData.value?.tags ?: listOf())
    }

    @MainThread
    fun onTitleChanged(it: CharSequence) {
        editorLiveData.value = EditorState(parts = editorLiveData.value?.parts
                ?: listOf(), title = it.toString(),
                tags = editorLiveData.value?.tags ?: listOf())
    }

    @MainThread
    fun onTagsChanged(it: List<String>) {

        editorLiveData.value = EditorState(parts = editorLiveData.value?.parts
                ?: listOf(), title = editorLiveData.value?.title ?: "",
                tags = it)
    }

    @MainThread
    fun onTextChanged(parts: List<EditorPart>) {
        if (DEBUG_EDITOR) Timber.e("onTextChanged")
        editorLiveData.value = EditorState(parts = parts, title = editorLiveData.value?.title ?: "",
                tags = editorLiveData.value?.tags ?: listOf())
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
            editorLiveData.value = EditorState(parts = parts,
                    error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null,
                            localizedMessage = R.string.post_body_must_be_not_empty),
                    title = editorLiveData.value?.title ?: "",
                    tags = editorLiveData.value?.tags ?: listOf())
            return
        }

        if (editorType == EditorActivity.EditorType.CREATE_POST || editorType == EditorActivity.EditorType.EDIT_POST) {
            if (editorLiveData.value?.title.isNullOrEmpty()) {
                editorLiveData.value = EditorState(parts = parts,
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.enter_title),
                        title = editorLiveData.value?.title ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())
                return
            }

            if (editorLiveData.value?.tags.isNullOrEmpty()) {
                editorLiveData.value = EditorState(parts = parts,
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null,
                                localizedMessage = R.string.at_least_one_tag),
                        title = editorLiveData.value?.title ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())
                return
            }
            editorLiveData.value = EditorState(isLoading = true,
                    parts = parts,
                    title = editorLiveData.value?.title ?: "",
                    tags = editorLiveData.value?.tags ?: listOf())

            val listener: (CreatePostResult?, GolosError?) -> Unit = { result, error ->
                if (error == null) {
                    wasSent = true
                    if (mode != null) mDraftsPersister?.deleteDraft(mode!!, {})

                }
                editorLiveData.value = EditorState(error = error,
                        isLoading = false,
                        parts = editorLiveData.value?.parts ?: ArrayList(),
                        completeMessage = if (error == null) R.string.send_post_success else null, title = editorLiveData.value?.title
                        ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())
                postStatusLiveData.value = PostSendStatus(result?.author
                        ?: "",
                        result?.blog ?: "",
                        result?.permlink ?: "")

            }

            if (editorType == EditorActivity.EditorType.CREATE_POST) {
                mRepository.createPost(editorLiveData.value?.title ?: return,
                        editorLiveData.value?.parts ?: ArrayList(),
                        editorLiveData.value?.tags ?: return,
                        listener)
            } else if (editorType == EditorActivity.EditorType.EDIT_POST) {

                if (mRootStory == null || mWorkingItem == null) return

                mRepository.editPost(editorLiveData.value?.title ?: return,
                        editorLiveData.value?.parts ?: ArrayList(),
                        editorLiveData.value?.tags ?: return,
                        mRootStory?.storyWithState() ?: return, listener)
            }
        } else if (editorType == EditorActivity.EditorType.CREATE_COMMENT || editorType == EditorActivity.EditorType.EDIT_COMMENT) {

            if (mRootStory == null || mWorkingItem == null) return

            val listener: (CreatePostResult?, GolosError?) -> Unit = { _, error ->
                if (error == null) {
                    wasSent = true
                    if (mode != null)
                        mDraftsPersister?.deleteDraft(mode!!, {})
                }

                editorLiveData.value = EditorState(error = error,
                        isLoading = false,
                        parts = editorLiveData.value?.parts ?: ArrayList(),
                        completeMessage = if (error == null) R.string.send_comment_success else null,
                        title = editorLiveData.value?.title ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())

            }

            editorLiveData.value = EditorState(isLoading = true, parts = editorLiveData.value?.parts
                    ?: ArrayList(),
                    title = editorLiveData.value?.title ?: "",
                    tags = editorLiveData.value?.tags ?: listOf())
            if (editorType == EditorActivity.EditorType.CREATE_COMMENT) {
                mRepository.createComment(mWorkingItem ?: return,
                        editorLiveData.value?.parts ?: ArrayList(),
                        listener)
            } else if (editorType == EditorActivity.EditorType.EDIT_COMMENT) {
                mRepository.editComment(mWorkingItem ?: return,
                        editorLiveData.value?.parts ?: return,
                        listener)
            }
        }
    }

    fun onDestroy() {
        if (!wasSent) mDraftsPersister?.saveDraft(mode ?: return, editorLiveData.value?.parts
                ?: return,
                title = editorLiveData.value?.title ?: "",
                tags = editorLiveData.value?.tags ?: listOf(),
                completionHandler = {})
    }
}



