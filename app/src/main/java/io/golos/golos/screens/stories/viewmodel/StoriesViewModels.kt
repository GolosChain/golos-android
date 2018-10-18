package io.golos.golos.screens.stories.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Handler
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.repository.KnifeHtmlizer
import io.golos.golos.repository.Repository
import io.golos.golos.repository.UserSettingsRepository
import io.golos.golos.repository.model.*
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.screens.stories.FilteredStoriesActivity
import io.golos.golos.screens.stories.adapters.FeedCellSettings
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.stories.model.NSFWStrategy
import io.golos.golos.screens.story.DiscussionActivity
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.screens.userslist.UsersListActivity
import io.golos.golos.utils.*
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by yuri on 01.11.17.
 */
object StoriesModelFactory {

    fun getStoriesViewModel(type: FeedType,
                            activity: FragmentActivity?,
                            fragment: Fragment?,
                            filter: StoryFilter?): DiscussionsViewModel {

        val provider = if (activity != null) ViewModelProviders.of(activity)
        else {
            if (fragment == null) throw IllegalStateException("activity or fragment must be not null")
            else ViewModelProviders.of(fragment)
        }

        val viewModel: DiscussionsViewModel


        viewModel = when (type) {
            FeedType.NEW -> provider.get(NewViewModel::class.java)
            FeedType.ACTUAL -> provider.get(ActualViewModle::class.java)
            FeedType.POPULAR -> provider.get(PopularViewModel::class.java)
            FeedType.PROMO -> provider.get(PromoViewModel::class.java)
            FeedType.PERSONAL_FEED -> provider.get(FeedViewModel::class.java)
            FeedType.BLOG -> provider.get(BlogViewModel::class.java)
            FeedType.COMMENTS -> provider.get(CommentsViewModel::class.java)
            FeedType.ANSWERS -> provider.get(AnswersViewModel::class.java)
            else -> throw IllegalStateException(" $type is unsupported")
        }
        if ((viewModel is FeedViewModel
                        || viewModel is CommentsViewModel
                        || viewModel is BlogViewModel) && filter == null) {
        } else {
            viewModel.onCreate(object : InternetStatusNotifier {
                override fun isAppOnline(): Boolean {
                    return App.isAppOnline()
                }
            }, filter)
        }
        return viewModel
    }
}

data class DiscussionsViewState(val isLoading: Boolean,
                                val showRefreshButton: Boolean,
                                val items: List<StoryWrapper>,
                                val error: GolosError?,
                                val fullscreenMessage: Int?)

class CommentsViewModel : FeedViewModel() {
    override val type: FeedType
        get() = FeedType.COMMENTS

}

class BlogViewModel : FeedViewModel() {
    override val type: FeedType
        get() = FeedType.BLOG
}

class AnswersViewModel : FeedViewModel() {
    override val type: FeedType
        get() = FeedType.ANSWERS
}

open class FeedViewModel : DiscussionsViewModel() {
    override val type: FeedType
        get() = FeedType.PERSONAL_FEED

    override fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        if (filter?.userNameFilter != null && visibleToUser) {
            super.onChangeVisibilityToUser(visibleToUser)
        }
    }

    override fun onSwipeToRefresh() {
        if (filter?.userNameFilter == null) return
        super.onSwipeToRefresh()
    }

    override fun onScrollToTheEnd() {
        if (filter?.userNameFilter == null) return
        super.onScrollToTheEnd()
    }

    override fun onNewItems(items: StoriesFeed): DiscussionsViewState {
        val state = super.onNewItems(items)
        return state.copy(fullscreenMessage = if (items.items.size.or(0) == 0) R.string.nothing_here else null)
    }
}


class NewViewModel : DiscussionsViewModel() {
    override val type: FeedType
        get() = FeedType.NEW
}

class ActualViewModle : DiscussionsViewModel() {
    override val type: FeedType
        get() = FeedType.ACTUAL
}

class PopularViewModel : DiscussionsViewModel() {
    override val type: FeedType
        get() = FeedType.POPULAR
}

class PromoViewModel : DiscussionsViewModel() {
    override val type: FeedType
        get() = FeedType.PROMO
}


abstract class DiscussionsViewModel : ViewModel() {
    protected abstract val type: FeedType
    private val mDiscussionsLiveData: MediatorLiveData<DiscussionsViewState> = MediatorLiveData()
    private val mFeedSettingsLiveData: MediatorLiveData<FeedCellSettings> = MediatorLiveData()
    private val mRepository = Repository.get
    private val isUpdating = AtomicBoolean(false)
    private var isVisibleToUser: Boolean = false
    protected var filter: StoryFilter? = null
    private lateinit var internetStatusNotifier: InternetStatusNotifier
    private var mObserver: Observer<Boolean>? = null
    private val updateSize = 20


    val discussionsLiveData: LiveData<DiscussionsViewState>
        get() {
            return mDiscussionsLiveData
        }
    val cellViewSettingLiveData: LiveData<FeedCellSettings>
        get() {
            return mFeedSettingsLiveData
        }

    fun onCreate(internetStatusNotifier: InternetStatusNotifier, filter: StoryFilter?) {
        this.filter = filter
        this.internetStatusNotifier = internetStatusNotifier

    }

    fun onStart() {
        mDiscussionsLiveData.removeSource(mRepository.getStories(type, filter))
        mDiscussionsLiveData.addSource(mRepository.getStories(type, filter)) {
            mExecutor.execute {
                val newState = onNewItems(it ?: return@execute)
                propogateNewState(newState)
            }
        }

        mDiscussionsLiveData.addSource(mRepository.getGolosUserAccountInfos()) { usersMap ->
            usersMap ?: return@addSource

            mExecutor.execute {
                mDiscussionsLiveData.value?.items ?: return@execute

                val newStories = mDiscussionsLiveData.value?.items?.map {
                    val story = it.story
                    it.copy(authorAccountInfo = if (story.rebloggedBy.isNotEmpty()) usersMap[story.rebloggedBy] else usersMap[story.author])
                }.orEmpty()

                val newState = mDiscussionsLiveData.value?.copy(items = newStories)
                propogateNewState(newState)
            }
        }

        mDiscussionsLiveData.addSource(mRepository.votingStates) { voteStates ->
            if (voteStates.isNullOrEmpty()) return@addSource
            mExecutor.execute {
                val currentStories = mDiscussionsLiveData.value?.items ?: return@execute
                val map = voteStates.associateBy { it.storyId }
                val storyIds = currentStories.asSequence().map { it.story.id }
                if (!storyIds.any { map.containsKey(it) }) return@execute

                var errorMsg: GolosError? = null

                val newState = mDiscussionsLiveData.value?.copy(items = currentStories.map {
                    val voteStateForItem = map[it.story.id] ?: return@map it
                    if (voteStateForItem.error != null) {
                        errorMsg = voteStateForItem.error
                    }
                    it.copy(voteUpdatingState = voteStateForItem)
                }, error = errorMsg.takeIf { it != mDiscussionsLiveData.value?.error })
                propogateNewState(newState)
            }
        }

        mDiscussionsLiveData.addSource(mRepository.appUserData) { applicationUser ->
            mExecutor.execute {
                val currentStories = mDiscussionsLiveData.value?.items ?: return@execute
                val newState = if (applicationUser == null) {
                    mDiscussionsLiveData.value?.copy(items = currentStories
                            .map {
                                it.copy(voteUpdatingState = null,
                                        voteStatus = GolosDiscussionItem.UserVoteType.NOT_VOTED_OR_ZERO_WEIGHT)
                            })
                } else {
                    mDiscussionsLiveData.value?.copy(items = currentStories
                            .map {
                                it.copy(voteStatus = it.story.isUserVotedOnThis(applicationUser.name))
                            })
                }

                propogateNewState(newState)
            }

        }

        mDiscussionsLiveData.addSource(mRepository.getExchangeLiveData()) { t: ExchangeValues? ->
            mExecutor.execute {
                val currentStories = mDiscussionsLiveData.value?.items ?: return@execute
                val newState = mDiscussionsLiveData.value?.copy(items = currentStories.map {
                    it.copy(exchangeValues = t ?: return@execute)
                })
                propogateNewState(newState)
            }
        }

        if (mObserver == null) {
            mObserver = Observer {
                mFeedSettingsLiveData.value = getFeedModeSettings()
            }
            Repository.get.userSettingsRepository.isStoriesCompactMode().observeForever(mObserver
                    ?: return)
            Repository.get.userSettingsRepository.isImagesShown().observeForever(mObserver
                    ?: return)
            Repository.get.userSettingsRepository.isNSFWShow().observeForever(mObserver ?: return)

            Repository.get.userSettingsRepository.getCurrency().observeForever {
                mFeedSettingsLiveData.value = getFeedModeSettings()
            }
            Repository.get.userSettingsRepository.getBountDisplay().observeForever {
                mFeedSettingsLiveData.value = getFeedModeSettings()
            }
        }
    }


    fun onStop() {
        mDiscussionsLiveData.removeSource(mRepository.getStories(type, filter))
        mDiscussionsLiveData.removeSource(mRepository.getGolosUserAccountInfos())
        mDiscussionsLiveData.removeSource(mRepository.votingStates)
        mDiscussionsLiveData.removeSource(mRepository.appUserData)
        mDiscussionsLiveData.removeSource(mRepository.getExchangeLiveData())

        if (mObserver != null) {
            Repository.get.userSettingsRepository.isStoriesCompactMode().removeObserver(mObserver
                    ?: return)
            Repository.get.userSettingsRepository.isImagesShown().removeObserver(mObserver
                    ?: return)
            Repository.get.userSettingsRepository.isNSFWShow().removeObserver(mObserver ?: return)
            mObserver = null
        }
    }

    private fun propogateNewState(state: DiscussionsViewState?) {
        if (state == mDiscussionsLiveData.value) return
        mHandler.post {
            mDiscussionsLiveData.value = state
        }
    }

    companion object {
        private val mExecutor = Executors.newSingleThreadExecutor()
        private val mHandler = Handler()
    }

    protected open fun onNewItems(items: StoriesFeed): DiscussionsViewState {
        val usersMap = mRepository.getGolosUserAccountInfos().value.orEmpty()
        val currentUser = mRepository.appUserData.value ?: ApplicationUser("", false)
        val voteStatuses = mRepository.votingStates.value.orEmpty()
        val exchangeValues = mRepository.getExchangeLiveData().value ?: ExchangeValues.nullValues

        val state = DiscussionsViewState(false,
                !items.isFeedActual,
                items.items.map {
                    createStoryWrapper(it.rootStory, voteStatuses, usersMap, currentUser, exchangeValues, true, KnifeHtmlizer)
                },
                null, null)

        mRepository.requestUsersAccountInfoUpdate(items.items.asSequence()
                .map { it.rootStory.author }
                .plus(items.items.asSequence().filter { it.rootStory.rebloggedBy.isNotEmpty() }.map { it.rootStory.rebloggedBy })
                .filter { storyAuthor -> !usersMap.containsKey(storyAuthor) }.toList())
        return state
    }

    open fun onSwipeToRefresh() {

        if (!internetStatusNotifier.isAppOnline()) {
            setAppOffline()
            return
        }
        if (mDiscussionsLiveData.value?.isLoading == false) {
            mDiscussionsLiveData.value = mDiscussionsLiveData.value?.copy(isLoading = true, error = null, showRefreshButton = false)

            mRepository.requestStoriesListUpdate(updateSize, type, filter, null, null) { _, _ ->

            }
            isUpdating.set(true)
        }
    }

    private fun createHashFromList(list: List<StoryWrapper>) = list.foldRight(0) { wrapper, accumulator -> accumulator + wrapper.hashCode() }


    private fun compareStates(new: DiscussionsViewState?, old: DiscussionsViewState?): Boolean {
        if (new != null && old == null) return false
        if (new == null && old != null) return false
        if (new.isNull() && old.isNull()) return true
        return new === old
    }


    open fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        this.isVisibleToUser = visibleToUser
        if (visibleToUser) {
            if (!internetStatusNotifier.isAppOnline()) {
                setAppOffline()
                return
            }
            mHandler.post {
                mExecutor.execute {
                    if (mDiscussionsLiveData.value?.items.isNullOrEmpty()) {
                        if (isUpdating.get()) return@execute
                        mHandler.post {
                            mDiscussionsLiveData.value = DiscussionsViewState(true, false, emptyList(), null, null)
                            mRepository.requestStoriesListUpdate(updateSize, type, filter, null, null) { _, _ -> }
                            isUpdating.set(true)
                        }
                    }
                }
            }
        }
    }

    private fun setAppOffline() {
        mDiscussionsLiveData.value = mDiscussionsLiveData.value?.copy(isLoading = false, error = GolosError(ErrorCode.ERROR_NO_CONNECTION,
                null,
                R.string.no_internet_connection))
    }

    private fun getFeedModeSettings() = FeedCellSettings(Repository.get.userSettingsRepository.isStoriesCompactMode().value == false,
            Repository.get.userSettingsRepository.isImagesShown().value == true,
            NSFWStrategy(Repository.get.userSettingsRepository.isNSFWShow().value == true,
                    Pair(mRepository.isUserLoggedIn(), mRepository.appUserData.value?.name.orEmpty())),
            Repository.get.userSettingsRepository.getCurrency().value
                    ?: UserSettingsRepository.GolosCurrency.USD,
            Repository.get.userSettingsRepository.getBountDisplay().value
                    ?: UserSettingsRepository.GolosBountyDisplay.THREE_PLACES
    )

    open fun onScrollToTheEnd() {
        if (!internetStatusNotifier.isAppOnline()) {
            setAppOffline()
            return
        }
        if (mDiscussionsLiveData.value?.items?.size ?: 0 < 1) return

        isUpdating.set(true)
        if (mDiscussionsLiveData.value?.isLoading == false) {
            mDiscussionsLiveData.value = mDiscussionsLiveData.value?.copy(isLoading = true)
        }
        val lastStory = mDiscussionsLiveData.value?.items?.last()?.story ?: return

        mRepository.requestStoriesListUpdate(updateSize,
                type,
                filter,
                lastStory.author, lastStory.permlink) { _, _ -> }
    }

    open fun onCardClick(it: StoryWrapper, context: Context) {
        DiscussionActivity.start(context, it.story.author,
                it.story.categoryName,
                it.story.permlink,
                type,
                filter)
    }

    open fun onCommentsClick(it: StoryWrapper, context: Context) {

        DiscussionActivity.start(context, it.story.author,
                it.story.categoryName,
                it.story.permlink,
                type,
                filter,
                true)
    }

    open fun onShareClick(it: StoryWrapper, context: Context) {
        val link = mRepository.getShareStoryLink(it.story)
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, link)
        sendIntent.type = "text/plain"
        context.startActivity(sendIntent)
    }

    open fun vote(it: StoryWrapper, vote: Short) {
        if (!internetStatusNotifier.isAppOnline()) {
            setAppOffline()
            return
        }
        if (!mRepository.isUserLoggedIn()) {
            return
        }
        val voteStatus = it.voteUpdatingState
        if (voteStatus?.state == UpdatingState.UPDATING) return
        if (mRepository.isUserLoggedIn()) {
            mRepository.vote(it.story, vote)
        } else {
            mDiscussionsLiveData.value =
                    mDiscussionsLiveData.value?.copy(error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
        }
    }

    fun onReblogAuthorClick(story: StoryWrapper, activity: FragmentActivity) {

        val reblogger = story.story.rebloggedBy
        if (reblogger.isEmpty()) return
        UserProfileActivity.start(activity, reblogger)
    }

    open fun reblog(it: StoryWrapper) {
        Timber.e("on reblog $it")
    }

    open fun cancelVote(it: StoryWrapper) {
        if (!internetStatusNotifier.isAppOnline()) {
            setAppOffline()
            return
        }
        if (mRepository.isUserLoggedIn()) {
            mRepository.cancelVote(it.story)
        } else {
            mDiscussionsLiveData.value = mDiscussionsLiveData.value?.copy(error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
        }
    }

    fun canVote(): Boolean {
        return mRepository.isUserLoggedIn()
    }

    fun onVoteRejected(it: StoryWrapper) {
        mDiscussionsLiveData.value = mDiscussionsLiveData.value?.copy(error = GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
    }

    fun onBlogClick(story: StoryWrapper, context: Context?) {

        context?.let {
            var feedType: FeedType = type
            if (feedType == FeedType.BLOG
                    || feedType == FeedType.COMMENTS
                    || feedType == FeedType.PERSONAL_FEED
                    || feedType == FeedType.UNCLASSIFIED
                    || feedType == FeedType.PROMO) feedType = FeedType.NEW
            FilteredStoriesActivity.start(it, tagName = story.story.categoryName)
        }
    }

    fun onUserClick(story: StoryWrapper, context: Context?) {
        UserProfileActivity.start(context ?: return, story.story.author)
    }

    fun onVotersClick(story: StoryWrapper, context: Context?) {
        UsersListActivity.startToShowUpVoters(context ?: return, story.story.id)
    }

    fun cancelVote(storyId: Long) {
        mDiscussionsLiveData.value?.items?.find { it.story.id == storyId }?.let { cancelVote(it) }
    }

    fun vote(storyId: Long, vote: Short) {
        mDiscussionsLiveData.value?.items?.find { it.story.id == storyId }?.let { vote(it, vote) }
    }

    fun onDownVoters(story: StoryWrapper, fragmentActivity: FragmentActivity) {
        UsersListActivity.startToShowDownVoters(fragmentActivity, story.story.id)
    }
}
