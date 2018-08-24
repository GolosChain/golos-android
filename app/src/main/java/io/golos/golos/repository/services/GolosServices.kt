package io.golos.golos.repository.services

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.AnyThread
import android.support.annotation.MainThread
import android.support.annotation.WorkerThread
import io.golos.golos.notifications.FCMTokenProvider
import io.golos.golos.notifications.FCMTokenProviderImpl
import io.golos.golos.notifications.FCMTokens
import io.golos.golos.repository.Repository
import io.golos.golos.repository.UserDataProvider
import io.golos.golos.repository.model.NotificationsPersister
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.utils.MainThreadExecutor
import timber.log.Timber
import java.util.TreeSet
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.collections.HashMap
import kotlin.collections.set

interface GolosServices {

    @MainThread
    fun setUp()


    fun getEvents(eventType: EventType? = null): LiveData<List<GolosEvent>>

    @AnyThread
    fun requestEventsUpdate(
            /**null if update all events**/
            eventType: EventType? = null,
            fromId: String? = null,
            limit: Int? = 100)
}

class GolosServicesImpl(
        private val tokenProvider: FCMTokenProvider = FCMTokenProviderImpl,
        private val userDataProvider: UserDataProvider = Repository.get,
        private val notificationsPersister: NotificationsPersister = Persister.get,
        golosServicesGateWay: GolosServicesGateWay? = null,
        private val workerExecutor: Executor = Executors.newSingleThreadExecutor(),
        private val mainThreadExecutor: Executor = MainThreadExecutor()) : GolosServices {

    @Volatile
    private var isAuthComplete: Boolean = false
    @Volatile
    private var isAuthInProgress: Boolean = false


    private val mGolosServicesGateWay: GolosServicesGateWay = golosServicesGateWay
            ?: GolosServicesGateWayImpl()

    private val mEventsMap: HashMap<EventType, MutableLiveData<List<GolosEvent>>> = HashMap<EventType, MutableLiveData<List<GolosEvent>>>().apply {
        EventType.values().forEach { eventType ->
            this[eventType] = MutableLiveData()
        }
    }
    private val allEvents = MutableLiveData<List<GolosEvent>>()


    override fun setUp() {
        tokenProvider.tokenLiveData.observeForever({
            onTokenOrAuthStateChanged()
        })
        userDataProvider.appUserData.observeForever({
            onTokenOrAuthStateChanged()
        })
    }

    private fun onTokenOrAuthStateChanged() {
        val appUserData = userDataProvider.appUserData.value
        if (appUserData == null || !appUserData.isUserLoggedIn) {
            if (isAuthComplete) {
                workerExecutor.execute {
                    mGolosServicesGateWay.logout()
                    notificationsPersister.setUserSubscribedOnNotificationsThroughServices(false)
                    isAuthComplete = false
                    isAuthInProgress = false
                    allEvents.value = null
                    mEventsMap.forEach { it.value.value = null }
                }
            }
            return
        }
        if (isAuthComplete) return
        if (isAuthInProgress) return
        isAuthInProgress = true
        workerExecutor.execute {
            try {
                mGolosServicesGateWay.auth(appUserData.userName ?: return@execute)
                isAuthComplete = true
                isAuthInProgress = false
                onAuthComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                isAuthInProgress = false
            }
        }
    }

    @WorkerThread
    private fun onAuthComplete() {
        subscribeToPushIfNeeded()
        requestEventsUpdate()
    }

    private fun subscribeToPushIfNeeded() {
        if (!isAuthComplete) return
        workerExecutor.execute {
            val token = tokenProvider.tokenLiveData.value ?: return@execute
            Timber.i("token = $token")
            if (!needToSubscribeToPushes(token)) return@execute
            try {
                mGolosServicesGateWay.subscribeOnNotifications(token.newToken)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun needToSubscribeToPushes(tokens: FCMTokens): Boolean {
        if (tokens.oldToken == null && !notificationsPersister.isUserSubscribedOnNotificationsThroughServices()) return true
        if (tokens.oldToken != null && (tokens.oldToken != tokens.newToken)) return true
        return false
    }


    override fun getEvents(eventType: EventType?): LiveData<List<GolosEvent>> {
        return if (eventType == null) allEvents
        else mEventsMap[eventType]!!
    }

    override fun requestEventsUpdate(eventType: EventType?, fromId: String?, limit: Int?) {
        workerExecutor.execute {
            try {
                val golosEvents = mGolosServicesGateWay.getEvents(fromId, eventType, limit)
                val liveData = if (eventType == null) allEvents else mEventsMap[eventType]!!
                val collectionInLiveData = liveData.value.orEmpty()

                if (collectionInLiveData.isEmpty()) {
                    mainThreadExecutor.execute { liveData.value = golosEvents }

                } else {
                    val set = TreeSet<GolosEvent>()
                    set.addAll(golosEvents)
                    set.addAll(collectionInLiveData)

                    mainThreadExecutor.execute { liveData.value = set.toList() }
                }
                if (eventType == null) {
                    val eventsMap = groupEventsByType(golosEvents)
                    val set = TreeSet<GolosEvent>()

                    eventsMap.forEach {
                        if (it.value.isEmpty()) return@forEach
                        set.clear()
                        set.addAll(it.value)
                        set.addAll(mEventsMap[it.key]!!.value.orEmpty())
                        mEventsMap[it.key]!!.value = set.toList()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun groupEventsByType(list: List<GolosEvent>): Map<EventType, List<GolosEvent>> {
        return list.groupBy {
            when (it) {
                is GolosVoteEvent -> EventType.VOTE
                is GolosFlagEvent -> EventType.FLAG
                is GolosTransferEvent -> EventType.TRANSFER
                is GolosSubscribeEvent -> EventType.SUBSCRIBE
                is GolosUnSubscribeEvent -> EventType.UNSUBSCRIBE
                is GolosReplyEvent -> EventType.REPLY
                is GolosRepostEvent -> EventType.REPOST
                is GolosMentionEvent -> EventType.MENTION
                is GolosAwardEvent -> EventType.AWARD
                is GolosCuratorAwardEvent -> EventType.CURATOR_AWARD
                is GolosMessageEvent -> EventType.MESSAGE
                is GolosWitnessVoteEvent -> EventType.WITNESS_VOTE
                is GolosWitnessCancelVoteEvent -> EventType.WITNESS_CANCEL_VOTE
            }
        }
    }


}