package io.golos.golos.notifications

import eu.bittrade.libs.golosj.Golos4J
import io.golos.golos.BuildConfig
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class GolosServicesApi(private val communicationHandler: GolosServicesCommunicator
                       = GolosServicesSocketHandler(BuildConfig.GATE_URL),
                       private val signHandler: SecretSigner
                       = GolosSecretSigner(Golos4J.getInstance().databaseMethods),
                       private val workerExecutor: Executor = Executors.newSingleThreadExecutor()) {

    @Volatile
    private var isAuthComplete: Boolean = false

    fun authForUser(userName: String) {
        workerExecutor.execute {
            (communicationHandler as? GolosServicesSocketHandler)?.onServiceNotification = {
                when (it) {
                    is GolosAuthRequest -> workerExecutor.execute {
                        communicationHandler.sendMessage(GolosAuthResponse(userName,
                                signHandler.sign(userName, it.secret ?: return@execute)))
                    }
                }
            }
            communicationHandler.requestAuth()
        }
    }
}