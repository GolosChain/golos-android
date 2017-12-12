package io.golos.golos.screens.story

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.widget.ImageView
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.StoryFilter
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.stories.FilteredStoriesActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.screens.story.model.StoryViewState
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.screens.widgets.PhotoActivity
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.Translit

/**
 * Created by yuri on 06.11.17.
 */
class StoryViewModel : ViewModel() {
    private val mLiveData = MediatorLiveData<StoryViewState>()
    private val mRepository = Repository.get
    lateinit var author: String
    lateinit var permLink: String
    lateinit var blog: String
    private lateinit var feedType: FeedType

    fun onCreate(author: String, permLink: String, blog: String, feedType: FeedType) {
        this.author = author
        this.permLink = permLink
        this.blog = blog
        this.feedType = feedType
        mLiveData.removeSource(mRepository.getStories(feedType, null))
        mLiveData.addSource(mRepository.getStories(feedType, null)) {
            val storyItems = it
            it?.
                    items?.
                    filter {
                        it.rootStory()?.author == this.author
                                && it.rootStory()?.permlink == this.permLink
                                && it.rootStory()?.categoryName == this.blog
                    }?.
                    forEach {
                        mLiveData.value = StoryViewState(false,
                                it.rootStory()?.title ?: "",
                                mLiveData.value?.isStoryCommentButtonShown ?: false,
                                storyItems?.error,
                                it.rootStory()?.tags ?: ArrayList(),
                                it)
                    }
        }
        mLiveData.removeSource(mRepository.getCurrentUserDataAsLiveData())
        mLiveData.addSource(mRepository.getCurrentUserDataAsLiveData()) {
            mLiveData.value = StoryViewState(mLiveData.value?.isLoading ?: false,
                    mLiveData.value?.storyTitle ?: "",
                    it?.userName != null,
                    mLiveData.value?.errorCode,
                    mLiveData.value?.tags ?: ArrayList(),
                    mLiveData.value?.storyTree ?: StoryTree(null, ArrayList()))
        }
        mRepository.requestStoryUpdate(this.author, this.permLink, this.blog, feedType)
    }


    val liveData: LiveData<StoryViewState>
        get() {
            return mLiveData
        }

    fun onMainStoryTextClick(activity: Activity, text: String) {

    }

    fun onMainStoryImageClick(activity: Activity, src: String, iv: ImageView?) {
        if (iv == null) PhotoActivity.startActivity(context = activity, imageSrc = src)
        else PhotoActivity.startActivityUsingTransition(activity, iv, src)
    }

    val showVoteDialog: Boolean
        get() {
            return mRepository.isUserLoggedIn()

        }

    fun canUserVoteOnThis(story: StoryWrapper): Boolean {
        return !story.story.isUserUpvotedOnThis
    }

    fun onStoryVote(story: StoryWrapper, percent: Short) {
        if (canUserVoteOnThis(story)) mRepository.upVote(story.story, percent)
        else mRepository.cancelVote(story.story)
    }

    fun canUserWriteComments(): Boolean {
        return mRepository.isUserLoggedIn()
    }

    fun onWriteRootComment(ctx: Context) {
        if (canUserWriteComments()) {
            mLiveData.value?.let {
                EditorActivity.startRootCommentEditor(ctx,
                        it.storyTree,
                        feedType)
            }
        }
    }

    fun onAnswerToComment(ctx: Context, item: GolosDiscussionItem) {
        if (mRepository.isUserLoggedIn()) {
            mLiveData.value?.let {
                EditorActivity.startAnswerOnCommentEditor(ctx,
                        it.storyTree,
                        item,
                        feedType)
            }
        } else {
            mLiveData.value = StoryViewState(mLiveData.value?.isLoading ?: false,
                    mLiveData.value?.storyTitle ?: "",
                    mRepository.isUserLoggedIn(),
                    GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_write_comment),
                    mLiveData.value?.tags ?: ArrayList(),
                    mLiveData.value?.storyTree ?: StoryTree(null, ArrayList()))
        }
    }

    fun onVoteRejected(story: Int) {
        mLiveData.value = StoryViewState(mLiveData.value?.isLoading ?: false,
                mLiveData.value?.storyTitle ?: "",
                mRepository.isUserLoggedIn(),
                GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote),
                mLiveData.value?.tags ?: ArrayList(),
                mLiveData.value?.storyTree ?: StoryTree(null, ArrayList()))
    }

    fun onTagClick(storyActivity: StoryActivity, text: String) {
        var text = text
        if (text.contains(Regex("[а-яА-Я]"))) text = "ru--" + Translit.ru2lat(text)
        FilteredStoriesActivity.start(storyActivity, feedType, StoryFilter(text))
    }

    fun onShareClick(context: Context) {
        val link = mRepository.getShareStoryLink(mLiveData.value?.storyTree?.rootStory() ?: return)
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, link)
        sendIntent.type = "text/plain"
        context.startActivity(sendIntent)
    }
}