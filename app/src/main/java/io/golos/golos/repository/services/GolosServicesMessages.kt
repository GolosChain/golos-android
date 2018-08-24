package io.golos.golos.repository.services

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
        @JsonProperty("profile")
        val profile: String,
        @JsonProperty("deviceType")
        val deviceType: String = "android") : GolosServicesRequest()

data class GolosEventRequest(
        @JsonProperty("fromId")
        val fromId: String? = null,
        @JsonProperty("limit")
        //from 1 to 100
        val limit: Int = 40,
        @JsonProperty("types")
        val types: String = "all") : GolosServicesRequest()


@JsonIgnoreProperties(ignoreUnknown = true)
open class IdentifiableImpl(@JsonProperty("id") override val id: Long) : Identifiable

@JsonIgnoreProperties(ignoreUnknown = true)
data class GolosServicesError(
        @JsonProperty("code")
        val code: Int,
        @JsonProperty("message")
        val message: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GolosServicesResponse(@JsonProperty("id")
                                 override val id: Long,
                                 @JsonProperty("result")
                                 val result: Any) : IdentifiableImpl(id)

@JsonIgnoreProperties(ignoreUnknown = true)
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
        private val params: Secret) : GolosServicesNotification(method = "sign") {

    val secret = params.secret

    data class Secret(@JsonProperty("secret") val secret: String)
}


