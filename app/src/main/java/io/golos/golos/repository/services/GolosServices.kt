package io.golos.golos.repository.services

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import io.golos.golos.repository.Preloadable
import io.golos.golos.repository.UserDataProvider
import io.golos.golos.repository.model.AppSettings
import io.golos.golos.repository.model.NotificationSettings
import io.golos.golos.repository.model.PreparingState
import io.golos.golos.repository.services.model.*
import io.golos.golos.utils.*
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.collections.set

interface ServiceLogoutListener {
    @WorkerThread
    fun onLogout()
}

interface LogoutEventEmitter {
    fun addLogoutListener(listener: ServiceLogoutListener)
}

interface GolosSettingsService : Preloadable {
    val notificationSettings: LiveData<NotificationSettings>
    val appSettings: LiveData<AppSettings>
    @MainThread
    fun setNotificationSettings(deviceId: String, newSettings: NotificationSettings)

    @MainThread
    fun setAppSettings(deviceId: String, newSettings: AppSettings)

    @MainThread
    fun requestNotificationSettingsUpdate(deviceId: String)

    @MainThread
    fun requestAppSettingsUpdate(deviceId: String)
}

interface GolosPushService : Preloadable, LogoutEventEmitter {
    @MainThread
    fun subscribeOnPushNotifications(fcmToken: String, deviceId: String)

    @MainThread
    fun unsubscribeFromPushNotificationsAsync(fcmToken: String, deviceId: String)

    @WorkerThread
    fun unsubscribeFromPushNotificationsSync(fcmToken: String, deviceId: String)
}

interface GolosServices : Preloadable, GolosSettingsService, GolosPushService {

    @MainThread
    fun setUp()

    @MainThread
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

    fun markAllAsRead()
}

class GolosServicesImpl(golosServicesGateWay: GolosServicesGateWay? = null,
                        val userDataProvider: UserDataProvider,
                        private val workerExecutor: Executor = Executors.newSingleThreadExecutor(),
                        private val mainThreadExecutor: Executor = MainThreadExecutor(),
                        private var errorLogger: ExceptionLogger = FabricExceptionLogger) : GolosServices {

    private val mNotificationSettingsLiveData = MutableLiveData<NotificationSettings>()
    private val mAppSettingsLiveData = MutableLiveData<AppSettings>()
    private val mReadyState = MutableLiveData<PreparingState>()
    private val mListeners = HashSet<ServiceLogoutListener>()


    @Volatile
    private var isAuthComplete: Boolean = false
    @Volatile
    private var isAuthInProgress: Boolean = false

    init {
        mReadyState.value = PreparingState.LOADING
    }

    override fun addLogoutListener(listener: ServiceLogoutListener) {
        if (!mListeners.contains(listener)) mListeners.add(listener)
    }

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
    override val notificationSettings: LiveData<NotificationSettings>
        get() = mNotificationSettingsLiveData
    override val appSettings: LiveData<AppSettings>
        get() = mAppSettingsLiveData

    private val allEventsLiveData = MutableLiveData<List<GolosEvent>>()
    private val mUnreadCountLiveData = MutableLiveData<Int>()
    private val mEventsListsHashMap = HashMap<List<EventType>, LiveData<List<GolosEvent>>>()


    override fun setNotificationSettings(deviceId: String, newSettings: NotificationSettings) {
        if (newSettings == mNotificationSettingsLiveData.value) return
        if (!isAuthComplete) return
        workerExecutor.execute {
            runWithRetryAndCatch(Runnable {
                mGolosServicesGateWay.setNotificationSettings(deviceId, newSettings)
                mainThreadExecutor.execute {
                    mNotificationSettingsLiveData.value = newSettings
                }
            })
        }
    }

    override fun requestNotificationSettingsUpdate(deviceId: String) {
        if (!isAuthComplete) return
        workerExecutor.execute {
            runWithRetryAndCatch(Runnable {
                val settings = mGolosServicesGateWay.getSettings(deviceId)
                mainThreadExecutor.execute { mNotificationSettingsLiveData.value = settings.push?.show }
            })
        }
    }

    override fun setAppSettings(deviceId: String, newSettings: AppSettings) {
        if (newSettings == mAppSettingsLiveData.value) return
        if (!isAuthComplete) return
        workerExecutor.execute {
            runWithRetryAndCatch(Runnable {
                mGolosServicesGateWay.setAppSettings(deviceId, newSettings)
                mainThreadExecutor.execute {
                    mAppSettingsLiveData.value = newSettings
                }
            })
        }
    }

    override fun requestAppSettingsUpdate(deviceId: String) {
        if (!isAuthComplete) return
        workerExecutor.execute {
            runWithRetryAndCatch(Runnable {
                val settings = mGolosServicesGateWay.getSettings(deviceId)
                mainThreadExecutor.execute { mAppSettingsLiveData.value = settings.basic }
            })
        }
    }

    override fun subscribeOnPushNotifications(fcmToken: String, deviceId: String) {
        if (!isAuthComplete) return
        workerExecutor.execute {
            runWithRetryAndCatch(Runnable {
                mGolosServicesGateWay.subscribeOnNotifications(deviceId, fcmToken)
            })
        }
    }

    @WorkerThread
    private fun runWithRetryAndCatch(runnable: Runnable) {

        try {
            runnable.run()
        } catch (e: java.lang.Exception) {
            logException(e)
            if (reauthIfNeeded(e)) runWithRetryAndCatch(runnable)
        }
    }

    override fun unsubscribeFromPushNotificationsAsync(fcmToken: String, deviceId: String) {
        if (!isAuthComplete) return
        workerExecutor.execute {
            runWithRetryAndCatch(Runnable {
                mGolosServicesGateWay.unSubscribeOnNotifications(deviceId, fcmToken)
            })
        }
    }

    override fun unsubscribeFromPushNotificationsSync(fcmToken: String, deviceId: String) {
        mGolosServicesGateWay.unSubscribeOnNotifications(deviceId, fcmToken)
    }

    override fun setUp() {
        instance = this
        userDataProvider.appUserData.observeForever {
            onAuthStateChanged()
        }
    }

    override val loadingState: LiveData<PreparingState>
        get() = mReadyState

    override fun markAllAsRead() {
        workerExecutor.execute {
            runWithRetryAndCatch(Runnable {
                mGolosServicesGateWay.markAllEventsAsRead()
                val allEventsResult = allEventsLiveData.value.orEmpty().map { if (it.fresh) it.setRead(true) else it }
                val mEventsResult = mEventsMap.mapValues { liveData -> liveData.value.value.orEmpty().map { if (it.fresh) it.setRead(true) else it } }
                mainThreadExecutor.execute {
                    mUnreadCountLiveData.value = 0
                    allEventsLiveData.value = allEventsResult
                    mEventsMap.forEach {
                        it.value.value = mEventsResult[it.key]
                    }
                }
            })
        }
    }

    override fun markAsRead(eventsIds: List<String>) {

        val allEvents = allEventsLiveData.value.orEmpty().associateBy { it.id }

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
            runWithRetryAndCatch(Runnable {
                mGolosServicesGateWay.markAsRead(copy)
                val unreadCount = mGolosServicesGateWay.getUnreadCount()
                val allEventsResult = setRead(eventsIds, allEventsLiveData.value.orEmpty())
                val mEventsResult = mEventsMap.mapValues { liveData -> setRead(eventsIds, liveData.value.value.orEmpty()) }

                mainThreadExecutor.execute {
                    mUnreadCountLiveData.value = unreadCount
                    if (allEventsResult.isChanged) allEventsLiveData.value = allEventsResult.resultingList
                    mEventsResult.forEach {
                        if (it.value.isChanged) {
                            mEventsMap[it.key]?.value = it.value.resultingList
                        }
                    }
                }
            })
        }
    }

    private class SettingReadChangeResult(val isChanged: Boolean, val resultingList: List<GolosEvent>)

    private fun setRead(ids: List<String>, events: List<GolosEvent>): SettingReadChangeResult {
        val eventListCopy = events.toArrayList()
        var isChanged = false
        eventListCopy.forEachIndexed { index, golosEvent ->
            if (ids.contains(golosEvent.id)) {
                eventListCopy[index] = golosEvent.setRead(true)
                isChanged = true
            }
        }
        return SettingReadChangeResult(isChanged, eventListCopy)
    }

    override fun getRequestStatus(forType: EventType?): LiveData<UpdatingState> {
        return mStatus[forType]!!
    }

    override fun getFreshEventsCount(): LiveData<Int> {
        return mUnreadCountLiveData
    }

    private fun onAuthStateChanged() {
        val appUserData = userDataProvider.appUserData.value
        if (appUserData == null || !appUserData.isLogged) {
            if (isAuthComplete) {
                workerExecutor.execute {
                    mListeners.forEach { it.onLogout() }
                    mGolosServicesGateWay.logout()
                    isAuthComplete = false
                    isAuthInProgress = false

                    mainThreadExecutor.execute {
                        mEventsMap.forEach { it.value.value = null }
                        allEventsLiveData.value = null
                        mUnreadCountLiveData.value = 0
                        mReadyState.value = PreparingState.LOADING
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
        mainThreadExecutor.execute {
            mReadyState.value = PreparingState.DONE
            requestEventsUpdate(markAsRead = false, completionHandler = { _, _ -> })
        }
    }


    override fun getEvents(eventType: List<EventType>?): LiveData<List<GolosEvent>> {
        return if (eventType == null) {
            allEventsLiveData
        } else {
            if (mEventsListsHashMap.containsKey(eventType)) return mEventsListsHashMap[eventType]!!

            val liveData = MediatorLiveData<List<GolosEvent>>()
            mEventsListsHashMap[eventType] = liveData

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
                                allEventsLiveData.value = allEventsLiveData.value
                                mStatus[null]!!.value = UpdatingState.DONE
                            }

                    }

                    if (events.isNotEmpty()) {
                        set.clear()
                        set.addAll(allEventsLiveData.value ?: emptyList())
                        set.addAll(events)
                        val sortedEvents = set.toList().sorted()
                        mainThreadExecutor.execute {
                            allEventsLiveData.value = sortedEvents

                            mStatus[null]!!.value = UpdatingState.DONE
                        }
                    }

                    mainThreadExecutor.execute {
                        mUnreadCountLiveData.value = golosEvents.freshCount
                        completionHandler(Unit, null)
                    }

                    if (allEventsLiveData.value.orEmpty().size > limit && events.size != limit && eventTypes == null) {
                        markAllAsRead()
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
                "auth counter = ${(mGolosServicesGateWay as GolosServicesGateWayImpl?)?.authCounter}"
    }

    private fun logException(e: java.lang.Exception) {
        Timber.e(e)
        errorLogger.log(e)
    }

    companion object {
        var instance: GolosServicesImpl? = null
    }

}