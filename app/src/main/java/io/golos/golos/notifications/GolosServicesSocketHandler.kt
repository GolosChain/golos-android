package io.golos.golos.notifications

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import eu.bittrade.libs.golosj.communication.CommunicationHandler.getObjectMapper
import eu.bittrade.libs.golosj.configuration.SteemJConfig
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
import javax.net.ssl.HostnameVerifier
import javax.websocket.*

class GolosServicesSocketHandler(private val gateUrl: String,
                                 maxTimeOut: Int = 45_000,
                                 @Volatile
                                 var onServiceNotification: ((GolosServicesNotification) -> Unit)? = null) : Endpoint(), MessageHandler.Whole<String>, GolosServicesCommunicator {
    private var mMapper: ObjectMapper? = getObjectMapper()

    private val mClient: ClientManager
    private var mSession: Session? = null
    private var mRawJsonResponse: String? = null
    private val mLatches = Collections.synchronizedMap<Long, CountDownLatch>(mapOf())
    private val mGlobalLock = Any()

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
        Timber.e("onMessage $message")

        if (message?.contains("\"id\"") == true) {
            val id = mapper.readValue<IdentifiableImpl>(message).id
            val latch = mLatches[id]
            mLatches.remove(id)
            latch?.countDown()
        } else if (!message.isNullOrEmpty()) {
            try {
                val golosServiceNotification = mapper.readValue<GolosServicesNotification>(message
                        ?: "")
                onServiceNotification?.invoke(golosServiceNotification)
            } catch (e: JsonParseException) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    override fun requestAuth() {
        if (mSession?.isOpen != true) mClient.connectToServer(this, ClientEndpointConfig.Builder.create().build(), URI(gateUrl))

    }

    @Throws(IOException::class, DeploymentException::class, JSONException::class)
    override fun sendMessage(message: GolosServicesForwardMessage): GolosServicesInputMessage {
        if (mSession?.isOpen != true) {
            synchronized(mGlobalLock, {
                if (mSession?.isOpen != true) {
                    mClient.connectToServer(this, ClientEndpointConfig.Builder.create().build(), URI(gateUrl))
                }
            })
        }

        Timber.e("sendMessage ${message.stringRepresentation()}")
        mSession?.basicRemote?.sendText(message.stringRepresentation())

        val latch = CountDownLatch(1)
        mLatches[message.id] = latch
        latch.await()

        return mMapper?.convertValue(mRawJsonResponse, GolosServicesInputMessage::class.java)
                ?: throw JSONException("cannot convert value $mRawJsonResponse")

    }


    private inner class MyReconnectHandler : ClientManager.ReconnectHandler() {
        override fun onConnectFailure(exception: Exception?): Boolean {
            return true
        }

        override fun onDisconnect(closeReason: CloseReason?): Boolean {
            return true
        }

        override fun getDelay(): Long {
            return 1_000L
        }
    }
}

interface GolosServicesCommunicator {
    @Throws(IOException::class, DeploymentException::class, JSONException::class)
    fun sendMessage(message: GolosServicesForwardMessage): GolosServicesInputMessage

    fun requestAuth()
}


