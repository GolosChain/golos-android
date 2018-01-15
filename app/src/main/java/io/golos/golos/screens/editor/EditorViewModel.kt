package io.golos.golos.screens.editor

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoryTreeItems
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError

/**
 * Created by yuri yurivladdurain@gmail.com on 25/10/2017.
 */
data class EditorState(val error: GolosError? = null,
                       val isLoading: Boolean = false,
                       var completeMessage: Int? = null,
                       val parts: List<EditorPart>)

data class PostSendStatus(val author: String,
                          val blog: String,
                          val permlink: String)

class EditorViewModel : ViewModel(), Observer<StoryTreeItems> {
    val editorLiveData = MutableLiveData<EditorState>()
    val postStatusLiveData = MutableLiveData<PostSendStatus>()
    val titleMaxLength = 255
    val postMaxLength = 100 * 1024
    val commentMaxLength = 16 * 1024
    private val mTextProcessor = TextProcessor()
    private var mTitleText: String = ""
    private var mTags = ArrayList<String>()
    private val mRepository = Repository.get
    private var mRootStory: StoryTree? = null
    private var mItemToAnswerOn: GolosDiscussionItem? = null
    var mode: EditorMode? = null
        set(value) {
            field = value
            val feedType = field?.feedType
            val rootStoryId = field?.rootStoryId
            val itemToAnsweronId = field?.commentToAnswerOnId
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

    init {
        editorLiveData.value = EditorState(parts = mTextProcessor.getInitialState())
    }

    override fun onChanged(t: StoryTreeItems?) {
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
        editorLiveData.value = EditorState(parts = mTextProcessor.processInput(parts, action))
    }

    fun onTitileChanges(it: CharSequence) {
        mTitleText = it.toString()
    }

    fun onTagsChanged(it: List<String>) {
        mTags = ArrayList(it)
    }

    fun onTextChanged(parts: List<EditorPart>) {
        editorLiveData.value = EditorState(parts = parts)
    }

    fun onSubmit() {
        if (mode == null) {
            return
        }
        if (!mRepository.isUserLoggedIn()) return
        if (mode!!.isPostEditor) {
            if (mTitleText.isEmpty()) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.enter_title))
                return
            }
            if (editorLiveData.value?.parts?.size ?: 0 == 0 ||
                    (editorLiveData.value?.parts?.size == 1
                            && editorLiveData.value!!.parts[0].markdownRepresentation.isEmpty())) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.post_body_must_be_not_empty))
                return
            }
            if (mTags.size < 1) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.at_least_one_tag))
                return
            }
            editorLiveData.value = EditorState(isLoading = true, parts = editorLiveData.value?.parts ?: ArrayList())
            mRepository.createPost(mTitleText, editorLiveData.value?.parts ?: ArrayList(), mTags, { result, error ->
                editorLiveData.value = EditorState(error = error,
                        isLoading = false,
                        parts = editorLiveData.value?.parts ?: ArrayList(),
                        completeMessage = if (error == null) R.string.send_post_success else null)
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
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.comment_body_must_be_not_empty))
                return
            }
            editorLiveData.value = EditorState(isLoading = true, parts = editorLiveData.value?.parts ?: ArrayList())
            mRepository.createComment(mRootStory!!,
                    mItemToAnswerOn!!,
                    editorLiveData.value?.parts ?: ArrayList(), { result, error ->
                editorLiveData.value = EditorState(error = error,
                        isLoading = false,
                        parts = editorLiveData.value?.parts ?: ArrayList(),
                        completeMessage = if (error == null) R.string.send_comment_success else null)
            })
        }
    }
}



