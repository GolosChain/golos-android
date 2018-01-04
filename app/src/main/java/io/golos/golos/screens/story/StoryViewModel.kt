package io.golos.golos.screens.story

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.support.annotation.VisibleForTesting
import android.widget.ImageView
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.StoryFilter
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.profile.ProfileActivity
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
    @VisibleForTesting
    var mRepository: Repository = Repository.get
    private lateinit var author: String
    private lateinit var permLink: String
    private var filter: StoryFilter? = null
    var blog: String? = null
    private lateinit var feedType: FeedType

    fun onCreate(author: String,
                 permLink: String,
                 blog: String?,
                 feedType: FeedType,
                 filter: StoryFilter?) {
        this.author = author
        this.permLink = permLink
        this.blog = blog
        this.feedType = feedType
        this.filter = filter
        mLiveData.removeSource(mRepository.getStories(feedType, filter))
        mLiveData.addSource(mRepository.getStories(feedType, filter)) {
            val storyItems = it
            it?.
                    items?.
                    filter {
                        it.rootStory()?.author == this.author
                                && it.rootStory()?.permlink == this.permLink
                    }?.
                    forEach {
                        mLiveData.value = StoryViewState(false,
                                it.rootStory()?.title ?: "",
                                mLiveData.value?.isStoryCommentButtonShown ?: false,
                                storyItems?.error,
                                it.rootStory()?.tags ?: ArrayList(),
                                it)
                        this.blog = mLiveData.value?.storyTree?.rootStory()?.categoryName
                    }
        }
        mLiveData.removeSource(mRepository.getCurrentUserDataAsLiveData())
        mLiveData.addSource(mRepository.getCurrentUserDataAsLiveData()) {
            mLiveData.value = StoryViewState(mLiveData.value?.isLoading ?: false,
                    mLiveData.value?.storyTitle ?: "",
                    it?.userName != null,
                    mLiveData.value?.errorCode,
                    mLiveData.value?.tags ?: ArrayList(),
                    mLiveData.value?.storyTree ?: StoryTree(null, ArrayList()),
                    mRepository.isUserLoggedIn())
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
                        feedType,
                        filter)
            }
        }
    }

    fun onAnswerToComment(ctx: Context, item: GolosDiscussionItem) {
        if (mRepository.isUserLoggedIn()) {
            mLiveData.value?.let {
                EditorActivity.startAnswerOnCommentEditor(ctx,
                        it.storyTree,
                        item,
                        feedType,
                        filter)
            }
        } else {
            showError(GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_write_comment))
        }
    }

    fun onVoteRejected(story: Int) {
        showError(GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
    }

    fun onTagClick(context: Context, text: String?) {
        var text = text
        var feedType: FeedType = feedType
        if (feedType == FeedType.BLOG
                || feedType == FeedType.COMMENTS
                || feedType == FeedType.PERSONAL_FEED
                || feedType == FeedType.UNCLASSIFIED
                || feedType == FeedType.PROMO) feedType = FeedType.NEW
        if (text?.contains(Regex("[а-яА-Я]")) == true) text = "ru--" + Translit.ru2lat(text ?: return)
        FilteredStoriesActivity.start(context, feedType, StoryFilter(text ?: return))
    }

    fun onShareClick(context: Context) {
        val link = mRepository.getShareStoryLink(mLiveData.value?.storyTree?.rootStory() ?: return)
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, link)
        sendIntent.type = "text/plain"
        context.startActivity(sendIntent)
    }

    fun requestRefresh() {
        mRepository.requestStoryUpdate(this.author, this.permLink, this.blog, feedType)
    }

    fun onCommentClick(context: Context, comment: GolosDiscussionItem) {
        StoryActivity.start(context, comment.author,
                comment.categoryName,
                comment.permlink,
                FeedType.UNCLASSIFIED,
                null)
    }

    fun onSubscribeButtonClick() {
        if (!mRepository.isUserLoggedIn()) {
            showError(GolosError(ErrorCode.ERROR_AUTH, null, R.string.must_be_logged_in_for_this_action))
            return
        }
        if (!App.isAppOnline()) {
            showError(GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
            return
        }

        if (mLiveData.value?.storyTree?.userSubscribeUpdatingStatus?.isCurrentUserSubscribed == true)
            mRepository.unFollow(mLiveData.value?.storyTree?.rootStory()?.author ?: return, { _, e ->
                showError(e ?: return@unFollow)
            })
        else if (mLiveData.value?.storyTree?.userSubscribeUpdatingStatus?.isCurrentUserSubscribed == false) {
            mRepository.follow(mLiveData.value?.storyTree?.rootStory()?.author ?: return, { _, e ->
                showError(e ?: return@follow)
            })
        }
    }

    fun onUserClick(context: Context, userName: String?) {
        ProfileActivity.start(context, userName?.toLowerCase() ?: return)
    }

    private fun showError(error: GolosError) {
        mLiveData.value = StoryViewState(false,
                mLiveData.value?.storyTitle ?: "",
                mRepository.isUserLoggedIn(),
                error,
                mLiveData.value?.tags ?: ArrayList(),
                mLiveData.value?.storyTree ?: StoryTree(null, ArrayList()),
                mRepository.isUserLoggedIn())
    }
}