package io.golos.golos.notifications

import android.arch.lifecycle.Observer
import eu.bittrade.libs.golosj.Golos4J
import io.golos.golos.BuildConfig
import io.golos.golos.repository.Repository
import io.golos.golos.repository.UserDataProvider
import io.golos.golos.repository.model.NotificationsPersister
import io.golos.golos.repository.persistence.Persister
import io.golos.golos.repository.persistence.model.AppUserData
import io.golos.golos.utils.JsonRpcError
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

interface GolosServicesGateWay {
    fun setUp()

    fun onUserLogout()
}

class GolosServicesInteractionManager(private val communicationHandler: GolosServicesCommunicator
                                      = GolosServicesSocketHandler(BuildConfig.GATE_URL),
                                      private val signHandler: SecretSigner
                                      = GolosSecretSigner(Golos4J.getInstance().databaseMethods),
                                      private val tokenProvider: FCMTokenProvider = FCMTokenProviderImpl,
                                      private val userDataProvider: UserDataProvider = Repository.get,
                                      private val notificationsPersister: NotificationsPersister = Persister.get,
                                      private val workerExecutor: Executor = Executors.newSingleThreadExecutor()) : GolosServicesGateWay {

    private enum class ServicesMethod {
        AUTH, PUSH_SUBSCRIBE;

        fun stringRepresentation() =
                when (this) {
                    AUTH -> this.toString().toLowerCase()
                    PUSH_SUBSCRIBE -> "pushNotifyOn"
                }
    }

    @Volatile
    private var isAuthComplete: Boolean = false
    @Volatile
    private var isAuthInProgress: Boolean = false
    private val authCounter = AtomicInteger(1)


    override fun setUp() {
        tokenProvider.onTokenChange.observeForever(object : Observer<FCMTokens> {
            override fun onChanged(t: FCMTokens?) {
                onTokenChanged()
            }
        })
        userDataProvider.appUserData.observeForever(object : Observer<AppUserData> {
            override fun onChanged(t: AppUserData?) {
                onChanged()
            }
        })

    }

    private fun onChanged() {
        val appUserData = userDataProvider.appUserData.value ?: return
        if (!appUserData.isUserLoggedIn) return
        if (isAuthComplete) return
        if (isAuthInProgress) return


        (communicationHandler as? GolosServicesSocketHandler)?.onServiceNotification = {
            when (it) {
                is GolosServerAuthRequest -> {
                    workerExecutor.execute {
                        try {
                            val authResult = communicationHandler.sendMessage(GolosAuthRequest(appUserData.userName
                                    ?: return@execute,
                                    signHandler.sign(appUserData.userName
                                            ?: return@execute, it.secret
                                            ?: return@execute)), ServicesMethod.AUTH.stringRepresentation())
                            println("$authResult")
                            if (authResult.isAuthSuccessMessage()) {

                                isAuthComplete = true
                                isAuthInProgress = false
                                onAuthComplete()
                            } else {
                                communicationHandler.requestAuth()
                            }
                        } catch (e: GolosServicesException) {
                            Timber.e("error $e")
                            if (e.getErrorType() == JsonRpcError.AUTH_ERROR) {
                                if (authCounter.incrementAndGet() < 20)
                                    workerExecutor.execute { communicationHandler.requestAuth() }
                            }
                        }
                    }
                }
            }
        }
        isAuthInProgress = true
        workerExecutor.execute {
            communicationHandler.requestAuth()
        }
    }

    override fun onUserLogout() {
        workerExecutor.execute {
            communicationHandler.dropConnection()
            notificationsPersister.setUserSubscribedOnNotificationsThroughServices(false)
            isAuthComplete = false
            isAuthInProgress = false
            authCounter.set(0)
        }
    }

    private fun onAuthComplete() {
        Timber.i("onAuthComplete")

        subscribeToPushIfNeeded()
    }

    private fun subscribeToPushIfNeeded() {
        if (!isAuthComplete) return
        workerExecutor.execute {
            val token = tokenProvider.onTokenChange.value ?: return@execute
            Timber.i("token = $token")
            if (!needToSubscribeToPushes(token)) return@execute
            try {
                val request = GolosPushSubscribeRequest(token.newToken)
                val result = communicationHandler.sendMessage(request,
                        ServicesMethod.PUSH_SUBSCRIBE.stringRepresentation())

                if (result.isPushSubscribeSuccesMessage()) notificationsPersister.setUserSubscribedOnNotificationsThroughServices(true)
                Timber.i("subscribed")

            } catch (e: GolosServicesException) {
                e.printStackTrace()
            }
        }
    }

    private fun needToSubscribeToPushes(tokens: FCMTokens): Boolean {
        if (tokens.oldToken == null && !notificationsPersister.isUserSubscribedOnNotificationsThroughServices()) return true
        if (tokens.oldToken != null && (tokens.oldToken != tokens.newToken)) return true
        return false
    }

    private fun onTokenChanged() {
        subscribeToPushIfNeeded()
    }

    override fun toString(): String {
        return "isAuthComplete = $isAuthComplete " +
                "\nisAuthInProgress = $isAuthInProgress " +
                "\nauthCounter = ${authCounter.get()}\nisUserSubscribedOnNotificationsThroughServices()" +
                " = ${notificationsPersister.isUserSubscribedOnNotificationsThroughServices()}\n\n" +
                "tokenProvider token = ${tokenProvider.onTokenChange.value}"
    }

}



