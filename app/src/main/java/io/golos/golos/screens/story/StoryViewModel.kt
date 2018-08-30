package io.golos.golos.screens.story

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.support.annotation.VisibleForTesting
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.repository.model.Tag
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.screens.stories.FilteredStoriesActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.*
import io.golos.golos.screens.userslist.UsersListActivity
import io.golos.golos.utils.*

class StoryViewModel : ViewModel() {
    private val mLiveData = MediatorLiveData<StoryViewState>()
    @VisibleForTesting
    var mRepository: Repository = Repository.get
    private lateinit var author: String
    private lateinit var permLink: String
    private var filter: StoryFilter? = null
    var blog: String? = null
    private lateinit var feedType: FeedType
    private lateinit var mInternetStatusNotifier: InternetStatusNotifier
    private val mImageClickEvents = MutableLiveData<ImageClickData>()
    public val imageDialogShowEvent: LiveData<ImageClickData> = mImageClickEvents


    fun onCreate(author: String,
                 permLink: String,
                 blog: String?,
                 feedType: FeedType,
                 filter: StoryFilter?,
                 internetStatusNotifier: InternetStatusNotifier) {
        this.author = author
        this.permLink = permLink
        this.blog = blog
        this.feedType = feedType
        this.filter = filter
        this.mInternetStatusNotifier = internetStatusNotifier

        mLiveData.removeSource(mRepository.getStories(feedType, filter))
        mLiveData.addSource(mRepository.getStories(feedType, filter)) {

            val storyItems = it
            it?.items?.find {
                it.rootStory()?.author == this.author
                        && it.rootStory()?.permlink == this.permLink
            }?.let {

                val mustHaveComments = it.rootStory()?.commentsCount ?: 0
                val commentsSize = it.comments().size
                var isLoading = false
                if (mustHaveComments > 0 && commentsSize == 0) isLoading = true

                mLiveData.value = StoryViewState(isLoading,
                        it.rootStory()?.title ?: "",
                        mRepository.isUserLoggedIn(),
                        storyItems?.error,
                        it.rootStory()?.tags ?: arrayListOf(),
                        it,
                        mRepository.isUserLoggedIn(),
                        mLiveData.value?.subscribeOnStoryAuthorStatus
                                ?: SubscribeStatus.UnsubscribedStatus,
                        mLiveData.value?.subscribeOnTagStatus
                                ?: SubscribeStatus.UnsubscribedStatus)

                this.blog = mLiveData.value?.storyTree?.rootStory()?.categoryName
            }
        }
        mLiveData.removeSource(mRepository.appUserData)
        mLiveData.addSource(mRepository.appUserData) {
            mLiveData.value = StoryViewState(mLiveData.value?.isLoading ?: false,
                    mLiveData.value?.storyTitle ?: "",
                    mRepository.isUserLoggedIn(),
                    mLiveData.value?.errorCode,
                    mLiveData.value?.tags ?: arrayListOf(),
                    mLiveData.value?.storyTree ?: StoryWithComments(null, ArrayList()),
                    mRepository.isUserLoggedIn(),
                    mLiveData.value?.subscribeOnStoryAuthorStatus
                            ?: SubscribeStatus.UnsubscribedStatus,
                    mLiveData.value?.subscribeOnTagStatus ?: SubscribeStatus.UnsubscribedStatus
            )
        }
        mLiveData.removeSource(mRepository.getCurrentUserSubscriptions())
        mLiveData.addSource(mRepository.getCurrentUserSubscriptions()) {
            if (it?.any { it.user.name == this.author } == true) {
                val followItem = it.find { it.user.name == this.author } ?: return@addSource
                mLiveData.value = StoryViewState(mLiveData.value?.isLoading ?: false,
                        mLiveData.value?.storyTitle ?: "",
                        mRepository.isUserLoggedIn(),
                        mLiveData.value?.errorCode,
                        mLiveData.value?.tags ?: arrayListOf(),
                        mLiveData.value?.storyTree ?: StoryWithComments(null, ArrayList()),
                        mRepository.isUserLoggedIn(),
                        followItem.status,
                        mLiveData.value?.subscribeOnTagStatus ?: SubscribeStatus.UnsubscribedStatus)
            }
        }
        mLiveData.removeSource(mRepository.getUserSubscribedTags())
        mLiveData.addSource(mRepository.getUserSubscribedTags(), {
            val tagItem = it?.find { it.name == this.blog }
            mLiveData.value = StoryViewState(mLiveData.value?.isLoading ?: false,
                    mLiveData.value?.storyTitle ?: "",
                    mRepository.isUserLoggedIn(),
                    mLiveData.value?.errorCode,
                    mLiveData.value?.tags ?: arrayListOf(),
                    mLiveData.value?.storyTree ?: StoryWithComments(null, ArrayList()),
                    mRepository.isUserLoggedIn(),
                    mLiveData.value?.subscribeOnStoryAuthorStatus
                            ?: SubscribeStatus.UnsubscribedStatus,
                    SubscribeStatus(tagItem != null, UpdatingState.DONE)
            )
        })
        mRepository.requestStoryUpdate(this.author, this.permLink, this.blog, feedType) { _, _ -> }

    }


    val liveData: LiveData<StoryViewState>
        get() {
            return mLiveData
        }

    fun onMainStoryTextClick(activity: Activity, text: String) {

    }

    data class ImageClickData(val position: Int, val images: List<String>)


    fun onMainStoryImageClick(src: String) {
        val images = mLiveData.value?.storyTree?.rootStory()?.parts
                ?.filter { it is ImageRow }
                ?.map { it as ImageRow }
                ?.map { it.src } ?: listOf()
        if (images.isEmpty()) images
                .toArrayList()
                .addAll(mLiveData.value?.storyTree?.rootStory()?.images
                        ?: arrayListOf())
        if (images.isEmpty()) return

        val position = images.indexOf(src)
        mImageClickEvents.value = ImageClickData(position, images)
    }

    val canUserVote: Boolean
        get() {
            return mRepository.isUserLoggedIn()

        }

    fun canUserUpVoteOnThis(story: StoryWrapper): Boolean {
        return story.story.userVotestatus != GolosDiscussionItem.UserVoteType.VOTED
    }

    fun onStoryVote(story: StoryWrapper, percent: Short) {
        if (story.updatingState == UpdatingState.UPDATING) return
        if (percent == 0.toShort()) mRepository.cancelVote(story)
        else {
            if (story.story.userVotestatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED
                    && percent < 0) {
                mRepository.vote(story, 0)
            } else {
                mRepository.vote(story, percent)
            }

        }
    }


    fun canUserWriteComments(): Boolean {
        return mRepository.isUserLoggedIn()
    }

    fun onWriteRootComment(ctx: Context) {
        if (canUserWriteComments()) {
            val story = mLiveData.value?.storyTree ?: return
            if (story.rootStory()?.isRootStory == true) {
                EditorActivity.startRootCommentEditor(ctx,
                        story,
                        feedType,
                        filter)

            } else {
                EditorActivity.startAnswerOnCommentEditor(ctx,
                        rootStory = story,
                        commentToAnswer = story.rootStory() ?: return,
                        feedType = feedType,
                        storyFilter = filter)
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

    fun onEditClick(ctx: Context, item: GolosDiscussionItem) {
        if (mRepository.isUserLoggedIn()
                && mRepository.appUserData.value?.userName == mLiveData.value?.storyTree?.rootStory()?.author) {

            mLiveData.value?.let {
                EditorActivity.startEditPostOrComment(ctx,
                        it.storyTree,
                        item,
                        feedType,
                        filter)
            }
        } else {
            showError(GolosError(ErrorCode.ERROR_AUTH, null, R.string.you_must_have_more_repo_for_action))
        }
    }

    public fun isPostEditable() = mLiveData.value?.storyTree?.storyWithState()?.isStoryEditable == true

    fun onVoteRejected() {
        showError(GolosError(ErrorCode.ERROR_AUTH, null, R.string.must_be_logged_in_for_this_action))
    }

    fun onTagClick(context: Context, text: String?) {
        FilteredStoriesActivity.start(context, text ?: return)
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
        mRepository.requestStoryUpdate(this.author, this.permLink, this.blog, feedType) { _, e -> }
    }

    fun onCommentClick(context: Context, comment: GolosDiscussionItem) {
        StoryActivity.start(context, comment.author,
                comment.categoryName,
                comment.permlink,
                FeedType.UNCLASSIFIED,
                null)
    }

    fun onSubscribeToBlogButtonClick() {
        if (!mRepository.isUserLoggedIn()) {
            showError(GolosError(ErrorCode.ERROR_AUTH, null, R.string.must_be_logged_in_for_this_action))
            return
        }
        if (!mInternetStatusNotifier.isAppOnline()) {
            showError(GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
            return
        }

        if (mLiveData.value?.subscribeOnStoryAuthorStatus?.isCurrentUserSubscribed == true)
            mRepository.unSubscribeOnUserBlog(mLiveData.value?.storyTree?.rootStory()?.author
                    ?: return, { _, e ->
                showError(e ?: return@unSubscribeOnUserBlog)
            })
        else if (mLiveData.value?.subscribeOnStoryAuthorStatus?.isCurrentUserSubscribed == false) {
            mRepository.subscribeOnUserBlog(mLiveData.value?.storyTree?.rootStory()?.author
                    ?: return, { _, e ->
                showError(e ?: return@subscribeOnUserBlog)
            })
        }
    }

    fun onSubscribeToMainTagClick() {
        val tag = mLiveData.value?.storyTree?.rootStory()?.categoryName ?: return
        if (mLiveData.value?.subscribeOnTagStatus?.isCurrentUserSubscribed == true) {
            mRepository.unSubscribeOnTag(Tag(tag, 0.0, 0L, 0L))
        } else if (mLiveData.value?.subscribeOnTagStatus?.isCurrentUserSubscribed == false) mRepository.subscribeOnTag(Tag(tag, 0.0, 0L, 0L))
    }

    fun onUserClick(context: Context, userName: String?) {
        UserProfileActivity.start(context, userName?.toLowerCase() ?: return)
    }

    private fun showError(error: GolosError) {
        mLiveData.value = StoryViewState(false,
                mLiveData.value?.storyTitle ?: "",
                mRepository.isUserLoggedIn(),
                error,
                mLiveData.value?.tags ?: arrayListOf(),
                mLiveData.value?.storyTree ?: StoryWithComments(null, ArrayList()),
                mRepository.isUserLoggedIn(),
                mLiveData.value?.subscribeOnStoryAuthorStatus ?: SubscribeStatus.UnsubscribedStatus,
                mLiveData.value?.subscribeOnTagStatus ?: SubscribeStatus.UnsubscribedStatus)
    }

    fun onStoryVotesClick(context: Context) {
        UsersListActivity.startToShowVoters(context, mLiveData.value?.storyTree?.rootStory()?.id
                ?: return)
    }

    fun onCommentVoteClick(activity: Activity, it: StoryWrapper) {
        UsersListActivity.startToShowVoters(activity, it.story.id)
    }

    fun onDestroy() {
        mLiveData.removeSource(mRepository.getStories(feedType, filter))
        mLiveData.removeSource(mRepository.appUserData)
        mLiveData.removeSource(mRepository.getCurrentUserSubscriptions())
        mLiveData.removeSource(mRepository.getUserSubscribedTags())
    }
}