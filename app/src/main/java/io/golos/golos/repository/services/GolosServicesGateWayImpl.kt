package io.golos.golos.repository.services

import androidx.annotation.WorkerThread
import io.golos.golos.BuildConfig
import io.golos.golos.repository.model.AppSettings
import io.golos.golos.repository.model.NotificationSettings
import io.golos.golos.repository.services.model.*
import io.golos.golos.utils.JsonRpcError
import io.golos.golos.utils.rpcErrorFromCode
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

interface GolosServicesGateWay {

    @WorkerThread
    fun auth(userName: String)

    @WorkerThread
    fun logout()

    @WorkerThread
    fun subscribeOnNotifications(deviceId: String, fcmToken: String)

    fun unSubscribeOnNotifications(deviceId: String, fcmToken: String)

    @WorkerThread
    fun getEvents(fromId: String?,
                  eventType: List<EventType>?,
                  markAsRead: Boolean,
                  limit: Int? = 100): GolosEvents

    @WorkerThread
    fun markAsRead(ids: List<String>)

    @WorkerThread
    fun getUnreadCount(): Int

    fun markAllEventsAsRead()
    fun setNotificationSettings(deviceId: String, newSettings: NotificationSettings)
    fun setAppSettings(deviceId: String, newSettings: AppSettings)

    fun getSettings(deviceId: String): GolosServicesSettings
}


class GolosServicesGateWayImpl(private val communicationHandler: GolosServicesCommunicator
                               = GolosServicesSocketHandler(BuildConfig.GATE_URL),
                               private val signHandler: SecretSigner
                               = GolosSecretSigner()) : GolosServicesGateWay {


    private enum class ServicesMethod {
        AUTH, PUSH_SUBSCRIBE, PUSH_UNSUBSCRIBE, GET_NOTIFS_HISTORY, MARK_VIEWED, GET_UNREAD_COUNT, SECRET_REQUEST, MARK_VIEWED_ALL, SET_SETTINGS, GET_SETTINGS;

        fun stringRepresentation() =
                when (this) {
                    AUTH -> this.toString().toLowerCase()
                    PUSH_SUBSCRIBE -> "push.notifyOn"
                    PUSH_UNSUBSCRIBE -> "push.notifyOff"
                    GET_NOTIFS_HISTORY -> "getNotifyHistory"
                    MARK_VIEWED -> "notify.markAsViewed"
                    GET_UNREAD_COUNT -> "getNotifyHistoryFresh"
                    SECRET_REQUEST -> "getSecret"
                    MARK_VIEWED_ALL -> "notify.markAllAsViewed"
                    SET_SETTINGS -> "setOptions"
                    GET_SETTINGS -> "getOptions"
                }
    }

    val authCounter = AtomicInteger(1)
    private var userName: String? = null


    override fun auth(userName: String) {
        communicationHandler.connect()
        authInternal(userName)
    }

    private fun authInternal(userName: String) {
        val secret = communicationHandler.sendMessage(GetSecretRequest(), ServicesMethod.SECRET_REQUEST.stringRepresentation())

        try {
            val authResult = communicationHandler.sendMessage(GolosAuthRequest(userName,
                    signHandler.sign(userName, secret.getSecret())), ServicesMethod.AUTH.stringRepresentation())

            if (authResult.isAuthSuccessMessage()) {
                this.userName = userName
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

    override fun setNotificationSettings(deviceId: String, newSettings: NotificationSettings) {
        val currentLang = Locale.getDefault()?.language ?: ""
        val request = GolosSettingChangeRequest(deviceId, null, null, GolosServicePushSettings(
                if (currentLang.contains("ru")) GolosServiceSettingsLanguage.RUSSIAN else GolosServiceSettingsLanguage.ENGLISH,
                newSettings
        ))
        communicationHandler.sendMessage(request,
                ServicesMethod.SET_SETTINGS.stringRepresentation())
    }

    override fun setAppSettings(deviceId: String, newSettings: AppSettings) {
        val request = GolosSettingChangeRequest(deviceId, newSettings, null, null)
        communicationHandler.sendMessage(request,
                ServicesMethod.SET_SETTINGS.stringRepresentation())
    }

    override fun getSettings(deviceId: String): GolosServicesSettings {
        return communicationHandler.sendMessage(GolosServicesSettingsRequest(deviceId),
                ServicesMethod.GET_SETTINGS.stringRepresentation()).getSettings()
    }

    override fun subscribeOnNotifications(deviceId: String, fcmToken: String) {
        val request = GolosPushSubscribeRequest(fcmToken, deviceId)
        communicationHandler.sendMessage(request,
                ServicesMethod.PUSH_SUBSCRIBE.stringRepresentation())
    }

    override fun unSubscribeOnNotifications(deviceId: String, fcmToken: String) {
        val request = GolosPushSubscribeRequest(fcmToken, deviceId)
        communicationHandler.sendMessage(request,
                ServicesMethod.PUSH_UNSUBSCRIBE.stringRepresentation())
    }

    override fun logout() {
        communicationHandler.dropConnection()
        authCounter.set(1)
        userName = null
    }

    override fun getEvents(fromId: String?, eventType: List<EventType>?, markAsRead: Boolean, limit: Int?): GolosEvents {
        val request = if (eventType == null) GolosAllEventRequest(fromId, limit
                ?: 15, if (markAsRead) null else false)
        else GolosEventRequest(fromId, limit
                ?: 15, eventType.map { it.toString() }, if (markAsRead) null else false)
        return sendMessage(request, ServicesMethod.GET_NOTIFS_HISTORY)
                .getEventData()
    }

    private fun sendMessage(request: GolosServicesRequest, method: ServicesMethod): GolosServicesResponse {
        return try {
            communicationHandler.sendMessage(request, method.stringRepresentation())
        } catch (e: Exception) {

            if (e !is GolosServicesException) throw e

            val errorCode = rpcErrorFromCode(e.golosServicesError.code)
            if (errorCode == JsonRpcError.AUTH_ERROR && userName != null) {

                authInternal(userName!!)
                return sendMessage(request, method)
            } else throw e
        }
    }

    override fun markAsRead(ids: List<String>) {
        sendMessage(MarkAsReadRequest(ids),
                ServicesMethod.MARK_VIEWED)
    }

    override fun markAllEventsAsRead() {
        sendMessage(MarkAllReadRequest(),
                ServicesMethod.MARK_VIEWED_ALL)
    }

    override fun getUnreadCount(): Int {
        return sendMessage(GetUnreadCountRequest(),
                ServicesMethod.GET_UNREAD_COUNT).getUnreadCount()
    }
}





