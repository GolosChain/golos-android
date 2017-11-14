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
import io.golos.golos.screens.main_stripes.model.StripeType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.screens.story.model.RootStory
import io.golos.golos.utils.ErrorCodes
import io.golos.golos.utils.SteemErrorParser
import timber.log.Timber
import java.security.InvalidParameterException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by yuri on 01.11.17.
 */
data class StripeViewState(val isLoading: Boolean,
                           val items: List<RootStory> = ArrayList(),
                           val error: ErrorCodes? = null,
                           val messageLocalized: Int? = null,
                           val message: String? = null)

class NewViewModel : StripeFragmentViewModel() {
    override val stripeType = StripeType.NEW
}

class ActualViewModle : StripeFragmentViewModel() {
    override val stripeType = StripeType.ACTUAL
}

class PopularViewModel : StripeFragmentViewModel() {
    override val stripeType = StripeType.POPULAR
}

class PromoViewModel : StripeFragmentViewModel() {
    override val stripeType = StripeType.PROMO
}


abstract class StripeFragmentViewModel : ViewModel() {
    abstract val stripeType: StripeType
    val stripeLiveData = MutableLiveData<StripeViewState>()
    private val mRepository = Repository.get
    private val isUpdating = AtomicBoolean(false)
    private var mTruncateSize = 1024
    private var mLatch = CountDownLatch(1)

    companion object {
        private val mHandler = Handler(Looper.getMainLooper())
    }

    init {
        stripeLiveData.value = StripeViewState(false)
    }

    fun onSwipeToRefresh() {
        if (isUpdating.get()) return
        isUpdating.set(true)
        postWithCatch {
            val item = mRepository.getStripeItems(limit = 20, type = stripeType, truncateBody = mTruncateSize)
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

    fun onChangeVisibilityToUser(visibleToUser: Boolean) {
        if (visibleToUser) {
            if (stripeLiveData.value?.items?.isEmpty() == true && !isUpdating.get()) {
                isUpdating.set(true)
                stripeLiveData.value = StripeViewState(true, ArrayList())
                postWithCatch({
                    val size = stripeLiveData.value?.items?.size
                    if (size == 0) {
                        mHandler.post({ stripeLiveData.value = StripeViewState(true, ArrayList()) })
                        val items = ArrayList(mRepository.getStripeItems(limit = 20, type = stripeType, truncateBody = mTruncateSize))
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

    fun onScrollToTheEnd() {
        if (isUpdating.get()) return
        isUpdating.set(true)
        postWithCatch({
            mHandler.post({ stripeLiveData.value = StripeViewState(true, stripeLiveData.value?.items ?: ArrayList()) })
            val newItems = mRepository.getStripeItems(limit = 20,
                    type = stripeType,
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


    private fun postWithCatch(action: () -> Unit) {
        Repository.sharedExecutor.execute({
            try {
                action.invoke()
                isUpdating.set(false)
            } catch (e: SteemResponseError) {
                Timber.e(e.error.steemErrorDetails.message)
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            null,
                            SteemErrorParser.getLocalizedError(e))
                })
            } catch (e: SteemInvalidTransactionException) {
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            message = e.message)
                })
            } catch (e: SteemTimeoutException) {
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            ErrorCodes.ERROR_SLOW_CONNECTION)
                })
            } catch (e: SteemCommunicationException) {
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            ErrorCodes.ERROR_NO_CONNECTION)
                })
            } catch (e: InvalidParameterException) {
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            ErrorCodes.ERROR_WRONG_ARGUMENTS)
                })
            } catch (e: SteemConnectionException) {
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            ErrorCodes.ERROR_NO_CONNECTION)
                })
            } catch (e: Exception) {
                e.printStackTrace()
                mHandler.post({
                    stripeLiveData.value = StripeViewState(false,
                            stripeLiveData.value?.items ?: ArrayList(),
                            ErrorCodes.ERROR_NO_CONNECTION)
                })
            }
        })
    }

    fun onCardClick(it: RootStory, context: Context?) {
        if (context == null) return
        StoryActivity.start(context, author = it.author,
                permlink = it.permlink,
                blog = it.categoryName,
                title = it.title)
    }

    fun onCommentsClick(it: RootStory, context: Context?) {
        if (context == null) return
        StoryActivity.start(context, author = it.author,
                permlink = it.permlink,
                blog = it.categoryName,
                title = it.title)
    }

    fun onShareClick(it: RootStory, context: Context?) {
        val link = "https://golos.blog/${it.categoryName}/@${it.author}/${it.permlink}"
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, link)
        sendIntent.type = "text/plain"
        context?.startActivity(sendIntent)
    }

    fun vote(it: RootStory, vote: Int) {
        if (mRepository.getCurrentUserData() == null) {
            mHandler.post({
                stripeLiveData.value = StripeViewState(false,
                        stripeLiveData.value?.items ?: ArrayList(),
                        null,
                      messageLocalized = R.string.login_to_vote)
            })
            return
        }
        Timber.e("vote, it = ${it.title}")
        postWithCatch {
            val story = mRepository.upvote(it.author, it.permlink, vote.toShort())
            Timber.e("upvoted!")
            mHandler.post({
                val state = stripeLiveData.value?.items
                if (state != null) {
                    var copy = ArrayList<RootStory>(state.size)
                    state.forEach { copy.add(it.clone() as RootStory) }
                    state.filter { it.id == story.id }.forEachIndexed { index, item -> copy[index] = story }
                    stripeLiveData.value = StripeViewState(stripeLiveData.value?.isLoading == true,
                            copy, null, R.string.upvoted)
                }
            })
        }
    }

    fun downVote(it: RootStory) {
        Timber.e("downVote, it = ${it.title}")
        postWithCatch {
            val story = mRepository.downVote(it.author, it.permlink)
            Timber.e("downvoted!")
            mHandler.post({
                val state = stripeLiveData.value?.items
                Timber.e("state = $state")
                if (state != null) {
                    var copy = ArrayList(state)
                    Timber.e("copy = $copy")
                    state.filter { it.id == story.id }.forEachIndexed { index, item -> copy[index] = story }
                    Timber.e("state = $state")
                    Timber.e("copy = $copy")
                    stripeLiveData.value = StripeViewState(stripeLiveData.value?.isLoading == true,
                            copy, null,
                            R.string.vote_canceled)
                }
            })
        }
    }

}

interface ImageLoadRunnable : Runnable