package io.golos.golos.screens.main_stripes.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import eu.bittrade.libs.steemj.exceptions.SteemCommunicationException
import eu.bittrade.libs.steemj.exceptions.SteemConnectionException
import eu.bittrade.libs.steemj.exceptions.SteemTimeoutException
import io.golos.golos.repository.Repository
import io.golos.golos.screens.main_stripes.model.StripeItem
import io.golos.golos.screens.main_stripes.model.StripeType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.utils.ErrorCodes
import timber.log.Timber
import java.security.InvalidParameterException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList


/**
 * Created by yuri on 01.11.17.
 */
data class StripeViewState(val isLoading: Boolean,
                           val items: List<StripeItem> = ArrayList(),
                           val error: ErrorCodes? = null)

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
    private val repository = Repository.get
    private val isUpdating = AtomicBoolean(false)
    private var mTruncateSize = 560
    private var mLatch = CountDownLatch(1)

    companion object {
        private val mStripesExecutor: ThreadPoolExecutor by lazy {
            val queu = PriorityBlockingQueue<Runnable>(15, Comparator<Runnable> { o1, o2 ->
                if (o1 is ImageLoadRunnable) Int.MAX_VALUE
                else if (o1 is Runnable) Int.MIN_VALUE
                else 0
            })
            ThreadPoolExecutor(1, 1,
                    Long.MAX_VALUE, TimeUnit.MILLISECONDS, queu)
        }
        private val mHandler = Handler(Looper.getMainLooper())
    }

    init {
        stripeLiveData.value = StripeViewState(false)
    }

    fun onSwipeToRefresh() {
        Timber.e("onSwipeToRefresh")
        if (isUpdating.get()) return
        isUpdating.set(true)
        postWithCatch {
            val item = repository.getStripeItems(limit = 20, type = stripeType, truncateBody = mTruncateSize)
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
                stripeLiveData.value = StripeViewState(true, ArrayList())
                postWithCatch({
                    val size = stripeLiveData.value?.items?.size
                    if (size == 0) {
                        mHandler.post({ stripeLiveData.value = StripeViewState(true, ArrayList()) })
                        val items = ArrayList(repository.getStripeItems(limit = 20, type = stripeType, truncateBody = mTruncateSize))
                        mHandler.post({
                            stripeLiveData.value = StripeViewState(false, items)
                            mLatch.countDown()
                        })
                        mLatch = CountDownLatch(1)
                        mLatch.await()
                        startLoadingAbscentAvatars()
                    }
                })
            }
        } else {

        }
    }

    fun onScrollToTheEnd() {
        if (isUpdating.get()) return
        isUpdating.set(true)
        postWithCatch({
            mHandler.post({ stripeLiveData.value = StripeViewState(true, stripeLiveData.value?.items ?: ArrayList()) })
            val newItems = repository.getStripeItems(limit = 20,
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
                mStripesExecutor.execute(object : ImageLoadRunnable {
                    override fun run() {
                        try {
                            if (it.avatarPath != null) return
                            val actualItems = ArrayList(stripeLiveData.value!!.items)
                            val avatar = repository.getUserAvatar(it.author, it.permlink, it.categoryName)
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
        mStripesExecutor.execute({
            try {
                action.invoke()
                isUpdating.set(false)
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

    fun onCardClick(it: StripeItem, context: Context?) {
        Timber.e("onCardClick $it")
        if (context == null) return
        StoryActivity.start(context, author = it.author,
                permlink = it.permlink,
                blog = it.categoryName,
                title = it.title)
    }

    fun onCommentsClick(it: StripeItem, context: Context?) {
        Timber.e("onCommentsClick $it")
        if (context == null) return
        StoryActivity.start(context, author = it.author,
                permlink = it.permlink,
                blog = it.categoryName,
                title = it.title)
    }

    fun onShareClick(it: StripeItem, context: Context?) {
        val link = "https://golos.io/${it.categoryName}/@${it.author}/${it.permlink}"
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, link)
        sendIntent.type = "text/plain"
        context?.startActivity(sendIntent)
    }
}

interface ImageLoadRunnable : Runnable