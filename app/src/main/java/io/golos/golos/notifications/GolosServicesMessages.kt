package io.golos.golos.notifications

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.golos.golos.utils.mapper
import java.util.concurrent.atomic.AtomicLong

interface Identifiable {
    val id: Long
}

internal val requestCounter = AtomicLong(0)

interface GolosServicesForwardMessage : Identifiable {
    fun stringRepresentation(): String = mapper.writeValueAsString(this)

}

data class GolosAuthResponse(val user: String, val sign: String) : GolosServicesForwardMessage {
    private val _id = requestCounter.incrementAndGet()
    override val id = _id
}


@JsonIgnoreProperties(ignoreUnknown = true)
class IdentifiableImpl(override val id: Long) : Identifiable

sealed class GolosServicesInputMessage


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(value = [(JsonSubTypes.Type(value = GolosAuthRequest::class, name = "auth"))]
)
sealed class GolosServicesNotification(val type: String)


class GolosAuthRequest(parameters: Array<String>) : GolosServicesNotification(type = "auth") {
    val secret = parameters.firstOrNull()
}


