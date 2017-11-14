package io.golos.golos.screens.main_stripes.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import eu.bittrade.libs.steemj.exceptions.*
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.screens.main_stripes.model.StripeFragmentType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.screens.story.model.RootStory
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.SteemErrorParser
import java.security.InvalidParameterException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by yuri on 01.11.17.
 */
data class StripeViewState(val isLoading: Boolean,
                           val items: List<RootStory> = ArrayList(),
                           val error: GolosError? = null,
                           val fullscreenMessage: Int? = null,
                           val popupMessage: Int? = null)

class FeedViewModel : StripeFragmentViewModel() {
    override val router = FeedRouter(Repository.get, Repository.get.getCurrentUserData()?.userName ?: "")

    override fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        var userData = mRepository.getCurrentUserData()
        if (userData == null && visibleToUser) {
            mHandler.post {
                stripeLiveData.value = StripeViewState(false,
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

class NewViewModel : StripeFragmentViewModel() {
    override val router = StripeRouter(Repository.get, StripeFragmentType.NEW)
}

class ActualViewModle : StripeFragmentViewModel() {
    override val router = StripeRouter(Repository.get, StripeFragmentType.ACTUAL)
}

class PopularViewModel : StripeFragmentViewModel() {
    override val router = StripeRouter(Repository.get, StripeFragmentType.POPULAR)
}

class PromoViewModel : StripeFragmentViewModel() {
    override val router = StripeRouter(Repository.get, StripeFragmentType.PROMO)
}


abstract class StripeFragmentViewModel : ViewModel() {
    //abstract val stripeType: StripeType
    abstract val router: StoriesRequestRouter
    val stripeLiveData = MutableLiveData<StripeViewState>()
    protected val mRepository = Repository.get
    protected val isUpdating = AtomicBoolean(false)
    protected var mTruncateSize = 1024
    protected var mLatch = CountDownLatch(1)

    companion object {
        val mHandler = Handler(Looper.getMainLooper())
    }

    init {
        stripeLiveData.value = StripeViewState(false)
    }

    open fun onSwipeToRefresh() {
        if (isUpdating.get()) return
        isUpdating.set(true)
        postWithCatch {
            val item = router.getStories(limit = 20, truncateBody = mTruncateSize)
            mLatch = CountDownLatch(1)
            mHandler.post({
                stripeLiveData.value = StripeViewState(false, item)
                mLatch.countDown()
            })
            mLatch.await()
            startLoadingAbscentAvatars()
            isUpdating.set(false)
        }
    }

    open fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        if (visibleToUser) {
            if (stripeLiveData.value?.items?.isEmpty() == true && !isUpdating.get()) {
                isUpdating.set(true)
                stripeLiveData.value = StripeViewState(true, ArrayList())
                postWithCatch({
                    val size = stripeLiveData.value?.items?.size
                    if (size == 0) {
                        mHandler.post({ stripeLiveData.value = StripeViewState(true, ArrayList()) })
                        val items = ArrayList(router.getStories(limit = 20, truncateBody = mTruncateSize))
                        mHandler.post({
                            stripeLiveData.value = StripeViewState(false, items)
                            mLatch.countDown()
                        })
                        mLatch = CountDownLatch(1)
                        mLatch.await()
                        startLoadingAbscentAvatars()
                        isUpdating.set(false)
                    }
                })
            }
        }
    }

    open fun onScrollToTheEnd() {
        if (isUpdating.get()) return
        isUpdating.set(true)
        postWithCatch({
            mHandler.post({ stripeLiveData.value = StripeViewState(true, stripeLiveData.value?.items ?: ArrayList()) })
            val newItems = router.getStories(limit = 20,
                    truncateBody = mTruncateSize,
                    startAuthor = stripeLiveData.value?.items?.last()?.author,
                    startPermlink = stripeLiveData.value?.items?.last()?.permlink)
            mHandler.post({
                val oldItems = stripeLiveData.value?.items ?: ArrayList()
                stripeLiveData.value =
                        StripeViewState(false, oldItems + newItems.subList(1, newItems.size))
                isUpdating.set(false)
                mLatch.countDown()
            })
            mLatch = CountDownLatch(1)
            mLatch.await()
            startLoadingAbscentAvatars()
        })
    }

    private fun startLoadingAbscentAvatars() {
        val actualItems = stripeLiveData.value?.items
        if (actualItems == null || actualItems.isEmpty()) return
        actualItems.forEach {
            if (it.avatarPath == null) {
                Repository.sharedExecutor.execute(object : ImageLoadRunnable {
                    override fun run() {
                        try {
                            if (it.avatarPath != null) return
                            val actualItems = ArrayList<RootStory>(stripeLiveData.value!!.items.size)
                            stripeLiveData.value!!.items.forEach { actualItems.add(it.clone() as RootStory) }
                            val avatar = mRepository.getUserAvatar(it.author, it.permlink, it.categoryName)
                            val newItems = actualItems.filter { item -> it.author == item.author }
                            if (newItems.isEmpty()) return
                            newItems.forEach { it.avatarPath = avatar }
                            mLatch = CountDownLatch(1)
                            mHandler.post({
                                stripeLiveData.value = StripeViewState(false, actualItems, null)
                                mLatch.countDown()
                            })
                            mLatch.await()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            mLatch.countDown()
                        }
                    }
                })
            }
        }
    }


    open protected fun postWithCatch(action: () -> Unit) {
        Repository.sharedExecutor.execute({
            try {
                action.invoke()
                isUpdating.set(false)
            } catch (e: SteemResponseError) {
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, null, SteemErrorParser.getLocalizedError(e)))
                })
            } catch (e: SteemInvalidTransactionException) {
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, e.message, null))
                })
            } catch (e: SteemTimeoutException) {
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            GolosError(ErrorCode.ERROR_SLOW_CONNECTION, null, R.string.slow_internet_connection))
                })
            } catch (e: SteemCommunicationException) {
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
                })
            } catch (e: InvalidParameterException) {
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            GolosError(ErrorCode.ERROR_WRONG_ARGUMENTS, null, R.string.wrong_args))
                })
            } catch (e: SteemConnectionException) {
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.no_internet_connection))
                })
            } catch (e: Exception) {
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            GolosError(ErrorCode.ERROR_NO_CONNECTION, null, R.string.unknown_error))
                })
            }
        })
    }

    open fun onCardClick(it: RootStory, context: Context?) {
        if (context == null) return
        StoryActivity.start(context, author = it.author,
                permlink = it.permlink,
                blog = it.categoryName,
                title = it.title)
    }

    open fun onCommentsClick(it: RootStory, context: Context?) {
        if (context == null) return
        StoryActivity.start(context, author = it.author,
                permlink = it.permlink,
                blog = it.categoryName,
                title = it.title)
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
            mHandler.post({
                stripeLiveData.value = StripeViewState(false,
                        stripeLiveData.value?.items ?: ArrayList(),
                        GolosError(ErrorCode.ERROR_AUTH, null, R.string.login_to_vote))
            })
            return
        }
        postWithCatch {
            val story = mRepository.upvote(it.author, it.permlink, vote.toShort())
            mHandler.post({
                val state = stripeLiveData.value?.items
                if (state != null) {
                    var copy = ArrayList<RootStory>(state.size)
                    state.forEach { copy.add(it.clone() as RootStory) }

                    state.forEachIndexed { index, item ->
                        if (item.id == story.id)
                            copy.set(index, story)
                    }
                    stripeLiveData.value = StripeViewState(stripeLiveData.value?.isLoading == true,
                            copy, null, R.string.upvoted)
                }
            })
        }
    }

    open fun downVote(it: RootStory) {
        postWithCatch {
            val story = mRepository.downVote(it.author, it.permlink)
            mHandler.post({
                val state = stripeLiveData.value?.items
                if (state != null) {
                    var copy = ArrayList(state)
                    state.forEachIndexed { index, item ->
                        if (item.id == story.id)
                            copy.set(index, story)
                    }
                    stripeLiveData.value = StripeViewState(stripeLiveData.value?.isLoading == true,
                            copy, null,
                            R.string.vote_canceled)
                }
            })
        }
    }

}

interface ImageLoadRunnable : Runnable