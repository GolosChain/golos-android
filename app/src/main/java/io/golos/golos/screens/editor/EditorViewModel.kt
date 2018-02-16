package io.golos.golos.screens.editor

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoriesFeed
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.isNullOrEmpty

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
    private var mItemToAnswerOn: GolosDiscussionItem? = null
    private var wasSent = false
    var mode: EditorMode? = null
        set(value) {
            field = value
            val feedType = field?.feedType
            val rootStoryId = field?.rootStoryId
            val itemToAnsweronId = field?.commentToAnswerOnId
            DraftsPersister.getDraft(value ?: return, { items, title, tags ->
                if (items.isNotEmpty())
                    editorLiveData.value = EditorState(null, false, null, items, title, tags)
                else {
                    editorLiveData.value = EditorState(parts = mTextProcessor.getInitialState(), title = "", tags = listOf())
                }
            })
            if (feedType != null && rootStoryId != null && itemToAnsweronId != null) {
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
        mRootStory = t?.items?.findLast { it.rootStory()?.id == mode?.rootStoryId }
        mRootStory?.let {
            if (it.rootStory()?.id == mode?.commentToAnswerOnId) mItemToAnswerOn = it.rootStory()!!
            else {
                mItemToAnswerOn = it.getFlataned().findLast { it.story.id == mode?.commentToAnswerOnId }?.story
            }
        }
    }

    fun onUserInput(action: EditorInputAction) {
        val parts = editorLiveData.value?.parts ?: ArrayList()
        editorLiveData.value = EditorState(parts = mTextProcessor.processInput(parts, action),
                title = editorLiveData.value?.title ?: "",
                tags = editorLiveData.value?.tags ?: listOf())
    }

    fun onTitleChanged(it: CharSequence) {
        editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: listOf(), title = it.toString(),
                tags = editorLiveData.value?.tags ?: listOf())
    }

    fun onTagsChanged(it: List<String>) {

        editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: listOf(), title = editorLiveData.value?.title ?: "",
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
        if (mode!!.isPostEditor) {
            if (editorLiveData.value?.title.isNullOrEmpty()) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.enter_title),
                        title = editorLiveData.value?.title ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())
                return
            }
            if (editorLiveData.value?.parts?.size ?: 0 == 0 ||
                    (editorLiveData.value?.parts?.size == 1
                            && editorLiveData.value!!.parts[0].markdownRepresentation.isEmpty())) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null,
                                localizedMessage = R.string.post_body_must_be_not_empty),
                        title = editorLiveData.value?.title ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())
                return
            }
            if (editorLiveData.value?.tags.isNullOrEmpty()) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: ArrayList(),
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
                                completeMessage = if (error == null) R.string.send_post_success else null, title = editorLiveData.value?.title ?: "",
                                tags = editorLiveData.value?.tags ?: listOf())
                        postStatusLiveData.value = PostSendStatus(result?.author ?: return@createPost,
                                result.blog,
                                result.permlink)

                    })
        } else {
            if (mRootStory == null || mItemToAnswerOn == null) return
            if (editorLiveData.value?.parts?.size ?: 0 == 0 ||
                    (editorLiveData.value?.parts?.size == 1
                            && editorLiveData.value!!.parts[0].markdownRepresentation.isEmpty())) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.comment_body_must_be_not_empty), title = editorLiveData.value?.title ?: "",
                        tags = editorLiveData.value?.tags ?: listOf())
                return
            }
            editorLiveData.value = EditorState(isLoading = true, parts = editorLiveData.value?.parts ?: ArrayList(),
                    title = editorLiveData.value?.title ?: "",
                    tags = editorLiveData.value?.tags ?: listOf())
            mRepository.createComment(mRootStory!!,
                    mItemToAnswerOn!!,
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
        if (!wasSent) DraftsPersister.saveDraft(mode ?: return, editorLiveData.value?.parts ?: return,
                title = editorLiveData.value?.title ?: "",
                tags = editorLiveData.value?.tags ?: listOf(),
                completionHandler = {})
    }
}



