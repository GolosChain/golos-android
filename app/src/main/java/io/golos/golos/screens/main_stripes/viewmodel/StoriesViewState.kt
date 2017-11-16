package io.golos.golos.screens.main_stripes.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.screens.story.model.RootStory
import io.golos.golos.utils.GolosError
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by yuri on 01.11.17.
 */
data class StoriesViewState(val isLoading: Boolean,
                            val items: List<RootStory> = ArrayList(),
                            val error: GolosError? = null,
                            val fullscreenMessage: Int? = null,
                            val popupMessage: Int? = null)

class FeedViewModel : StoriesViewModel() {
    override val router = FeedRouter(Repository.get, Repository.get.getCurrentUserData()?.userName ?: "")
    override val type: FeedType
        get() = FeedType.PERSONAL_FEED

    override fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        var userData = mRepository.getCurrentUserData()
        if (userData == null && visibleToUser) {
            mHandler.post {
                mStoriesLiveData.value = StoriesViewState(false,
                        ArrayList(),
                        fullscreenMessage = R.string.login_to_see_feed)
            }
        } else super.onChangeVisibilityToUser(visibleToUser)
    }

    override fun onSwipeToRefresh() {
        var userData: UserData? = mRepository.getCurrentUserData() ?: return
        super.onSwipeToRefresh()
    }

    override fun onScrollToTheEnd() {
        var userData: UserData? = mRepository.getCurrentUserData() ?: return
        super.onScrollToTheEnd()
    }
}

class NewViewModel : StoriesViewModel() {
    override val router = StripeRouter(Repository.get, FeedType.NEW)
    override val type: FeedType
        get() = FeedType.NEW
}

class ActualViewModle : StoriesViewModel() {
    override val router = StripeRouter(Repository.get, FeedType.ACTUAL)
    override val type: FeedType
        get() = FeedType.ACTUAL
}

class PopularViewModel : StoriesViewModel() {
    override val router = StripeRouter(Repository.get, FeedType.POPULAR)
    override val type: FeedType
        get() = FeedType.POPULAR
}

class PromoViewModel : StoriesViewModel() {
    override val router = StripeRouter(Repository.get, FeedType.PROMO)
    override val type: FeedType
        get() = FeedType.PROMO
}


abstract class StoriesViewModel : ViewModel() {
    protected abstract val router: StoriesRequestRouter
    protected abstract val type: FeedType
    protected val mStoriesLiveData: MediatorLiveData<StoriesViewState> = MediatorLiveData()
    protected val mRepository = Repository.get
    protected val isUpdating = AtomicBoolean(false)
    protected var mLatch = CountDownLatch(1)

    companion object {
        val mHandler = Handler(Looper.getMainLooper())
    }

    val storiesLiveData: LiveData<StoriesViewState>
        get() {
            return mStoriesLiveData
        }

    init {
        mStoriesLiveData.addSource(mRepository.getStories(type)) {
          //  Timber.e("on stories $type size is ${it?.items?.size} ad viewmodel type is ${this.type}")
            mStoriesLiveData.value = StoriesViewState(false, it?.items?.map { RootStory(it.comment) } ?: ArrayList(), it?.error)
        }
    }

    open fun onSwipeToRefresh() {
        if (mStoriesLiveData.value?.isLoading == false) {
            mStoriesLiveData.value = StoriesViewState(true, mStoriesLiveData.value?.items ?: ArrayList())
            mRepository.requestStoriesUpdate(20, type, null, null)
            isUpdating.set(true)
        }
    }

    open fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        if (visibleToUser) {
            if (mStoriesLiveData.value == null ||
                    mStoriesLiveData.value?.items == null ||
                    mStoriesLiveData.value?.items?.isEmpty() == true) {
                if (isUpdating.get()) return
                mStoriesLiveData.value = StoriesViewState(true)
                mRepository.requestStoriesUpdate(20, type, null, null)
                isUpdating.set(true)
            }
        }
    }

    open fun onScrollToTheEnd() {
        mRepository.requestStoriesUpdate(20, type, mStoriesLiveData.value?.items?.last()?.author, mStoriesLiveData.value?.items?.last()?.permlink)
    }

    open fun onCardClick(it: RootStory, context: Context?) {
        if (context == null) return
        StoryActivity.start(context, it)
    }

    open fun onCommentsClick(it: RootStory, context: Context?) {
        if (context == null) return
        StoryActivity.start(context, it)
    }

    open fun onShareClick(it: RootStory, context: Context?) {
        val link = "https://golos.blog/${it.categoryName}/@${it.author}/${it.permlink}"
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, link)
        sendIntent.type = "text/plain"
        context?.startActivity(sendIntent)
    }

    open fun vote(it: RootStory, vote: Int) {
        if (mRepository.getCurrentUserData() == null) {
            return
        }
        mRepository.upVoteNoCallback(it, vote.toShort())
    }

    open fun downVote(it: RootStory) {
        if (mRepository.getCurrentUserData() == null) {
            return
        }
        mRepository.cancelVoteNoCallback(it)
    }

    open var canVote = mRepository.getCurrentUserData()?.userName != null
}

interface ImageLoadRunnable : Runnable