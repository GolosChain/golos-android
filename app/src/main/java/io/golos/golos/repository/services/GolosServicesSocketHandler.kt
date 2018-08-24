package io.golos.golos.repository.services

import android.support.annotation.WorkerThread
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import eu.bittrade.libs.golosj.communication.CommunicationHandler.getObjectMapper
import eu.bittrade.libs.golosj.configuration.SteemJConfig
import io.golos.golos.utils.JsonConvertable
import io.golos.golos.utils.mapper
import org.glassfish.tyrus.client.ClientManager
import org.glassfish.tyrus.client.ClientProperties
import org.glassfish.tyrus.client.SslContextConfigurator
import org.glassfish.tyrus.client.SslEngineConfigurator
import org.json.JSONException
import timber.log.Timber
import java.io.IOException
import java.lang.Exception
import java.net.URI
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.HostnameVerifier
import javax.websocket.*

class GolosServicesSocketHandler(private val gateUrl: String,
                                 maxTimeOut: Int = 45_000,
                                 @Volatile
                                 var onServiceNotification: ((GolosServicesNotification) -> Unit)? = null) : Endpoint(), MessageHandler.Whole<String>, GolosServicesCommunicator {
    private var mMapper: ObjectMapper? = getObjectMapper()

    private val mClient: ClientManager
    private var mSession: Session? = null
    @Volatile
    private var mRawJsonResponse: String? = null
    private val mLatches = Collections.synchronizedMap<Long, CountDownLatch>(hashMapOf())

    init {
        this.mClient = ClientManager.createClient()

        // Tyrus expects a SSL connection if the SSL_ENGINE_CONFIGURATOR
        // property is present. This leads to a "connection failed" error when
        // a non SSL secured protocol is used. Due to this we only add the
        // property when connecting to a SSL secured node.
        if (SteemJConfig.getInstance().isSslVerificationDisabled && SteemJConfig.getInstance().webSocketEndpointURI.scheme == "wss" || SteemJConfig.getInstance().webSocketEndpointURI.scheme == "https") {
            val sslEngineConfigurator = SslEngineConfigurator(SslContextConfigurator())
            sslEngineConfigurator.hostnameVerifier = HostnameVerifier { _, _ -> true }
            mClient.properties[ClientProperties.SSL_ENGINE_CONFIGURATOR] = sslEngineConfigurator
        }

        mClient.defaultMaxSessionIdleTimeout = maxTimeOut.toLong()
        mClient.properties[ClientProperties.RECONNECT_HANDLER] = MyReconnectHandler()

    }

    override fun onOpen(session: Session?, config: EndpointConfig?) {
        mSession = session
        mSession?.addMessageHandler(this)
    }

    override fun onMessage(message: String?) {
        Timber.i("onMessage message = $message")

        if (message?.contains("\"id\"") == true) {
            val id = mapper.readValue<IdentifiableImpl>(message).id
            val latch = mLatches[id]
            mLatches.remove(id)
            mRawJsonResponse = message
            latch?.countDown()
        } else if (!message.isNullOrEmpty()) {
            try {
                val golosServiceNotification = mapper.readValue<GolosServicesNotification>(message
                        ?: "")
                onServiceNotification?.invoke(golosServiceNotification)
            } catch (e: Exception) {
                e.printStackTrace()
                Timber.e(e)
            }
        }
    }

    @Synchronized
    override fun requestAuth() {
        if (mSession?.isOpen == true) mSession?.close()
        mClient.connectToServer(this, ClientEndpointConfig.Builder.create().build(), URI(gateUrl))

    }

    @Throws(IOException::class, DeploymentException::class, JSONException::class)
    override fun sendMessage(message: GolosServicesRequest, method: String): GolosServicesResponse {
        if (mSession?.isOpen != true) {
            requestAuth()
        }

        val messageToSend = GolosServicesMessagesWrapper(method, message)
        val messageString = mapper.writeValueAsString(messageToSend)

        println("sendMessage $messageString from ${Thread.currentThread().name}")


        mSession?.basicRemote?.sendText(messageString)

        val latch = CountDownLatch(1)
        mLatches[messageToSend.id] = latch
        latch.await()

        if (mRawJsonResponse?.contains("\"error\"") == true) {
            val error = mMapper?.readValue<GolosServicesErrorMessage>(mRawJsonResponse ?: "")
                    ?: throw IllegalArgumentException("cannot convert value $mRawJsonResponse")
            throw GolosServicesException(error.error)

        } else
            return mMapper?.readValue(mRawJsonResponse ?: "")
                    ?: throw IllegalArgumentException("cannot convert value $mRawJsonResponse")
    }

    override fun dropConnection() {
        mSession?.close()

        mLatches.forEach { entry ->
            mRawJsonResponse = "{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":${Int.MIN_VALUE},\"message\":\"Logout\"}}"
            entry.value.countDown() }
    }

    private inner class MyReconnectHandler : ClientManager.ReconnectHandler() {
        override fun onConnectFailure(exception: Exception?): Boolean {
            return true
        }

        override fun onDisconnect(closeReason: CloseReason?): Boolean {
            return false
        }

        override fun getDelay(): Long {
            return 5_000L
        }
    }
}

interface GolosServicesCommunicator {
    @Throws(IOException::class, DeploymentException::class, JSONException::class)
    @WorkerThread
    fun sendMessage(message: GolosServicesRequest, method: String): GolosServicesResponse

    @WorkerThread
    fun requestAuth()

    @WorkerThread
    fun dropConnection()
}

class GolosServicesException(val golosServicesError: GolosServicesError) : Exception(golosServicesError.toString())


internal data class GolosServicesMessagesWrapper(@JsonProperty("method") val method: String,
                                                 @JsonProperty("params") val params: JsonConvertable) : Identifiable {
    fun stringRepresentation(): String = mapper.writeValueAsString(this)

    @JsonProperty("jsonrpc")
    val jsonRpc = "2.0"

    @JsonIgnore()
    private val _id = requestCounter.incrementAndGet()
    @JsonProperty("id")
    override val id = _id
}

private val requestCounter = AtomicLong(0)