package io.golos.golos.repository.services

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.convertValue
import io.golos.golos.utils.JsonRpcError
import io.golos.golos.utils.mapper
import io.golos.golos.utils.rpcErrorFromCode

@JsonIgnoreProperties(ignoreUnknown = true)
private class Success(@JsonProperty("status") val status: String)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class EventsData(@JsonProperty("total") val total: Int,
                              @JsonProperty("fresh") val fresh: Int,
                              @JsonProperty("data") val data: List<Event>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GolosEvents(val freshCount: Int, val events: List<GolosEvent>)

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
            REPOST -> "reblog"
            REWARD -> "reward"
            CURATOR_AWARD -> "curatorReward"
            MESSAGE -> "message"
            WITNESS_VOTE -> "witnessVote"
            WITNESS_CANCEL_VOTE -> "witnessCancelVote"
        }
    }
}

fun GolosServicesResponse.isAuthSuccessMessage(): Boolean {
    return try {
        val succes = mapper.convertValue<Success>(result)
        succes.status.equals("ok", true)
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


fun GolosServicesResponse.getSecret(): String {
    return try {
        mapper.convertValue<GolosServerAuthRequest.Secret>(result).secret

    } catch (e: Exception) {
        e.printStackTrace()
        ""
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


fun GolosServicesResponse.getUnreadCount(): Int {
    return try {
        mapper.convertValue<FreshResult>(result).fresh
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
class FreshResult(@JsonProperty("fresh") val fresh: Int)


fun GolosServicesResponse.getEventData(): GolosEvents {
    return try {
        mapper.convertValue<EventsData>(result).let {
            GolosEvents(it.fresh,
                    it.data.map { GolosEvent.fromEvent(it) })
        }
    } catch (e: Exception) {
        e.printStackTrace()
        GolosEvents(0, emptyList())
    }
}

fun GolosServicesErrorMessage.getErrorType(): JsonRpcError? {
    return rpcErrorFromCode(this.error.code)
}

fun GolosServicesException.getErrorType(): JsonRpcError? = rpcErrorFromCode(this.golosServicesError.code)

