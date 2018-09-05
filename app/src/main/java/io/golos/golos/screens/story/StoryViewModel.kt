package io.golos.golos.screens.story

import android.app.Activity
import android.arch.lifecycle.*
import android.content.Context
import android.content.Intent
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.ApplicationUser
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

class StoryViewModel : ViewModel(), Observer<ApplicationUser> {


    private val mStoryLiveData = MediatorLiveData<StoryViewState>()
    private val mSubscriptionsLiveData = MediatorLiveData<StorySubscriptionBlockState>()
    private lateinit var mRepository: Repository
    private lateinit var author: String
    private lateinit var permLink: String
    private var filter: StoryFilter? = null
    var blog: String? = null
    private lateinit var feedType: FeedType
    private lateinit var mInternetStatusNotifier: InternetStatusNotifier
    private val mImageClickEvents = MutableLiveData<ImageClickData>()
    public val imageDialogShowEvent: LiveData<ImageClickData> = mImageClickEvents
    private var curentUserName: String? = null


    fun onCreate(author: String,
                 permLink: String,
                 blog: String?,
                 feedType: FeedType,
                 filter: StoryFilter?,
                 internetStatusNotifier: InternetStatusNotifier,
                 repository: Repository = Repository.get) {
        this.author = author
        this.permLink = permLink
        this.blog = blog
        this.feedType = feedType
        this.filter = filter
        this.mInternetStatusNotifier = internetStatusNotifier
        mRepository = repository
        mStoryLiveData.value = StoryViewState()
        mSubscriptionsLiveData.value = StorySubscriptionBlockState(false, SubscribeStatus.UnsubscribedStatus, SubscribeStatus.UnsubscribedStatus)
    }


    val storyLiveData: LiveData<StoryViewState>
        get() {
            return mStoryLiveData
        }
    val subscriptionLiveData: LiveData<StorySubscriptionBlockState>
        get() {
            return mSubscriptionsLiveData
        }

    fun onMainStoryTextClick(activity: Activity, text: String) {

    }

    override fun onChanged(t: ApplicationUser?) {
        if (t == null || !t.isLogged) {
            mSubscriptionsLiveData.removeSource(mRepository.getGolosUserSubscriptions(curentUserName
                    ?: return))
            mSubscriptionsLiveData.value = StorySubscriptionBlockState(false, SubscribeStatus.UnsubscribedStatus, SubscribeStatus.UnsubscribedStatus)
            curentUserName = null
        } else {
            curentUserName = t.name
            mStoryLiveData.value = mStoryLiveData.value?.copy(isStoryCommentButtonShown = true)

            mSubscriptionsLiveData.addSource(mRepository.getGolosUserSubscriptions(t.name)) {
                onGolosUserSubscriptionsStateChanged()
            }
        }
    }

    private fun onGolosUserSubscriptionsStateChanged() {
        val currentSubscriptions = mRepository.getGolosUserSubscriptions(mRepository.appUserData.value?.name
                ?: return).value.orEmpty()
        val currentSubscriptionsStates = mRepository.currentUserSubscriptionsUpdateStatus.value.orEmpty()
        mSubscriptionsLiveData.value = mSubscriptionsLiveData
                .value
                ?.copy(subscribeOnStoryAuthorStatus = SubscribeStatus.create(currentSubscriptions.contains(author),
                        currentSubscriptionsStates[author] ?: UpdatingState.DONE))
    }

    fun onStart() {

        mStoryLiveData.addSource(mRepository.getStories(feedType, filter)) {

            it?.items?.find {
                it.rootStory()?.author == this.author
                        && it.rootStory()?.permlink == this.permLink
            }?.let {
                val story = it.rootStory() ?: return@let
                val mustHaveComments = story.commentsCount
                val commentsSize = it.comments().size
                var isLoading = false
                if (mustHaveComments > 0 && commentsSize == 0) isLoading = true

                mStoryLiveData.value = mStoryLiveData.value?.copy(
                        isLoading = isLoading,
                        storyTitle = it.rootStory()?.title.orEmpty(),
                        tags = it.rootStory()?.tags.orEmpty().toArrayList(),
                        storyTree = it,
                        isStoryCommentButtonShown = mRepository.isUserLoggedIn())
                this.blog = mStoryLiveData.value?.storyTree?.rootStory()?.categoryName
            }
        }

        mRepository.appUserData.observeForever(this)

        mSubscriptionsLiveData.addSource(mRepository.getUserSubscribedTags()) {
            val tagItem = it?.find { it.name == this.blog }
            mSubscriptionsLiveData.value = mSubscriptionsLiveData.value?.copy(
                    subscribeOnTagStatus = SubscribeStatus(tagItem != null, UpdatingState.DONE))
        }
        mSubscriptionsLiveData.addSource(mRepository.currentUserSubscriptionsUpdateStatus) { onGolosUserSubscriptionsStateChanged() }
        mRepository.requestStoryUpdate(this.author, this.permLink, this.blog, feedType) { _, _ -> }
    }

    fun onStop() {
        mStoryLiveData.removeSource(mRepository.getStories(feedType, filter))
        mStoryLiveData.removeSource(mRepository.appUserData)
        mSubscriptionsLiveData.removeSource(mRepository.getGolosUserSubscriptions(mRepository.appUserData.value?.name.orEmpty()))
        mSubscriptionsLiveData.removeSource(mRepository.getUserSubscribedTags())
        mSubscriptionsLiveData.removeSource(mRepository.currentUserSubscriptionsUpdateStatus)
        mRepository.appUserData.removeObserver(this)
    }

    data class ImageClickData(val position: Int, val images: List<String>)


    fun onMainStoryImageClick(src: String) {
        val images = mStoryLiveData.value?.storyTree?.rootStory()?.parts
                ?.filter { it is ImageRow }
                ?.map { it as ImageRow }
                ?.map { it.src } ?: listOf()
        if (images.isEmpty()) images
                .toArrayList()
                .addAll(mStoryLiveData.value?.storyTree?.rootStory()?.images
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
            val story = mStoryLiveData.value?.storyTree ?: return
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
            mStoryLiveData.value?.let {
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
                && mRepository.appUserData.value?.name == mStoryLiveData.value?.storyTree?.rootStory()?.author) {

            mStoryLiveData.value?.let {
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

    public fun isPostEditable() = mStoryLiveData.value?.storyTree?.storyWithState()?.isStoryEditable == true

    fun onVoteRejected() {
        showError(GolosError(ErrorCode.ERROR_AUTH, null, R.string.must_be_logged_in_for_this_action))
    }

    fun onTagClick(context: Context, text: String?) {
        FilteredStoriesActivity.start(context, text ?: return)
    }

    fun onShareClick(context: Context) {
        val link = mRepository.getShareStoryLink(mStoryLiveData.value?.storyTree?.rootStory()
                ?: return)
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

        if (mSubscriptionsLiveData.value?.subscribeOnStoryAuthorStatus?.isCurrentUserSubscribed == true)
            mRepository.unSubscribeFromGolosUserBlog(mStoryLiveData.value?.storyTree?.rootStory()?.author
                    ?: return) { _, e ->
                showError(e ?: return@unSubscribeFromGolosUserBlog)
            }
        else if (mSubscriptionsLiveData.value?.subscribeOnStoryAuthorStatus?.isCurrentUserSubscribed == false) {
            mRepository.subscribeOnGolosUserBlog(mStoryLiveData.value?.storyTree?.rootStory()?.author
                    ?: return) { _, e ->
                showError(e ?: return@subscribeOnGolosUserBlog)
            }
        }
    }

    fun onSubscribeToMainTagClick() {
        val tag = mStoryLiveData.value?.storyTree?.rootStory()?.categoryName ?: return
        if (mSubscriptionsLiveData.value?.subscribeOnTagStatus?.isCurrentUserSubscribed == true) {
            mRepository.unSubscribeOnTag(Tag(tag, 0.0, 0L, 0L))
        } else if (mSubscriptionsLiveData.value?.subscribeOnTagStatus?.isCurrentUserSubscribed == false) mRepository.subscribeOnTag(Tag(tag, 0.0, 0L, 0L))
    }

    fun onUserClick(context: Context, userName: String?) {
        UserProfileActivity.start(context, userName?.toLowerCase() ?: return)
    }

    private fun showError(error: GolosError) {
        mStoryLiveData.value = mStoryLiveData.value?.copy(isLoading = false, errorCode = error)
    }

    fun onStoryVotesClick(context: Context) {
        UsersListActivity.startToShowVoters(context, mStoryLiveData.value?.storyTree?.rootStory()?.id
                ?: return)
    }

    fun onCommentVoteClick(activity: Activity, it: StoryWrapper) {
        UsersListActivity.startToShowVoters(activity, it.story.id)
    }

}