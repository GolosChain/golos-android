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
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.collections.HashMap
import kotlin.collections.HashSet
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
            limit: Int = 15,
            markAsRead: Boolean,
            completionHandler: (Unit, GolosError?) -> Unit)

    @MainThread
    fun getRequestStatus(forType: EventType?): LiveData<UpdatingState>

    fun getFreshEventsCount(): LiveData<Int>

    fun markAsRead(eventsIds: List<String>)

}

class GolosServicesImpl(
        private val tokenProvider: FCMTokenProvider = FCMTokenProviderImpl,
        private val userDataProvider: UserDataProvider = Repository.get,
        private val notificationsPersister: NotificationsPersister = Persister.get,
        golosServicesGateWay: GolosServicesGateWay? = null,
        private val workerExecutor: Executor = Executors.newSingleThreadExecutor(),
        private val mainThreadExecutor: Executor = MainThreadExecutor()) : GolosServices {


    private val mSendingQue = Collections.synchronizedSet(HashSet<String>())

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
    private val mStatus: HashMap<EventType?, MutableLiveData<UpdatingState>> = HashMap<EventType?, MutableLiveData<UpdatingState>>().apply {
        EventType.values().forEach { eventType ->
            this[eventType] = MutableLiveData()
            this[eventType]!!.value = UpdatingState.DONE
        }
        this[null] = MutableLiveData()
        this[null]!!.value = UpdatingState.DONE
    }

    private val allEvents = MutableLiveData<List<GolosEvent>>()
    private val mUnreadCountLiveData = MutableLiveData<Int>()


    override fun setUp() {
        tokenProvider.tokenLiveData.observeForever {
            onTokenOrAuthStateChanged()
        }
        userDataProvider.appUserData.observeForever {
            onTokenOrAuthStateChanged()
        }

        instance = this
    }

    override fun markAsRead(eventsIds: List<String>) {

        val allEvents = allEvents.value.orEmpty().associateBy { it.id }

        val copy = eventsIds.toArrayList()
        eventsIds.forEach {
            val event = allEvents[it] ?: return@forEach
            if (!event.fresh) {

                copy.remove(it)
            }
        }
        if (copy.isEmpty()) {

            return
        }

        workerExecutor.execute {

            try {
                mGolosServicesGateWay.markAsRead(copy)

                val unreadCount = mGolosServicesGateWay.getUnreadCount()
                mainThreadExecutor.execute {
                    mUnreadCountLiveData.value = unreadCount
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (reauthIfNeeded(e)) markAsRead(eventsIds)
            }

        }
    }

    override fun getRequestStatus(forType: EventType?): LiveData<UpdatingState> {
        return mStatus[forType]!!
    }

    override fun getFreshEventsCount(): LiveData<Int> {
        return mUnreadCountLiveData
    }

    private fun onTokenOrAuthStateChanged() {
        val appUserData = userDataProvider.appUserData.value
        if (appUserData == null || !appUserData.isLogged) {
            if (isAuthComplete) {
                workerExecutor.execute {
                    mGolosServicesGateWay.logout()
                    notificationsPersister.setUserSubscribedOnNotificationsThroughServices(false)
                    isAuthComplete = false
                    isAuthInProgress = false

                    mainThreadExecutor.execute {
                        mEventsMap.forEach { it.value.value = null }
                        allEvents.value = null
                    }
                }
            }
            return
        }
        if (isAuthComplete) return
        if (isAuthInProgress) return
        isAuthInProgress = true
        workerExecutor.execute {
            try {
                mGolosServicesGateWay.auth(appUserData.name)
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
        mainThreadExecutor.execute {
            requestEventsUpdate(markAsRead = false, completionHandler = { _, _ -> })
        }
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
            val allData = HashSet<GolosEvent>()
            val set = HashSet<GolosEvent>()

            eventType.forEach {
                liveData.addSource(mEventsMap[it] as LiveData<List<GolosEvent>>) {
                    set.clear()
                    set.addAll(liveData.value ?: emptyList())
                    set.addAll(it ?: emptyList())
                    liveData.value = set.toList().sorted()
                }
                allData.addAll(mEventsMap[it]!!.value.orEmpty())
            }

            liveData.value = allData.toList().sorted()
            liveData
        }
    }

    @AnyThread
    override fun requestEventsUpdate(eventTypes: List<EventType>?,
                                     fromId: String?,
                                     limit: Int,
                                     markAsRead: Boolean,
                                     completionHandler: (Unit, GolosError?) -> Unit) {
        if (!isAuthComplete) return
        mainThreadExecutor.execute {
            if (eventTypes == null) mStatus[null]?.value = UpdatingState.UPDATING
            else {
                eventTypes.forEach {
                    mStatus[it]?.value = UpdatingState.UPDATING
                }
            }
            workerExecutor.execute {
                try {
                    val golosEvents = mGolosServicesGateWay.getEvents(fromId, eventTypes, markAsRead, limit)
                    val events = golosEvents.events

                    val eventsMap = groupEventsByType(events)
                    val set = HashSet<GolosEvent>()

                    eventsMap.forEach {
                        set.clear()
                        set.addAll(it.value)
                        set.addAll(mEventsMap[it.key]!!.value.orEmpty())
                        val list = set.toMutableList()
                        list.sorted()
                        mainThreadExecutor.execute {
                            mEventsMap[it.key]!!.value = list
                            mStatus[it.key]!!.value = UpdatingState.DONE
                        }
                    }
                    if (eventsMap.isEmpty()) {
                        eventTypes?.forEach {
                            mainThreadExecutor.execute {
                                mEventsMap[it]!!.value = mEventsMap[it]!!.value
                                mStatus[it]!!.value = UpdatingState.DONE
                            }
                        }
                        if (eventTypes == null)
                            mainThreadExecutor.execute {
                                allEvents.value = allEvents.value
                                mStatus[null]!!.value = UpdatingState.DONE
                            }

                    }

                    if (events.isNotEmpty()) {
                        set.clear()
                        set.addAll(allEvents.value ?: emptyList())
                        set.addAll(events)
                        val sortedEvents = set.toList().sorted()
                        mainThreadExecutor.execute {
                            allEvents.value = sortedEvents

                            mStatus[null]!!.value = UpdatingState.DONE
                        }
                    }

                    mainThreadExecutor.execute {
                        mUnreadCountLiveData.value = golosEvents.freshCount
                        completionHandler(Unit, null)
                    }

                    if (events.size != limit && eventTypes == null) {
                        mGolosServicesGateWay.markAllEventsAsRead()
                        mainThreadExecutor.execute {
                            mUnreadCountLiveData.value = 0
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()

                    if (reauthIfNeeded(e)) requestEventsUpdate(eventTypes, fromId, limit, markAsRead, completionHandler)
                    else {
                        mainThreadExecutor.execute { completionHandler(Unit, GolosErrorParser.parse(e)) }
                    }

                }
            }
        }
    }


    private fun reauthIfNeeded(e: Exception): Boolean {
        if (e !is GolosServicesException) return false

        val errorCode = rpcErrorFromCode(e.golosServicesError.code)
        if ((errorCode == JsonRpcError.BAD_REQUEST || errorCode == JsonRpcError.AUTH_ERROR)
                && userDataProvider.appUserData.value?.isLogged == true) {

            isAuthComplete = false
            isAuthInProgress = true
            mGolosServicesGateWay.auth(userDataProvider.appUserData.value?.name
                    ?: return false)
            isAuthComplete = true
            isAuthInProgress = false

            return true
        } else if (userDataProvider.appUserData.value?.isLogged != true) {
            mGolosServicesGateWay.logout()
            isAuthComplete = false
            isAuthInProgress = false
        }
        return false
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
                is GolosAwardEvent -> EventType.REWARD
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