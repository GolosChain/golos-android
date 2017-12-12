package io.golos.golos.screens.stories.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.StoryFilter
import io.golos.golos.repository.model.StoryTreeItems
import io.golos.golos.screens.stories.FilteredStoriesActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by yuri on 01.11.17.
 */
data class StoriesViewState(val isLoading: Boolean,
                            val items: List<StoryTree> = ArrayList(),
                            val error: GolosError? = null,
                            val fullscreenMessage: Int? = null,
                            val popupMessage: Int? = null)

class CommentsViewModle : FeedViewModel() {
    override val type: FeedType
        get() = FeedType.COMMENTS
}

class BlogViewModle : FeedViewModel() {
    override val type: FeedType
        get() = FeedType.BLOG
}

open class FeedViewModel : StoriesViewModel() {
    override val type: FeedType
        get() = FeedType.PERSONAL_FEED

    override fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        if (!mRepository.isUserLoggedIn() && visibleToUser) {
            mHandler.post {
                mStoriesLiveData.value = StoriesViewState(false,
                        ArrayList(),
                        fullscreenMessage = R.string.login_to_see_feed)
            }
        } else super.onChangeVisibilityToUser(visibleToUser)
    }

    override fun onSwipeToRefresh() {
        if (!mRepository.isUserLoggedIn()) return
        super.onSwipeToRefresh()
    }

    override fun onScrollToTheEnd() {
        if (!mRepository.isUserLoggedIn()) return
        super.onScrollToTheEnd()
    }

    override fun onNewItems(items: StoryTreeItems?): StoriesViewState {
        return StoriesViewState(false, items?.items ?: ArrayList(), items?.error,
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


abstract class StoriesViewModel : ViewModel() {
    protected abstract val type: FeedType
    protected val mStoriesLiveData: MediatorLiveData<StoriesViewState> = MediatorLiveData()
    protected val mRepository = Repository.get
    protected val isUpdating = AtomicBoolean(false)
    protected var isVisibleToUser: Boolean = false
    var filter: StoryFilter? = null
        set(value) {
            if (field != value) {
                mStoriesLiveData.value = StoriesViewState(true, ArrayList())
                mStoriesLiveData.removeSource(mRepository.getStories(type, field))
                mStoriesLiveData.addSource(mRepository.getStories(type, value)) {
                    mStoriesLiveData.value = onNewItems(it)
                }
                mRepository.requestStoriesListUpdate(20, type, value, null, null)
            }
            field = value

        }

    companion object {
        val mHandler = Handler(Looper.getMainLooper())
    }

    val storiesLiveData: LiveData<StoriesViewState>
        get() {
            return mStoriesLiveData
        }

    init {
        mStoriesLiveData.addSource(mRepository.getStories(type, filter)) {
            mStoriesLiveData.value = onNewItems(it)
        }
    }

    protected open fun onNewItems(items: StoryTreeItems?): StoriesViewState {
        return StoriesViewState(false, items?.items ?: ArrayList(), items?.error)
    }

    open fun onSwipeToRefresh() {
        if (!App.isAppOnline()) {
            setAppOffline()
            return
        }
        if (mStoriesLiveData.value?.isLoading == false) {
            mStoriesLiveData.value = StoriesViewState(true, mStoriesLiveData.value?.items ?: ArrayList())
            mRepository.requestStoriesListUpdate(20, type, filter, null, null)
            isUpdating.set(true)
        }
    }

    open fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        this.isVisibleToUser = isVisibleToUser
        if (visibleToUser) {
            if (!App.isAppOnline()) {
                setAppOffline()
                return
            }
            if (mStoriesLiveData.value == null ||
                    mStoriesLiveData.value?.items == null ||
                    mStoriesLiveData.value?.items?.isEmpty() == true) {
                if (isUpdating.get()) return
                mStoriesLiveData.value = StoriesViewState(true)
                mRepository.requestStoriesListUpdate(20, type, filter, null, null)
                isUpdating.set(true)
            }
        }
    }

    protected fun setAppOffline() {
        mStoriesLiveData.value = StoriesViewState(false, mStoriesLiveData.value?.items ?: ArrayList(),
                GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
    }

    open fun onScrollToTheEnd() {
        if (!App.isAppOnline()) {
            setAppOffline()
            return
        }
        if (mStoriesLiveData.value?.items?.size ?: 0 < 1) return
        //  if (isUpdating.get())return
        isUpdating.set(true)
        mStoriesLiveData.value = StoriesViewState(true, mStoriesLiveData.value?.items ?: ArrayList())
        mRepository.requestStoriesListUpdate(20,
                type,
                null,
                mStoriesLiveData.value?.items?.last()?.rootStory()?.author, mStoriesLiveData.value?.items?.last()?.rootStory()?.permlink)
    }

    open fun onCardClick(it: StoryTree, context: Context?) {
        if (context == null) return
        StoryActivity.start(context, it.rootStory()?.author ?: return,
                it.rootStory()?.categoryName ?: return,
                it.rootStory()?.permlink ?: return,
                type)
    }

    open fun onCommentsClick(it: StoryTree, context: Context?) {
        if (context == null) return
        StoryActivity.start(context, it.rootStory()?.author ?: return,
                it.rootStory()?.categoryName ?: return,
                it.rootStory()?.permlink ?: return,
                type)
    }

    open fun onShareClick(it: StoryTree, context: Context?) {
        val link = mRepository.getShareStoryLink(it.rootStory() ?: return)
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, link)
        sendIntent.type = "text/plain"
        context?.startActivity(sendIntent)
    }

    open fun vote(it: StoryTree, vote: Short) {
        if (!App.isAppOnline()) {
            setAppOffline()
            return
        }
        if (!mRepository.isUserLoggedIn()) {
            return
        }
        if (mRepository.isUserLoggedIn()) {
            val story = it.rootStory() ?: return
            mRepository.upVote(story, vote)
        } else {
            mStoriesLiveData.value = StoriesViewState(mStoriesLiveData.value?.isLoading ?: false,
                    mStoriesLiveData.value?.items ?: ArrayList(),
                    GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
        }

    }

    open fun downVote(it: StoryTree) {
        if (!App.isAppOnline()) {
            setAppOffline()
            return
        }
        if (mRepository.isUserLoggedIn()) {
            val story = it.rootStory() ?: return
            mRepository.cancelVote(story)
        } else {
            mStoriesLiveData.value = StoriesViewState(mStoriesLiveData.value?.isLoading ?: false,
                    mStoriesLiveData.value?.items ?: ArrayList(),
                    GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
        }
    }

    open var canVote = mRepository.isUserLoggedIn()

    fun onVoteRejected(it: StoryTree) {
        mStoriesLiveData.value = StoriesViewState(mStoriesLiveData.value?.isLoading ?: false,
                mStoriesLiveData.value?.items ?: ArrayList(),
                GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
    }

    fun onBlogClick(context: Context?, story: StoryTree) {

        context?.let {
            FilteredStoriesActivity.start(it, feedType = type, filter = StoryFilter(story.rootStory()?.categoryName))
        }
    }
}
