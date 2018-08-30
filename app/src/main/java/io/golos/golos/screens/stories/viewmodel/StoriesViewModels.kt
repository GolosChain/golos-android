package io.golos.golos.screens.stories.viewmodel

import android.arch.lifecycle.*
import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.UserSettingsRepository
import io.golos.golos.repository.model.StoriesFeed
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.profile.UserProfileActivity
import io.golos.golos.screens.stories.FilteredStoriesActivity
import io.golos.golos.screens.stories.adapters.FeedCellSettings
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.stories.model.NSFWStrategy
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.userslist.UsersListActivity
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.InternetStatusNotifier
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by yuri on 01.11.17.
 */
object StoriesModelFactory {

    fun getStoriesViewModel(type: FeedType,
                            activity: FragmentActivity?,
                            fragment: Fragment?,
                            filter: StoryFilter?): StoriesViewModel {

        val provider = if (activity != null) ViewModelProviders.of(activity)
        else {
            if (fragment == null) throw IllegalStateException("activity or fragment must be not null")
            else ViewModelProviders.of(fragment)
        }
        var viewModel: StoriesViewModel


        viewModel = when (type) {
            FeedType.NEW -> provider.get(NewViewModel::class.java)
            FeedType.ACTUAL -> provider.get(ActualViewModle::class.java)
            FeedType.POPULAR -> provider.get(PopularViewModel::class.java)
            FeedType.PROMO -> provider.get(PromoViewModel::class.java)
            FeedType.PERSONAL_FEED -> provider.get(FeedViewModel::class.java)
            FeedType.BLOG -> provider.get(BlogViewModel::class.java)
            FeedType.COMMENTS -> provider.get(CommentsViewModel::class.java)
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

data class StoriesViewState(val isLoading: Boolean,
                            val showRefreshButton: Boolean,
                            val items: List<StoryWithComments> = ArrayList(),
                            val error: GolosError? = null,
                            val fullscreenMessage: Int? = null,
                            val popupMessage: Int? = null)

class CommentsViewModel : FeedViewModel() {
    override val type: FeedType
        get() = FeedType.COMMENTS

}

class BlogViewModel : FeedViewModel() {
    override val type: FeedType
        get() = FeedType.BLOG
}

open class FeedViewModel : StoriesViewModel() {
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

    override fun onNewItems(items: StoriesFeed?): StoriesViewState {
        return StoriesViewState(false,
                items?.isFeedActual == false,
                items?.items ?: ArrayList(),
                items?.error,
                if (items?.items?.size ?: 0 == 0) R.string.nothing_here else null)
    }
}


class NewViewModel : StoriesViewModel() {
    override val type: FeedType
        get() = FeedType.NEW
}

class ActualViewModle : StoriesViewModel() {
    override val type: FeedType
        get() = FeedType.ACTUAL
}

class PopularViewModel : StoriesViewModel() {
    override val type: FeedType
        get() = FeedType.POPULAR
}

class PromoViewModel : StoriesViewModel() {
    override val type: FeedType
        get() = FeedType.PROMO
}


abstract class StoriesViewModel : ViewModel(), Observer<StoriesFeed> {
    protected abstract val type: FeedType
    protected val mStoriesLiveData: MutableLiveData<StoriesViewState> = MutableLiveData()
    protected val mFeedSettingsLiveData: MediatorLiveData<FeedCellSettings> = MediatorLiveData()
    protected val mRepository = Repository.get
    protected val isUpdating = AtomicBoolean(false)
    protected var isVisibleToUser: Boolean = false
    protected var filter: StoryFilter? = null
    protected lateinit var internetStatusNotifier: InternetStatusNotifier
    private var mObserver: Observer<Boolean>? = null

    val storiesLiveData: LiveData<StoriesViewState>
        get() {
            return mStoriesLiveData
        }
    val cellViewSettingLiveData: LiveData<FeedCellSettings>
        get() {
            return mFeedSettingsLiveData
        }

    fun onCreate(internetStatusNotifier: InternetStatusNotifier, filter: StoryFilter?) {
        if (this.filter != null && this.filter != filter) {//recreating
            mStoriesLiveData.value = StoriesViewState(false,
                    false,
                    arrayListOf())
            isUpdating.set(false)
            onChangeVisibilityToUser(isVisibleToUser)
        }
        this.filter = filter
        this.internetStatusNotifier = internetStatusNotifier
        mRepository.getStories(type, filter).observeForever(this)


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

    override fun onChanged(t: StoriesFeed?) {
        if (t?.type == type && t.filter == filter)
            mStoriesLiveData.value = onNewItems(t)
    }

    protected open fun onNewItems(items: StoriesFeed?): StoriesViewState {
        return StoriesViewState(false,
                items?.isFeedActual == false,
                items?.items ?: ArrayList(),
                items?.error)
    }

    open fun onSwipeToRefresh() {

        if (!internetStatusNotifier.isAppOnline()) {
            setAppOffline()
            return
        }
        if (mStoriesLiveData.value?.isLoading == false) {
            mStoriesLiveData.value = StoriesViewState(true,
                    false,
                    mStoriesLiveData.value?.items ?: ArrayList())
            mRepository.requestStoriesListUpdate(20, type, filter, null, null, { _, _ -> })
            isUpdating.set(true)
        }
    }


    open fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        this.isVisibleToUser = visibleToUser
        if (visibleToUser) {
            if (!internetStatusNotifier.isAppOnline()) {
                setAppOffline()
                return
            }
            if (mStoriesLiveData.value == null ||
                    mStoriesLiveData.value?.items == null ||
                    mStoriesLiveData.value?.items?.isEmpty() == true) {
                if (isUpdating.get()) return
                mStoriesLiveData.value = StoriesViewState(true, false)
                mRepository.requestStoriesListUpdate(20, type, filter, null, null) { _, _ -> }
                isUpdating.set(true)
            }
        }
    }

    private fun setAppOffline() {
        mStoriesLiveData.value = StoriesViewState(false,
                mStoriesLiveData.value?.showRefreshButton == true,
                mStoriesLiveData.value?.items ?: ArrayList(),
                GolosError(ErrorCode.ERROR_NO_CONNECTION,
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
        if (mStoriesLiveData.value?.items?.size ?: 0 < 1) return
        //  if (isUpdating.get())return
        isUpdating.set(true)
        if (mStoriesLiveData.value?.isLoading == false) {
            mStoriesLiveData.value = StoriesViewState(true,
                    mStoriesLiveData.value?.showRefreshButton == true,
                    mStoriesLiveData.value?.items ?: ArrayList())
        }

        mRepository.requestStoriesListUpdate(20,
                type,
                filter,
                mStoriesLiveData.value?.items?.last()?.rootStory()?.author, mStoriesLiveData.value?.items?.last()?.rootStory()?.permlink, { _, _ -> })
    }

    open fun onCardClick(it: StoryWithComments, context: Context?) {
        if (context == null) return

        StoryActivity.start(context, it.rootStory()?.author ?: return,
                it.rootStory()?.categoryName ?: return,
                it.rootStory()?.permlink ?: return,
                type, filter)
    }

    open fun onCommentsClick(it: StoryWithComments, context: Context?) {
        if (context == null) return
        StoryActivity.start(context, it.rootStory()?.author ?: return,
                it.rootStory()?.categoryName ?: return,
                it.rootStory()?.permlink ?: return,
                type,
                filter,
                true)
    }

    open fun onShareClick(it: StoryWithComments, context: Context?) {
        val link = mRepository.getShareStoryLink(it.rootStory() ?: return)
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, link)
        sendIntent.type = "text/plain"
        context?.startActivity(sendIntent)
    }

    open fun vote(it: StoryWithComments, vote: Short) {
        if (!internetStatusNotifier.isAppOnline()) {
            setAppOffline()
            return
        }
        if (!mRepository.isUserLoggedIn()) {
            return
        }
        if (mRepository.isUserLoggedIn()) {
            val story = it.storyWithState() ?: return
            mRepository.vote(story, vote)
        } else {
            mStoriesLiveData.value = StoriesViewState(mStoriesLiveData.value?.isLoading ?: false,
                    mStoriesLiveData.value?.showRefreshButton == true,
                    mStoriesLiveData.value?.items ?: ArrayList(),
                    GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
        }

    }

    open fun cancelVote(it: StoryWithComments) {
        if (!internetStatusNotifier.isAppOnline()) {
            setAppOffline()
            return
        }
        if (mRepository.isUserLoggedIn()) {
            val story = it.storyWithState() ?: return
            mRepository.cancelVote(story)
        } else {
            mStoriesLiveData.value = StoriesViewState(mStoriesLiveData.value?.isLoading ?: false,
                    mStoriesLiveData.value?.showRefreshButton == true,
                    mStoriesLiveData.value?.items ?: ArrayList(),
                    GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
        }
    }

    fun canVote(): Boolean {
        return mRepository.isUserLoggedIn()
    }

    fun onVoteRejected(it: StoryWithComments) {
        mStoriesLiveData.value = StoriesViewState(mStoriesLiveData.value?.isLoading ?: false,
                mStoriesLiveData.value?.showRefreshButton == true,
                mStoriesLiveData.value?.items ?: ArrayList(),
                GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
    }

    fun onBlogClick(story: StoryWithComments, context: Context?) {

        context?.let {
            var feedType: FeedType = type
            if (feedType == FeedType.BLOG
                    || feedType == FeedType.COMMENTS
                    || feedType == FeedType.PERSONAL_FEED
                    || feedType == FeedType.UNCLASSIFIED
                    || feedType == FeedType.PROMO) feedType = FeedType.NEW
            FilteredStoriesActivity.start(it, tagName = story.rootStory()?.categoryName ?: return)
        }
    }

    fun onUserClick(it: StoryWithComments, context: Context?) {
        UserProfileActivity.start(context ?: return, it.rootStory()?.author ?: return)
    }

    fun onVotersClick(it: StoryWithComments, context: Context?) {
        UsersListActivity.startToShowVoters(context ?: return, it.rootStory()?.id ?: return)
    }

    override fun toString(): String {
        return "StoriesViewModel(type=$type, isVisibleToUser=$isVisibleToUser, filter=$filter)"
    }

    fun onDestroy() {
        mRepository.getStories(type, filter).removeObserver(this)
        if (mObserver != null) {
            Repository.get.userSettingsRepository.isStoriesCompactMode().removeObserver(mObserver
                    ?: return)
            Repository.get.userSettingsRepository.isImagesShown().removeObserver(mObserver
                    ?: return)
            Repository.get.userSettingsRepository.isNSFWShow().removeObserver(mObserver ?: return)
            mObserver = null
        }
    }

}
