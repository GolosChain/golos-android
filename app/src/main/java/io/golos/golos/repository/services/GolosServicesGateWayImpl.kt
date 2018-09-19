package io.golos.golos.repository.services

import android.support.annotation.WorkerThread
import io.golos.golos.BuildConfig
import io.golos.golos.utils.JsonRpcError
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

interface GolosServicesGateWay {

    @WorkerThread
    fun auth(userName: String)

    @WorkerThread
    fun logout()

    @WorkerThread
    fun subscribeOnNotifications(fcmToken: String)

    @WorkerThread
    fun getEvents(fromId: String?,
                  eventType: List<EventType>?,
                  markAsRead: Boolean,
                  limit: Int? = 100): GolosEvents

    @WorkerThread
    fun markAsRead(ids: List<String>)

    @WorkerThread
    fun getUnreadCount(): Int
}


class GolosServicesGateWayImpl(private val communicationHandler: GolosServicesCommunicator
                               = GolosServicesSocketHandler(BuildConfig.GATE_URL),
                               private val signHandler: SecretSigner
                               = GolosSecretSigner()) : GolosServicesGateWay {


    private enum class ServicesMethod {
        AUTH, PUSH_SUBSCRIBE, GET_NOTIFS_HISTORY, MARK_VIEWED, GET_UNREAD_COUNT, SECRET_REQUEST;

        fun stringRepresentation() =
                when (this) {
                    AUTH -> this.toString().toLowerCase()
                    PUSH_SUBSCRIBE -> "pushNotifyOn"
                    GET_NOTIFS_HISTORY -> "getNotifyHistory"
                    MARK_VIEWED -> "markAsViewed"
                    GET_UNREAD_COUNT -> "getNotifyHistoryFresh"
                    SECRET_REQUEST -> "getSecret"
                }
    }

    val authCounter = AtomicInteger(1)

    @Volatile
    private var authRequest: GolosServerAuthRequest? = null

    @Volatile
    private var authLatch: CountDownLatch = CountDownLatch(1)


    override fun auth(userName: String) {
        (communicationHandler as? GolosServicesSocketHandler)?.onServiceNotification = {
            when (it) {
                is GolosServerAuthRequest -> {
                    authRequest = it
                    authLatch.countDown()
                }
            }
        }
        communicationHandler.connect()
        synchronized(this) {
            if (authLatch.count == 0L) authLatch = CountDownLatch(1)
            authLatch.await()
        }


        authRequest ?: throw IllegalStateException("multithreading not supported")
        authInternal(userName)
    }

    private fun authInternal(userName: String){
        val secret = communicationHandler.sendMessage(GetSecretRequest(), ServicesMethod.SECRET_REQUEST.stringRepresentation())

        try {
            val authResult = communicationHandler.sendMessage(GolosAuthRequest(userName,
                    signHandler.sign(userName, secret.getSecret())), ServicesMethod.AUTH.stringRepresentation())

            if (authResult.isAuthSuccessMessage()) {
                authCounter.set(1)
            } else {
                authInternal(userName)
            }
        } catch (e: GolosServicesException) {
            Timber.e("error $e")
            if (e.getErrorType() == JsonRpcError.AUTH_ERROR) {
                if (authCounter.incrementAndGet() < 30)
                    authInternal(userName)
            } else throw e
        }
    }

    override fun subscribeOnNotifications(fcmToken: String) {
        val request = GolosPushSubscribeRequest(fcmToken)
        val result = communicationHandler.sendMessage(request,
                ServicesMethod.PUSH_SUBSCRIBE.stringRepresentation())
        Timber.i(result.toString())
    }

    override fun logout() {
        communicationHandler.dropConnection()
        authCounter.set(1)
    }

    override fun getEvents(fromId: String?, eventType: List<EventType>?, markAsRead: Boolean, limit: Int?): GolosEvents {
        val request = if (eventType == null) GolosAllEventRequest(fromId, limit ?: 40, markAsRead)
        else GolosEventRequest(fromId, limit ?: 40, eventType.map { it.toString() }, markAsRead)
        return communicationHandler.sendMessage(request, ServicesMethod.GET_NOTIFS_HISTORY.stringRepresentation())
                .getEventData()
    }

    override fun markAsRead(ids: List<String>) {
        communicationHandler.sendMessage(MarkAsReadRequest(ids),
                ServicesMethod.MARK_VIEWED.stringRepresentation())
    }

    override fun getUnreadCount(): Int {
        return communicationHandler.sendMessage(GetUnreadCountRequest(),
                ServicesMethod.GET_UNREAD_COUNT.stringRepresentation()).getUnreadCount()
    }
}



