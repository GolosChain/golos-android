package io.golos.golos.repository.services

import android.support.annotation.WorkerThread
import io.golos.golos.BuildConfig
import io.golos.golos.utils.JsonRpcError
import timber.log.Timber
import java.util.*
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
    fun getEvents(fromId: String?, eventType: EventType?, limit: Int? = 100): List<GolosEvent>

}

class GolosServicesGateWayImpl(private val communicationHandler: GolosServicesCommunicator
                               = GolosServicesSocketHandler(BuildConfig.GATE_URL),
                               private val signHandler: SecretSigner
                               = GolosSecretSigner()) : GolosServicesGateWay {


    private enum class ServicesMethod {
        AUTH, PUSH_SUBSCRIBE, GET_NOTIFS_HISTORY;

        fun stringRepresentation() =
                when (this) {
                    AUTH -> this.toString().toLowerCase()
                    PUSH_SUBSCRIBE -> "pushNotifyOn"
                    GET_NOTIFS_HISTORY -> "getNotifyHistory"
                }
    }

    private val authCounter = AtomicInteger(1)

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
        communicationHandler.requestAuth()
        synchronized(this, {
            if (authLatch.count == 0L) authLatch = CountDownLatch(1)
            authLatch.await()
        })


        val authRequest = authRequest ?: throw IllegalStateException("multithreading not supported")

        try {
            val authResult = communicationHandler.sendMessage(GolosAuthRequest(userName,
                    signHandler.sign(userName, authRequest.secret)), ServicesMethod.AUTH.stringRepresentation())
            println("$authResult")
            if (authResult.isAuthSuccessMessage()) {

            } else {
                communicationHandler.requestAuth()
            }
        } catch (e: GolosServicesException) {
            Timber.e("error $e")
            if (e.getErrorType() == JsonRpcError.AUTH_ERROR) {
                if (authCounter.incrementAndGet() < 20)
                    auth(userName)
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

    override fun getEvents(fromId: String?, eventType: EventType?, limit: Int?): List<GolosEvent> {
        val types: String = if (eventType == null) "all" else Arrays.toString(arrayOf(eventType.toString()))
        return communicationHandler.sendMessage(GolosEventRequest(fromId, limit ?: 40,
                types), ServicesMethod.GET_NOTIFS_HISTORY.stringRepresentation())
                .getEventData()
                .map { GolosEvent.fromEvent(it) }
    }
}



