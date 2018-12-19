package io.golos.golos.screens.story

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.common.primitives.Longs
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.ExchangeValues
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.repository.model.Tag
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.screens.stories.FilteredStoriesActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.stories.model.VoteChooserDescription
import io.golos.golos.screens.stories.model.VoteChooserType
import io.golos.golos.screens.story.model.*
import io.golos.golos.screens.userslist.UsersListActivity
import io.golos.golos.utils.*

private class ByPoplarityComparator : Comparator<GolosDiscussionItem> {
    override fun compare(o1: GolosDiscussionItem?, o2: GolosDiscussionItem?): Int {
        if (o1 == null) return -1
        if (o2 == null) return +1
        val first = o1.curatorPayoutValue + o1.totalPayoutValue + o1.pendingPayoutValue
        val second = o2.curatorPayoutValue + o2.totalPayoutValue + o2.pendingPayoutValue
        val result = -(first.compareTo(second))
        return if (result != 0) result
        else o1.netRshares.compareTo(o2.netRshares)
    }
}

private class ByNewComparator : Comparator<GolosDiscussionItem> {
    override fun compare(o1: GolosDiscussionItem?, o2: GolosDiscussionItem?): Int {
        return -Longs.compare(o1?.created ?: 0L, o2?.created ?: 0)
    }
}

private class ByOldComparator : Comparator<GolosDiscussionItem> {
    override fun compare(o1: GolosDiscussionItem?, o2: GolosDiscussionItem?): Int {
        return Longs.compare(o1?.created ?: 0L, o2?.created ?: 0)
    }
}

private class ByVotesComparator : Comparator<GolosDiscussionItem> {
    override fun compare(o1: GolosDiscussionItem?, o2: GolosDiscussionItem?): Int {
        if (o1 == null) return -1
        if (o2 == null) return +1
        return -(o1.upvotesNum).compareTo(o2.upvotesNum)
    }
}

private fun CommentsSortType.asComparator() = when (this) {
    CommentsSortType.POPULARITY -> ByPoplarityComparator()
    CommentsSortType.NEW_FIRST -> ByNewComparator()
    CommentsSortType.OLD_FIRST -> ByOldComparator()
    CommentsSortType.VOTES -> ByVotesComparator()
}


class DiscussionViewModel : ViewModel() {


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
    private var mLastVotingStoryId: Long = Long.MIN_VALUE
    private var mStoryWithComments: StoryWithComments? = null
    private val mChooserLiveData: MutableLiveData<VoteChooserDescription> = OneShotLiveData()
    private val mUnsubscribeConfirmalLiveData: MutableLiveData<String> = OneShotLiveData()

    val chooserLiveData: LiveData<VoteChooserDescription> = mChooserLiveData
    val unsubscribeConfirmalLiveData: LiveData<String> = mUnsubscribeConfirmalLiveData


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

    private fun onGolosUserSubscriptionsStateChanged() {
        val currentSubscriptions = mRepository.currentUserSubscriptions.value.orEmpty()

        val currentSubscriptionsStates = mRepository.currentUserSubscriptionsUpdateStatus.value.orEmpty()
        mSubscriptionsLiveData.value = mSubscriptionsLiveData
                .value
                ?.copy(subscribeOnStoryAuthorStatus = SubscribeStatus.create(currentSubscriptions.contains(author),
                        currentSubscriptionsStates[author] ?: UpdatingState.DONE))
    }

    fun onStart() {

        mStoryLiveData.addSource(mRepository.getStories(feedType, filter)) {

            it?.items?.find {
                it.rootStory.author == this.author
                        && it.rootStory.permlink == this.permLink
            }?.let {
                val currentSubscriptions = mRepository.currentUserSubscriptions.value.orEmpty()

                val currentSubscriptionsStates = mRepository.currentUserSubscriptionsUpdateStatus.value.orEmpty()
                mSubscriptionsLiveData.value = mSubscriptionsLiveData
                        .value
                        ?.copy(subscribeOnStoryAuthorStatus = SubscribeStatus.create(currentSubscriptions.contains(author),
                                currentSubscriptionsStates[author] ?: UpdatingState.DONE))

                mStoryWithComments = it

                val story = it.rootStory
                val mustHaveComments = story.commentsCount
                val commentsSize = it.comments.size
                var isLoading = false
                if (mustHaveComments > 0 && commentsSize == 0) isLoading = true
                val accounts = mRepository.getGolosUserAccountInfos().value.orEmpty()
                val voteStates = mRepository.votingStates.value.orEmpty()
                val currentUser = mRepository.appUserData.value
                val exchangeValues = mRepository.getExchangeLiveData().value
                        ?: ExchangeValues.nullValues
                val repostedPosts = mRepository.currentUserRepostedBlogEntries.value.orEmpty()
                val repostUpdateStates = mRepository.currentUserRepostStates.value.orEmpty()

                var rootStoryWrapper = createStoryWrapper(story, voteStates, accounts, repostedPosts, repostUpdateStates, currentUser, exchangeValues,
                        false, null)

                rootStoryWrapper = changeWrapperAccountInfoIfNeeded(feedType,
                        filter,
                        rootStoryWrapper,
                        mRepository.getGolosUserAccountInfos().value.orEmpty())

                val comments = it.getFlataned((mStoryLiveData.value?.commentsSortType
                        ?: CommentsSortType.POPULARITY).asComparator()).map {
                    createStoryWrapper(it, voteStates, accounts, repostedPosts, repostUpdateStates, currentUser, exchangeValues,
                            false, null)
                }

                mStoryLiveData.value = StoryViewState(
                        isLoading = isLoading,
                        storyTitle = it.rootStory.title,
                        tags = it.rootStory.tags,
                        showRepostButton = isRepostButtonNeedToBeShown(feedType, filter, mRepository.appUserData.value, it.rootStory),
                        storyTree = StoryWrapperWithComment(rootStoryWrapper, comments),
                        discussionType = if (story.isStory()) DiscussionType.STORY else DiscussionType.COMMENT,
                        canUserCommentThis = mRepository.isUserLoggedIn(),
                        commentsSortType = CommentsSortType.POPULARITY)

                this.blog = story.categoryName
            }
        }
        mStoryLiveData.addSource(mRepository.votingStates) { voteStates ->
            if (voteStates.isNullOrEmpty()) return@addSource
            val currentStoryTree = mStoryLiveData.value?.storyTree ?: return@addSource
            val errorMsg: GolosError? = voteStates.findLast { it.storyId == mLastVotingStoryId }?.error

            val rootStory = currentStoryTree.rootWrapper.copy(voteUpdatingState = voteStates.find {
                it.storyId == currentStoryTree.rootWrapper.story.id
            })
            val comments = currentStoryTree.comments.map { commentWrapper ->
                commentWrapper.copy(voteUpdatingState = voteStates.find { commentWrapper.story.id == it.storyId })
            }
            val newState = mStoryLiveData.value?.storyTree?.copy(rootWrapper = rootStory, comments = comments)
            if (newState != mStoryLiveData.value?.storyTree) mStoryLiveData.value = mStoryLiveData.value?.copy(storyTree = newState
                    ?: return@addSource, error = errorMsg)

        }

        mSubscriptionsLiveData.addSource(mRepository.getUserSubscribedTags()) {
            val tagItem = it?.find { it.name == this.blog }
            mSubscriptionsLiveData.value = mSubscriptionsLiveData.value?.copy(
                    subscribeOnTagStatus = SubscribeStatus(tagItem != null, UpdatingState.DONE))
        }
        mSubscriptionsLiveData.addSource(mRepository.currentUserSubscriptionsUpdateStatus) {
            onGolosUserSubscriptionsStateChanged()
        }

        fun createRepostState(): StoryViewState? {
            val reposts = mRepository.currentUserRepostedBlogEntries.value.orEmpty()
            val repostStates = mRepository.currentUserRepostStates.value.orEmpty()
            val currentState = mStoryLiveData.value
            return currentState?.copy(storyTree = currentState.storyTree.copy(
                    rootWrapper = changeRepostState(currentState.storyTree.rootWrapper, reposts, repostStates)))
        }

        mStoryLiveData.addSource(mRepository.currentUserRepostedBlogEntries) { repostedEntries ->
            if (repostedEntries == null) return@addSource
            val newState = createRepostState()
            if (newState != mStoryLiveData.value)
                mStoryLiveData.value = newState
        }
        mStoryLiveData.addSource(mRepository.currentUserRepostStates) { repostState ->
            if (repostState == null) return@addSource
            val newState = createRepostState()
            if (newState != mStoryLiveData.value)
                mStoryLiveData.value = newState
        }

        mRepository.requestStoryUpdate(this.author, this.permLink, this.blog, true, true, feedType) { _, _ -> }
    }

    fun onStop() {
        mStoryLiveData.removeSource(mRepository.getStories(feedType, filter))
        mSubscriptionsLiveData.removeSource(mRepository.getUserSubscribedTags())
        mSubscriptionsLiveData.removeSource(mRepository.currentUserSubscriptionsUpdateStatus)
        mStoryLiveData.removeSource(mRepository.votingStates)
        mStoryLiveData.removeSource(mRepository.currentUserRepostedBlogEntries)
        mStoryLiveData.removeSource(mRepository.currentUserRepostStates)

    }

    data class ImageClickData(val position: Int, val images: List<String>)

    fun changeCommentsSortType(newType: CommentsSortType) {
        val story = mStoryWithComments ?: return
        val currentStoryTree = mStoryLiveData.value?.storyTree ?: return
        if (newType == mStoryLiveData.value?.commentsSortType) return

        val accounts = mRepository.getGolosUserAccountInfos().value.orEmpty()
        val voteStates = mRepository.votingStates.value.orEmpty()
        val currentUser = mRepository.appUserData.value
        val exchangeValues = mRepository.getExchangeLiveData().value
                ?: ExchangeValues.nullValues
        val repostedPosts = mRepository.currentUserRepostedBlogEntries.value.orEmpty()
        val repostUpdateStates = mRepository.currentUserRepostStates.value.orEmpty()

        mStoryLiveData.value = mStoryLiveData.value?.copy(commentsSortType = newType,
                storyTree = StoryWrapperWithComment(currentStoryTree.rootWrapper,
                        comments = story.getFlataned(newType.asComparator()).map {

                            createStoryWrapper(it, voteStates,
                                    accounts, repostedPosts, repostUpdateStates, currentUser, exchangeValues, false, null)
                        }))

    }


    fun onMainStoryImageClick(src: String) {
        val images = mStoryLiveData.value?.storyTree?.rootWrapper?.story?.parts
                ?.asSequence()
                ?.filter { it is ImageRow }
                ?.map { it as ImageRow }
                ?.map { it.src }
                ?.toList()
                .orEmpty()
        if (images.isEmpty()) images
                .toArrayList()
                .addAll(mStoryLiveData.value?.storyTree?.rootWrapper?.story?.images.orEmpty())
        if (images.isEmpty()) return

        val position = images.indexOf(src)
        mImageClickEvents.value = ImageClickData(position, images)
    }

    val canUserVote: Boolean
        get() {
            return mRepository.isUserLoggedIn()

        }

    fun onStoryVote(story: StoryWrapper, percent: Short) {
        if (story.voteUpdatingState?.state == UpdatingState.UPDATING) return
        mLastVotingStoryId = story.story.id
        if (percent == 0.toShort()) mRepository.cancelVote(story.story)
        else {
            mRepository.vote(story.story, percent)
        }
    }


    fun canUserWriteComments(): Boolean {
        return mRepository.isUserLoggedIn()
    }

    fun onWriteRootComment(ctx: Context) {
        if (canUserWriteComments()) {
            val story = mStoryLiveData.value?.storyTree ?: return
            if (story.rootWrapper.story.isRootStory) {
                EditorActivity.startRootCommentEditor(ctx,
                        story.rootWrapper.story,
                        feedType,
                        filter)

            } else {
                EditorActivity.startAnswerOnCommentEditor(ctx,
                        rootStory = story.rootWrapper.story,
                        commentToAnswer = story.rootWrapper.story,
                        feedType = feedType,
                        storyFilter = filter)
            }

        }
    }

    fun onAnswerToComment(ctx: Context, item: GolosDiscussionItem) {
        if (mRepository.isUserLoggedIn()) {
            mStoryLiveData.value?.let {
                EditorActivity.startAnswerOnCommentEditor(ctx,
                        it.storyTree.rootWrapper.story,
                        item,
                        feedType,
                        filter)
            }
        } else {
            showError(GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_write_comment))
        }
    }

    fun repost(wrapper: StoryWrapper) {

    }

    fun onEditClick(ctx: Context, item: GolosDiscussionItem) {
        if (mRepository.isUserLoggedIn()
                && mRepository.appUserData.value?.name == item.author) {

            mStoryLiveData.value?.let {
                EditorActivity.startEditPostOrComment(ctx,
                        it.storyTree.rootWrapper.story,
                        item,
                        feedType,
                        filter)
            }
        }
    }

    public fun isPostEditable() = mStoryLiveData.value?.storyTree?.rootWrapper?.isStoryEditable == true

    fun onVoteRejected() {
        showError(GolosError(ErrorCode.ERROR_AUTH, null, R.string.must_be_logged_in_for_this_action))
    }

    fun onTagClick(context: Context, text: String?) {
        FilteredStoriesActivity.start(context, text ?: return)
    }

    fun onShareClick(context: Context) {
        val link = mRepository.getShareStoryLink(mStoryLiveData.value?.storyTree?.rootWrapper?.story
                ?: return)
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, link)
        sendIntent.type = "text/plain"
        context.startActivity(sendIntent)
    }

    fun requestRefresh() {
        mRepository.requestStoryUpdate(this.author, this.permLink, this.blog, true, true, feedType) { _, e -> }
    }

    fun onCommentClick(context: Context, comment: GolosDiscussionItem) {
        DiscussionActivity.start(context, comment.author,
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
            mUnsubscribeConfirmalLiveData.value = mStoryLiveData.value?.storyTree?.rootWrapper?.story?.author
                    ?: return
        else if (mSubscriptionsLiveData.value?.subscribeOnStoryAuthorStatus?.isCurrentUserSubscribed == false) {
            mRepository.subscribeOnGolosUserBlog(mStoryLiveData.value?.storyTree?.rootWrapper?.story?.author
                    ?: return) { _, e ->
                showError(e ?: return@subscribeOnGolosUserBlog)
            }
        }
    }

    fun unsubscribeFromUser(name: String) {
        if (!mRepository.isUserLoggedIn()) {
            showError(GolosError(ErrorCode.ERROR_AUTH, null, R.string.must_be_logged_in_for_this_action))
            return
        }
        if (!mInternetStatusNotifier.isAppOnline()) {
            showError(GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
            return
        }
        if (!mRepository.currentUserSubscriptions.value.orEmpty().contains(name)) return
        mRepository.unSubscribeFromGolosUserBlog(name) { _, e ->
            showError(e ?: return@unSubscribeFromGolosUserBlog)
        }
    }

    fun onSubscribeToMainTagClick() {
        val tag = mStoryLiveData.value?.storyTree?.rootWrapper?.story?.categoryName ?: return
        if (mSubscriptionsLiveData.value?.subscribeOnTagStatus?.isCurrentUserSubscribed == true) {
            mRepository.unSubscribeOnTag(Tag(tag, 0.0, 0L, 0L))
        } else if (mSubscriptionsLiveData.value?.subscribeOnTagStatus?.isCurrentUserSubscribed == false) mRepository.subscribeOnTag(Tag(tag, 0.0, 0L, 0L))
    }

    fun onUserClick(context: Context, userName: String?) {
        UserProfileActivity.start(context, userName?.toLowerCase() ?: return)
    }

    private fun showError(error: GolosError) {
        mStoryLiveData.value = mStoryLiveData.value?.copy(isLoading = false, error = error)
    }

    fun onCommentVoteClick(activity: Activity, it: StoryWrapper) {
        UsersListActivity.startToShowAllVoters(activity, it.story.id)
    }

    fun onStoryVote(storyId: Long, percent: Short) {
        val story = if (mStoryLiveData.value?.storyTree?.rootWrapper?.story?.id == storyId) mStoryLiveData.value?.storyTree?.rootWrapper
        else mStoryLiveData.value?.storyTree?.comments?.find { it.story.id == storyId }
        story?.let { onStoryVote(it, percent) }
    }

    fun onLikeClick(storyId: Long) {
        val story = if (mStoryLiveData.value?.storyTree?.rootWrapper?.story?.id == storyId) mStoryLiveData.value?.storyTree?.rootWrapper
        else mStoryLiveData.value?.storyTree?.comments?.find { it.story.id == storyId }

        story ?: return

        if (story.voteUpdatingState?.state == UpdatingState.UPDATING) return
        if (story.voteStatus == GolosDiscussionItem.UserVoteType.VOTED) {
            mChooserLiveData.value = VoteChooserDescription(storyId, VoteChooserType.CANCEL_VOTE)
        } else {
            val defaultVotePower = Repository.get.appSettings.value?.defaultUpvotePower
                    ?: Byte.MIN_VALUE
            if (defaultVotePower < 1) mChooserLiveData.value = VoteChooserDescription(storyId, VoteChooserType.LIKE)
            else onStoryVote(storyId, defaultVotePower.toShort())
        }
    }

    fun onDizlikeClick(storyId: Long) {
        val story = if (mStoryLiveData.value?.storyTree?.rootWrapper?.story?.id == storyId) mStoryLiveData.value?.storyTree?.rootWrapper
        else mStoryLiveData.value?.storyTree?.comments?.find { it.story.id == storyId } ?: return

        story ?: return

        if (story.voteUpdatingState?.state == UpdatingState.UPDATING) return
        if (story.voteStatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED) {
            mChooserLiveData.value = VoteChooserDescription(storyId, VoteChooserType.CANCEL_VOTE)
        } else {
            mChooserLiveData.value = VoteChooserDescription(storyId, VoteChooserType.DIZLIKE)
        }
    }

    fun onDownVotersClick(rootWrapper: StoryWrapper, discussionActivity: Activity) {
        UsersListActivity.startToShowDownVoters(discussionActivity, mStoryLiveData.value?.storyTree?.rootWrapper?.story?.id
                ?: return)
    }


    fun onUpvotersClick(rootWrapper: StoryWrapper, discussionActivity: Activity) {
        UsersListActivity.startToShowUpVoters(discussionActivity, mStoryLiveData.value?.storyTree?.rootWrapper?.story?.id
                ?: return)
    }

    fun onRootStoryReblog() {
        val wrapper = mStoryLiveData.value?.storyTree?.rootWrapper ?: return
        if (wrapper.repostStatus == UpdatingState.UPDATING) return
        if (wrapper.isPostReposted) return
        mRepository.repost(wrapper.story) { _, e ->
            if (e != null) mStoryLiveData.value = mStoryLiveData.value?.copy(error = e)
        }
    }

}