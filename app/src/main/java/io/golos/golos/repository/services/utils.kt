package io.golos.golos.repository.services

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.convertValue
import io.golos.golos.utils.JsonRpcError
import io.golos.golos.utils.mapper
import io.golos.golos.utils.rpcErrorFromCode

private class Success(@JsonProperty("status") val status: String)


@JsonIgnoreProperties(ignoreUnknown = true)
private class EventData(@JsonProperty("data") val data: List<Event>)

enum class EventType {
    VOTE, FLAG, TRANSFER, REPLY, SUBSCRIBE, UNSUBSCRIBE, MENTION, REPOST, REWARD, CURATOR_AWARD, MESSAGE,
    WITNESS_VOTE, WITNESS_CANCEL_VOTE;

    override fun toString(): String {
        return when (this) {
            VOTE -> "vote"
            FLAG -> "flag"
            TRANSFER -> "transfer"
            REPLY -> "reply"
            SUBSCRIBE -> "subscribe"
            UNSUBSCRIBE -> "unsubscribe"
            MENTION -> "mention"
            REPOST -> "repost"
            REWARD -> "reward"
            CURATOR_AWARD -> "curatorReward"
            MESSAGE -> "message"
            WITNESS_VOTE -> "witnessVote"
            WITNESS_CANCEL_VOTE -> "witnessCancelVote"
        }
    }
}


fun GolosServicesResponse.isAuthSuccessMessage(): Boolean {
    try {
        val succes = mapper.convertValue<Success>(result)
        return succes.status.equals("ok", true)
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

fun GolosServicesResponse.isPushSubscribeSuccesMessage(): Boolean {
    try {
        val succes = mapper.convertValue<Success>(result)
        return succes.status.equals("ok", true)
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

fun GolosServicesResponse.getEventData(): List<Event> {
    try {
        val eventData = mapper.convertValue<EventData>(result)
        return eventData.data
    } catch (e: Exception) {
        e.printStackTrace()
        return emptyList()
    }
}

fun GolosServicesErrorMessage.getErrorType(): JsonRpcError? {
    return rpcErrorFromCode(this.error.code)
}

fun GolosServicesException.getErrorType(): JsonRpcError? = rpcErrorFromCode(this.golosServicesError.code)

