package io.golos.golos.notifications

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.golos.golos.utils.JsonConvertable
import io.golos.golos.utils.mapper

interface Identifiable {
    val id: Long
}

abstract class GolosServicesRequest : JsonConvertable {
    fun stringRepresentation(): String = mapper.writeValueAsString(this)
}


data class GolosAuthRequest(
        @JsonProperty("user")
        val user: String,
        @JsonProperty("sign")
        val sign: String) : GolosServicesRequest()

data class GolosPushSubscribeRequest(
        @JsonProperty("key")
        val fcmToken: String,
        @JsonProperty("deviceType")
        val deviceType: String = "android") : GolosServicesRequest()


@JsonIgnoreProperties(ignoreUnknown = true)
open class IdentifiableImpl(@JsonProperty("id") override val id: Long) : Identifiable

data class GolosServicesError(
        @JsonProperty("code")
        val code: Int,
        @JsonProperty("message")
        val message: String)

data class GolosServicesResponse(@JsonProperty("id")
                                 override val id: Long,
                                 @JsonProperty("result")
                                 val result: List<String>) : IdentifiableImpl(id)

data class GolosServicesErrorMessage(
        @JsonProperty("id")
        override val id: Long,
        @JsonProperty("error")
        val error: GolosServicesError,
        @JsonProperty("jsonrpc")
        val jsonRpc: String) : IdentifiableImpl(id)


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "method")
@JsonSubTypes(value = [(JsonSubTypes.Type(value = GolosServerAuthRequest::class, name = "sign"))]
)
sealed class GolosServicesNotification(
        @JsonProperty("method")
        val method: String,
        @JsonProperty("jsonrpc")
        val jsonrpc: String = "2.0")


class GolosServerAuthRequest(
        @JsonProperty("params")
        private val parameters: Array<String>) : GolosServicesNotification(method = "sign") {

    val secret = parameters.firstOrNull()
}


