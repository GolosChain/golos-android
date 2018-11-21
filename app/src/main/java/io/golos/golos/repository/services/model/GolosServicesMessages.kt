package io.golos.golos.repository.services.model

import com.fasterxml.jackson.annotation.*
import io.golos.golos.utils.JsonConvertable
import io.golos.golos.utils.mapper

interface Identifiable {
    val id: Long
}

abstract class GolosServicesRequest : JsonConvertable {
    fun stringRepresentation(): String = mapper.writeValueAsString(this)
}


class GolosAuthRequest(
        @JsonProperty("user")
        val user: String,
        @JsonProperty("sign")
        val sign: String) : GolosServicesRequest()

class GolosPushSubscribeRequest(
        @JsonProperty("key")//fcm token
        val key: String,
        @JsonProperty("profile")//device id
        val profile: String) : GolosServicesRequest()

class GolosPushUnSubscribeRequest(
        @JsonProperty("key")//fcm token
        val key: String,
        @JsonProperty("profile")//device id
        val profile: String) : GolosServicesRequest()

@JsonInclude(JsonInclude.Include.NON_NULL)
class GolosEventRequest(
        @JsonProperty("fromId")
        val fromId: String? = null,
        @JsonProperty("limit")
        //from 1 to 100
        val limit: Int ,
        @JsonProperty("types")
        val types: List<String>,
        @JsonProperty("markAsViewed")
        val markAsViewed: Boolean?) : GolosServicesRequest()

@JsonInclude(JsonInclude.Include.NON_NULL)
class GolosAllEventRequest(
        @JsonProperty("fromId")
        val fromId: String? = null,
        @JsonProperty("limit")
        //from 1 to 100
        val limit: Int,
        @JsonProperty("markAsViewed")
        val markAsViewed: Boolean?,
        @JsonProperty("types")
        val types: String = "all"
) : GolosServicesRequest()

class MarkAsReadRequest(
        @JsonProperty("ids")
        val ids: List<String>)
    : GolosServicesRequest()

class GetUnreadCountRequest
    : GolosServicesRequest()

class MarkAllReadRequest
    : GolosServicesRequest()

class GetSecretRequest
    : GolosServicesRequest()

@JsonIgnoreProperties(ignoreUnknown = true)
open class IdentifiableImpl(@JsonProperty("id") override val id: Long) : Identifiable

@JsonIgnoreProperties(ignoreUnknown = true)
class GolosServicesError(
        @JsonProperty("code")
        val code: Int,
        @JsonProperty("message")
        val message: String)

@JsonIgnoreProperties(ignoreUnknown = true)
class GolosServicesResponse(@JsonProperty("id")
                            override val id: Long,
                            @JsonProperty("result")
                            val result: Any) : IdentifiableImpl(id)

@JsonIgnoreProperties(ignoreUnknown = true)
class GolosServicesErrorMessage(
        @JsonProperty("id")
        override val id: Long,
        @JsonProperty("error")
        val error: GolosServicesError,
        @JsonProperty("jsonrpc")
        val jsonRpc: String) : IdentifiableImpl(id)


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "method")
@JsonSubTypes(value = [(JsonSubTypes.Type(value = GolosServerAuthRequest::class, name = "sign"))]
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class GolosServicesNotification(
        @JsonProperty("method")
        val method: String,
        @JsonProperty("jsonrpc")
        val jsonrpc: String = "2.0")


class GolosServerAuthRequest(
        @JsonProperty("params")
        private val params: Secret) : GolosServicesNotification(method = "sign") {

    val secret = params.secret

    class Secret(@JsonProperty("secret") val secret: String)
}


