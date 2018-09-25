package io.golos.golos.repository.services

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.common.primitives.Longs
import io.golos.golos.notifications.PostLinkExtractedData
import io.golos.golos.notifications.PostLinkable
import io.golos.golos.repository.Repository
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class CustomJsonDateDeserializer : JsonDeserializer<Long>() {

    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jsonParser: JsonParser,
                             deserializationContext: DeserializationContext): Long {


        val date = jsonParser.text ?: ""
        return try {
            format.parse(date).time
        } catch (e: ParseException) {
            0L
        }
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes(value = [JsonSubTypes.Type(value = VoteEvent::class, name = "vote"),
    JsonSubTypes.Type(value = FlagEvent::class, name = "flag"),
    JsonSubTypes.Type(value = TransferEvent::class, name = "transfer"),
    JsonSubTypes.Type(value = ReplyEvent::class, name = "reply"),
    JsonSubTypes.Type(value = SubscribeEvent::class, name = "subscribe"),
    JsonSubTypes.Type(value = UnSubscribeEvent::class, name = "unsubscribe"),
    JsonSubTypes.Type(value = MentionEvent::class, name = "mention"),
    JsonSubTypes.Type(value = RepostEvent::class, name = "repost"),
    JsonSubTypes.Type(value = AwardEvent::class, name = "reward"),
    JsonSubTypes.Type(value = CuratorAwardEvent::class, name = "curatorReward"),
    JsonSubTypes.Type(value = MessageEvent::class, name = "message"),
    JsonSubTypes.Type(value = WitnessVoteEvent::class, name = "witnessVote"),
    JsonSubTypes.Type(value = WitnessCancelVoteEvent::class, name = "witnessCancelVote")])


sealed class Event(
        @JsonProperty("eventType")
        private val eventType: String) {
    @JsonProperty("counter")
    var counter: Int = 1

    @JsonProperty("_id")
    var _id: String = ""
    @JsonProperty("fresh")
    var fresh: Boolean = true

    @JsonProperty("fromUsers")
    var fromUsers: List<String> = listOf()

    @JsonProperty("createdAt")
    @JsonDeserialize(using = CustomJsonDateDeserializer::class)
    var createdAt: Long = 0L

    @JsonProperty("updatedAt")
    @JsonDeserialize(using = CustomJsonDateDeserializer::class)
    var updatedAt: Long = 0L

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Event) return false

        if (eventType != other.eventType) return false
        if (counter != other.counter) return false
        if (_id != other._id) return false
        if (fresh != other.fresh) return false
        if (fromUsers != other.fromUsers) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eventType.hashCode()
        result = 31 * result + counter
        result = 31 * result + _id.hashCode()
        result = 31 * result + fresh.hashCode()
        result = 31 * result + fromUsers.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }


}

data class Award(@JsonProperty("golos") val golos: Double,
                 @JsonProperty("golosPower") val golosPower: Double,
                 @JsonProperty("gbg") val gbg: Double)

data class VoteEvent(@JsonProperty("permlink") val permlink: String) : Event(EventType.VOTE.toString())

data class FlagEvent(@JsonProperty("permlink") val permlink: String) : Event(EventType.FLAG.toString())

data class TransferEvent(@JsonProperty("amount") val amount: String) : Event(EventType.TRANSFER.toString())

class SubscribeEvent : Event(EventType.SUBSCRIBE.toString())

class UnSubscribeEvent : Event(EventType.UNSUBSCRIBE.toString())

data class ReplyEvent(@JsonProperty("permlink") val permlink: String,
                      @JsonProperty("parentPermlink") val parentPermlink: String) : Event(EventType.REPLY.toString())

data class MentionEvent(@JsonProperty("permlink") val permlink: String,
                        @JsonProperty("parentPermlink") val parentPermlink: String) : Event(EventType.MENTION.toString())

data class RepostEvent(@JsonProperty("permlink") val permlink: String) : Event(EventType.REPOST.toString())

data class AwardEvent(@JsonProperty("reward") val reward: Award, @JsonProperty("permlink") val permlink: String)
    : Event(EventType.REWARD.toString())

data class CuratorAwardEvent(@JsonProperty("permlink") val permlink: String,
                             @JsonProperty("curatorTargetAuthor") val curatorTargetAuthor: String,
                             @JsonProperty("curatorReward") val curatorReward: Double)
    : Event(EventType.CURATOR_AWARD.toString())

class MessageEvent : Event(EventType.MESSAGE.toString())

class WitnessVoteEvent : Event(EventType.WITNESS_VOTE.toString())

class WitnessCancelVoteEvent : Event(EventType.WITNESS_CANCEL_VOTE.toString())


private fun String.makeLinkFromPermlink(): PostLinkExtractedData? {
    return PostLinkExtractedData(Repository.get.appUserData.value?.name
            ?: return null, null, this)
}

sealed class GolosEvent(val id: String, val creationTime: Long, val counter: Int, val fresh: Boolean) : Comparable<GolosEvent> {

    override fun compareTo(other: GolosEvent) = -Longs.compare(this.creationTime, other.creationTime)

    override fun toString(): String {
        return "GolosEvent(id='$id', creationTime=$creationTime, counter=$counter)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GolosEvent

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


    companion object {
        fun fromEvent(event: Event): GolosEvent {
            return when (event) {
                is VoteEvent -> GolosVoteEvent(event._id, event.createdAt, event.counter, event.permlink, event.fromUsers, event.fresh)
                is FlagEvent -> GolosFlagEvent(event._id, event.createdAt, event.counter, event.permlink, event.fromUsers, event.fresh)
                is TransferEvent -> GolosTransferEvent(event._id, event.createdAt, event.counter, event.amount, event.fromUsers, event.fresh)
                is SubscribeEvent -> GolosSubscribeEvent(event._id, event.createdAt, event.counter, event.fromUsers, event.fresh)
                is UnSubscribeEvent -> GolosUnSubscribeEvent(event._id, event.createdAt, event.counter, event.fromUsers, event.fresh)
                is ReplyEvent -> GolosReplyEvent(event._id, event.createdAt, event.counter, event.permlink, event.parentPermlink, event.fromUsers, event.fresh)
                is RepostEvent -> GolosRepostEvent(event._id, event.createdAt, event.counter, event.permlink, event.fromUsers, event.fresh)
                is MentionEvent -> GolosMentionEvent(event._id, event.createdAt, event.counter, event.permlink, event.parentPermlink, event.fromUsers, event.fresh)
                is AwardEvent -> GolosAwardEvent(event._id, event.createdAt, event.counter, event.reward, event.permlink, event.fresh)
                is CuratorAwardEvent -> GolosCuratorAwardEvent(event._id, event.createdAt, event.counter, event.permlink, event.curatorTargetAuthor, event.curatorReward, event.fresh)
                is MessageEvent -> GolosMessageEvent(event._id, event.createdAt, event.counter, event.fromUsers, event.fresh)
                is WitnessVoteEvent -> GolosWitnessVoteEvent(event._id, event.createdAt, event.counter, event.fromUsers, event.fresh)
                is WitnessCancelVoteEvent -> GolosWitnessCancelVoteEvent(event._id, event.createdAt, event.counter, event.fromUsers, event.fresh)
            }
        }
    }
}


class GolosVoteEvent(id: String,
                     creationTime: Long,
                     counter: Int,
                     val permlink: String,
                     val fromUsers: List<String>, fresh: Boolean) : GolosEvent(id, creationTime, counter, fresh), PostLinkable, Authorable {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun getAuthors() = fromUsers

    override fun toString(): String {
        return "GolosVoteEvent(permlink='$permlink', fromUsers=$fromUsers)"
    }

}

class GolosFlagEvent(id: String, creationTime: Long, counter: Int,
                     val permlink: String, val fromUsers: List<String>, fresh: Boolean) : PostLinkable, GolosEvent(id, creationTime, counter, fresh), Authorable {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun getAuthors() = fromUsers


    override fun toString(): String {
        return "GolosFlagEvent(permlink='$permlink', fromUsers=$fromUsers)"
    }

}

class GolosTransferEvent(id: String, creationTime: Long, counter: Int,
                         val amount: String, val fromUsers: List<String>, fresh: Boolean) : GolosEvent(id, creationTime, counter, fresh), Authorable {
    override fun getAuthors() = fromUsers


    override fun toString(): String {
        return "GolosTransferEvent(amount='$amount', fromUsers=$fromUsers)"
    }

}

class GolosSubscribeEvent(id: String, creationTime: Long, counter: Int,
                          val fromUsers: List<String>, fresh: Boolean) : GolosEvent(id, creationTime, counter, fresh), Authorable {
    override fun getAuthors(): List<String> = fromUsers


    override fun toString(): String {
        return "GolosSubscribeEvent(fromUsers=$fromUsers id = $id)"
    }

}

class GolosUnSubscribeEvent(id: String, creationTime: Long, counter: Int,
                            val fromUsers: List<String>, fresh: Boolean) : GolosEvent(id, creationTime, counter, fresh), Authorable {
    override fun getAuthors(): List<String> = fromUsers


    override fun toString(): String {
        return "GolosUnSubscribeEvent(fromUsers=$fromUsers id = $id)"
    }

}

class GolosReplyEvent(id: String, creationTime: Long, counter: Int,
                      val permlink: String,
                      val parentPermlink: String,
                      val fromUsers: List<String>, fresh: Boolean) : PostLinkable, GolosEvent(id, creationTime, counter, fresh), Authorable {

    override fun getLink() = PostLinkExtractedData(fromUsers.first(), null, permlink)
    override fun getAuthors() = fromUsers


    override fun toString(): String {
        return "GolosReplyEvent(permlink='$permlink', parentPermlink='$parentPermlink', fromUsers=$fromUsers)"
    }

}

class GolosMentionEvent(id: String, creationTime: Long, counter: Int,
                        val permlink: String, val parentPermlink: String,
                        val fromUsers: List<String>, fresh: Boolean) : PostLinkable, GolosEvent(id, creationTime, counter, fresh), Authorable {
    override fun getLink() = PostLinkExtractedData(fromUsers.first(), null, permlink)
    override fun getAuthors() = fromUsers


    override fun toString(): String {
        return "GolosMentionEvent(permlink='$permlink', parentPermlink='$parentPermlink', fromUsers=$fromUsers)"
    }

}

class GolosRepostEvent(id: String, creationTime: Long, counter: Int,
                       val permlink: String,
                       val fromUsers: List<String>, fresh: Boolean) : PostLinkable, GolosEvent(id, creationTime, counter, fresh), Authorable {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun getAuthors() = fromUsers


    override fun toString(): String {
        return "GolosRepostEvent(permlink='$permlink', fromUsers=$fromUsers)"
    }

}

class GolosAwardEvent(id: String, creationTime: Long, counter: Int,
                      val award: Award, val permlink: String, fresh: Boolean) : GolosEvent(id, creationTime, counter, fresh) {


    override fun toString(): String {
        return "GolosAwardEvent(awardInVShares=$award, permlink='$permlink')"
    }
}

class GolosCuratorAwardEvent(id: String, creationTime: Long, counter: Int,
                             val permlink: String,
                             val author: String,
                             val awardInVShares: Double, fresh: Boolean) : PostLinkable, GolosEvent(id, creationTime, counter, fresh) {
    override fun getLink() = PostLinkExtractedData(author, null, permlink)


    override fun toString(): String {
        return "GolosCuratorAwardEvent(permlink='$permlink', awardInVShares=$awardInVShares)"
    }

}

class GolosMessageEvent(id: String, creationTime: Long, counter: Int,
                        val fromUsers: List<String>, fresh: Boolean) : GolosEvent(id, creationTime, counter, fresh), Authorable {
    override fun getAuthors(): List<String> = fromUsers


    override fun toString(): String {
        return "GolosMessageEvent(fromUsers=$fromUsers)"
    }

}

class GolosWitnessVoteEvent(id: String, creationTime: Long, counter: Int,
                            val fromUsers: List<String>, fresh: Boolean) : GolosEvent(id, creationTime, counter, fresh), Authorable {
    override fun getAuthors(): List<String> = fromUsers


    override fun toString(): String {
        return "GolosWitnessVoteEvent(fromUsers=$fromUsers)"
    }

}

class GolosWitnessCancelVoteEvent(id: String, creationTime: Long, counter: Int,
                                  val fromUsers: List<String>, fresh: Boolean) : GolosEvent(id, creationTime, counter, fresh), Authorable {
    override fun getAuthors(): List<String> = fromUsers


    override fun toString(): String {
        return "GolosWitnessCancelVoteEvent(fromUsers=$fromUsers)"
    }

}


interface Authorable {
    fun getAuthors(): List<String>
}







