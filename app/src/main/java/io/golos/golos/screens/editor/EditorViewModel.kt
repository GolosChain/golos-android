package io.golos.golos.screens.editor

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoriesFeed
import io.golos.golos.screens.story.model.*
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.*
import java.util.*

/**
 * Created by yuri yurivladdurain@gmail.com on 25/10/2017.
 */
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
    val postStatusLiveData = MutableLiveData<PostSendStatus>()
    val titleMaxLength = 255
    val postMaxLength = 100 * 1024
    val commentMaxLength = 16 * 1024
    private val mTextProcessor = TextProcessor()
    private val mRepository = Repository.get
    private var mRootStory: StoryWithComments? = null
    private var mWorkingItem: GolosDiscussionItem? = null
    private var wasSent = false
    var mode: EditorMode? = null
        set(value) {
            field = value
            val feedType = field?.feedType
            val rootStoryId = field?.rootStoryId
            val workingItemId = field?.workingItemId

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


    override fun onChanged(t: StoriesFeed?) {
        if (mWorkingItem != null || t == null) return

        mRootStory = t.items.findLast { it.rootStory()?.id == mode?.rootStoryId }
        mRootStory?.let {
            mWorkingItem = if (it.rootStory()?.id == mode?.workingItemId) it.rootStory()!!
            else {
                it.getFlataned().findLast { it.story.id == mode?.workingItemId }?.story
            }
        }
        DraftsPersister.getDraft(mode ?: return, { items, title, tags ->
            if (items.isNotEmpty()) {
                editorLiveData.value = EditorState(null, false, null, items, title, tags)
            } else {
                if (mode?.editorType == EditorActivity.EditorType.CREATE_POST || mode?.editorType == EditorActivity.EditorType.CREATE_COMMENT) {
                    editorLiveData.value = EditorState(parts = mTextProcessor.getInitialState(), title = "", tags = listOf())
                } else if (mode?.editorType == EditorActivity.EditorType.EDIT_COMMENT || mode?.editorType == EditorActivity.EditorType.EDIT_POST) {
                    mWorkingItem?.let {
                        val parts: List<EditorPart> = (if (it.parts.isEmpty()) StoryParserToRows.parse(it) else it.parts)
                                .map<Row, EditorPart> {
                                    when (it) {
                                        is TextRow -> EditorTextPart(UUID.randomUUID().toString(), it.text.toHtml().toString(), null)
                                        is ImageRow -> EditorImagePart(UUID.randomUUID().toString(), "image", it.src, null)
                                        else -> EditorTextPart(UUID.randomUUID().toString(), "", null)
                                    }
                                }
                        if (parts.isNotEmpty()) {
                            val last = parts.last()
                            if (last is EditorTextPart) last.pointerPosition = last.text.length
                            else {
                                parts.toArrayList().add(EditorTextPart(UUID.randomUUID().toString(), "", 0))
                            }
                        }
                        editorLiveData.value = EditorState(parts = parts, title = "", tags = it.tags.map { LocalizedTag.convertToLocalizedName(it) })
                    }
                }
            }
        })
    }

    fun onUserInput(action: EditorInputAction) {
        val parts = editorLiveData.value?.parts ?: ArrayList()
        editorLiveData.value = EditorState(parts = mTextProcessor.processInput(parts, action),
                title = editorLiveData.value?.title ?: "",
                tags = editorLiveData.value?.tags ?: listOf())
    }

    fun onTitleChanged(it: CharSequence) {
        editorLiveData.value = EditorState(parts = editorLiveData.value?.parts
                ?: listOf(), title = it.toString(),
                tags = editorLiveData.value?.tags ?: listOf())
    }

    fun onTagsChanged(it: List<String>) {

        editorLiveData.value = EditorState(parts = editorLiveData.value?.parts
                ?: listOf(), title = editorLiveData.value?.title ?: "",
                tags = it)
    }

    fun onTextChanged(parts: List<EditorPart>) {
        editorLiveData.value = EditorState(parts = parts, title = editorLiveData.value?.title ?: "",
                tags = editorLiveData.value?.tags ?: listOf())
    }

    fun onSubmit() {
        if (mode == null) {
            return
        }
        if (!mRepository.isUserLoggedIn()) return
        if (mode?.editorType == EditorActivity.EditorType.CREATE_POST) {
            if (editorLiveData.value?.title.isNullOrEmpty()) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts
                        ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.enter_title),
                        title = editorLiveData.value?.title ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())
                return
            }
            if (editorLiveData.value?.parts?.size ?: 0 == 0 ||
                    (editorLiveData.value?.parts?.size == 1
                            && editorLiveData.value!!.parts[0].markdownRepresentation.isEmpty())) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts
                        ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null,
                                localizedMessage = R.string.post_body_must_be_not_empty),
                        title = editorLiveData.value?.title ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())
                return
            }
            if (editorLiveData.value?.tags.isNullOrEmpty()) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts
                        ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null,
                                localizedMessage = R.string.at_least_one_tag),
                        title = editorLiveData.value?.title ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())
                return
            }
            editorLiveData.value = EditorState(isLoading = true,
                    parts = editorLiveData.value?.parts ?: ArrayList(),
                    title = editorLiveData.value?.title ?: "",
                    tags = editorLiveData.value?.tags ?: listOf())
            mRepository.createPost(editorLiveData.value?.title ?: return,
                    editorLiveData.value?.parts ?: ArrayList(),
                    editorLiveData.value?.tags ?: return,
                    { result, error ->
                        if (error == null) {
                            wasSent = true
                            DraftsPersister.deleteDraft(mode ?: return@createPost, {})
                        }
                        editorLiveData.value = EditorState(error = error,
                                isLoading = false,
                                parts = editorLiveData.value?.parts ?: ArrayList(),
                                completeMessage = if (error == null) R.string.send_post_success else null, title = editorLiveData.value?.title
                                ?: "",
                                tags = editorLiveData.value?.tags ?: listOf())
                        postStatusLiveData.value = PostSendStatus(result?.author
                                ?: return@createPost,
                                result.blog,
                                result.permlink)

                    })
        } else if (mode?.editorType == EditorActivity.EditorType.CREATE_COMMENT) {
            if (mRootStory == null || mWorkingItem == null) return
            if (editorLiveData.value?.parts?.size ?: 0 == 0 ||
                    (editorLiveData.value?.parts?.size == 1
                            && editorLiveData.value!!.parts[0].markdownRepresentation.isEmpty())) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts
                        ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.comment_body_must_be_not_empty), title = editorLiveData.value?.title
                        ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())
                return
            }
            editorLiveData.value = EditorState(isLoading = true, parts = editorLiveData.value?.parts
                    ?: ArrayList(),
                    title = editorLiveData.value?.title ?: "",
                    tags = editorLiveData.value?.tags ?: listOf())
            mRepository.createComment(mRootStory!!,
                    mWorkingItem!!,
                    editorLiveData.value?.parts ?: ArrayList(), { result, error ->
                if (error == null) {
                    wasSent = true
                    DraftsPersister.deleteDraft(mode ?: return@createComment, {})
                }

                editorLiveData.value = EditorState(error = error,
                        isLoading = false,
                        parts = editorLiveData.value?.parts ?: ArrayList(),
                        completeMessage = if (error == null) R.string.send_comment_success else null,
                        title = editorLiveData.value?.title ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())

            })

        }
    }

    fun onDestroy() {
        if (!wasSent) DraftsPersister.saveDraft(mode ?: return, editorLiveData.value?.parts
                ?: return,
                title = editorLiveData.value?.title ?: "",
                tags = editorLiveData.value?.tags ?: listOf(),
                completionHandler = {})
    }
}



