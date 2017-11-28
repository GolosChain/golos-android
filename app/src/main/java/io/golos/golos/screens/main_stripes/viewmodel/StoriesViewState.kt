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
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.GolosError
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by yuri on 01.11.17.
 */
data class StoriesViewState(val isLoading: Boolean,
                            val items: List<StoryTree> = ArrayList(),
                            val error: GolosError? = null,
                            val fullscreenMessage: Int? = null,
                            val popupMessage: Int? = null)

class FeedViewModel : StoriesViewModel() {
    override val type: FeedType
        get() = FeedType.PERSONAL_FEED

    override fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        var userData = mRepository.getSavedActiveUserData()
        if (userData == null && visibleToUser) {
            mHandler.post {
                mStoriesLiveData.value = StoriesViewState(false,
                        ArrayList(),
                        fullscreenMessage = R.string.login_to_see_feed)
            }
        } else super.onChangeVisibilityToUser(visibleToUser)
    }

    override fun onSwipeToRefresh() {
        var userData: UserData? = mRepository.getSavedActiveUserData() ?: return
        super.onSwipeToRefresh()
    }

    override fun onScrollToTheEnd() {
        var userData: UserData? = mRepository.getSavedActiveUserData() ?: return
        super.onScrollToTheEnd()
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
            mStoriesLiveData.value = StoriesViewState(false, it?.items ?: ArrayList(), it?.error)
        }
    }

    open fun onSwipeToRefresh() {
        if (mStoriesLiveData.value?.isLoading == false) {
            mStoriesLiveData.value = StoriesViewState(true, mStoriesLiveData.value?.items ?: ArrayList())
            mRepository.requestStoriesListUpdate(20, type, null, null)
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
                mRepository.requestStoriesListUpdate(20, type, null, null)
                isUpdating.set(true)
            }
        }
    }

    open fun onScrollToTheEnd() {
        if (mStoriesLiveData.value?.items?.size ?: 0 < 1) return
        mRepository.requestStoriesListUpdate(20,
                type,
                mStoriesLiveData.value?.items?.last()?.rootStory()?.author,
                mStoriesLiveData.value?.items?.last()?.rootStory()?.permlink)
    }

    open fun onCardClick(it: StoryTree, context: Context?) {
        if (context == null) return
        StoryActivity.start(context, it, type)
    }

    open fun onCommentsClick(it: StoryTree, context: Context?) {
        if (context == null) return
        StoryActivity.start(context, it, type)
    }

    open fun onShareClick(it: StoryTree, context: Context?) {
        val link = "https://golos.blog/${it.rootStory()?.categoryName}/@${it.rootStory()?.author}/${it.rootStory()?.permlink}"
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, link)
        sendIntent.type = "text/plain"
        context?.startActivity(sendIntent)
    }

    open fun vote(it: StoryTree, vote: Short) {
        if (mRepository.getSavedActiveUserData() == null) {
            return
        }
        val story = it.rootStory() ?: return
        mRepository.upVote(story, vote)
    }

    open fun downVote(it: StoryTree) {
        if (mRepository.getSavedActiveUserData() == null) {
            return
        }
        val story = it.rootStory() ?: return
        mRepository.cancelVote(story)
    }

    open var canVote = mRepository.getSavedActiveUserData()?.userName != null
}

interface ImageLoadRunnable : Runnable