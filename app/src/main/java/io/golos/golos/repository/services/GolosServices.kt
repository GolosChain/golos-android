package io.golos.golos.repository.services

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
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
import io.golos.golos.utils.*
import timber.log.Timber
import java.util.TreeSet
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.collections.HashMap
import kotlin.collections.set

interface GolosServices {

    @MainThread
    fun setUp()


    fun getEvents(eventType: List<EventType>? = null): LiveData<List<GolosEvent>>

    @AnyThread
    fun requestEventsUpdate(
            /**null if update all events**/
            eventTypes: List<EventType>? = null,
            fromId: String? = null,
            limit: Int? = 20,
            completionHandler: (Unit, GolosError?) -> Unit)
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
        tokenProvider.tokenLiveData.observeForever {
            onTokenOrAuthStateChanged()
        }
        userDataProvider.appUserData.observeForever {
            onTokenOrAuthStateChanged()
        }

        instance = this
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
        requestEventsUpdate(completionHandler = { _, _ -> })
    }

    private fun subscribeToPushIfNeeded() {
        if (!isAuthComplete) return
        workerExecutor.execute {
            val token = tokenProvider.tokenLiveData.value ?: return@execute
            Timber.i("token = $token")
            if (!needToSubscribeToPushes(token)) return@execute
            try {
                mGolosServicesGateWay.subscribeOnNotifications(token.newToken)
                notificationsPersister.setUserSubscribedOnNotificationsThroughServices(true)

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


    override fun getEvents(eventType: List<EventType>?): LiveData<List<GolosEvent>> {
        return if (eventType == null) {
            allEvents
        } else {
            val liveData = MediatorLiveData<List<GolosEvent>>()
            val allData = TreeSet<GolosEvent>()
            val set = TreeSet<GolosEvent>()

            eventType.forEach {
                liveData.addSource(mEventsMap[it] as LiveData<List<GolosEvent>>) {
                    set.clear()
                    set.addAll(liveData.value ?: emptyList())
                    set.addAll(it ?: emptyList())
                    liveData.value = set.toList()
                }
                allData.addAll(mEventsMap[it]!!.value.orEmpty())
            }

            liveData.value = allData.toList()
            liveData
        }
    }

    override fun requestEventsUpdate(eventTypes: List<EventType>?,
                                     fromId: String?,
                                     limit: Int?,
                                     completionHandler: (Unit, GolosError?) -> Unit) {
        workerExecutor.execute {
            try {
                val golosEvents = mGolosServicesGateWay.getEvents(fromId, eventTypes, limit)
                val eventsMap = groupEventsByType(golosEvents)
                val set = TreeSet<GolosEvent>()

                eventsMap.forEach {
                    set.clear()
                    set.addAll(it.value)
                    set.addAll(mEventsMap[it.key]!!.value.orEmpty())
                    val list = set.toList()
                    mainThreadExecutor.execute {
                        mEventsMap[it.key]!!.value = list
                    }
                }
                if (eventsMap.isEmpty()) {
                    eventTypes?.forEach {
                        mainThreadExecutor.execute {
                            mEventsMap[it]!!.value = mEventsMap[it]!!.value
                        }

                    }
                    if (eventTypes == null)
                        mainThreadExecutor.execute {
                            allEvents.value = allEvents.value
                        }
                }

                if (eventTypes == null) {
                    set.addAll(allEvents.value ?: emptyList())
                    set.addAll(golosEvents)
                    mainThreadExecutor.execute {
                        allEvents.value = set.toList()
                    }
                }
                mainThreadExecutor.execute { completionHandler(Unit, null) }
            } catch (e: Exception) {
                e.printStackTrace()
                if (e is GolosServicesException) {
                    reauthIfNeeded(e)
                }
                mainThreadExecutor.execute { completionHandler(Unit, GolosErrorParser.parse(e)) }
            }
        }
    }

    private fun reauthIfNeeded(e: GolosServicesException) {
        if (rpcErrorFromCode(e.golosServicesError.code) == JsonRpcError.BAD_REQUEST
                && userDataProvider.appUserData.value?.isUserLoggedIn == true) {
            mGolosServicesGateWay.auth(userDataProvider.appUserData.value?.userName
                    ?: return)
            isAuthComplete = true
            isAuthInProgress = false
            onAuthComplete()
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

    override fun toString(): String {
        return "isAuthComplete=$isAuthComplete\n" +
                "isAuthInProgress=$isAuthInProgress\n" +
                "auth counter = ${(mGolosServicesGateWay as GolosServicesGateWayImpl?)?.authCounter}\n" +
                "isSubscribed on pushes = ${notificationsPersister.isUserSubscribedOnNotificationsThroughServices()}"
    }

    companion object {
        var instance: GolosServicesImpl? = null
    }


}