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
    JsonSubTypes.Type(value = AwardEvent::class, name = "award"),
    JsonSubTypes.Type(value = CuratorAwardEvent::class, name = "curatorAward"),
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

data class AwardEvent(@JsonProperty("permlink") val permlink: String, @JsonProperty("award") val award: Award)
    : Event(EventType.AWARD.toString())

data class CuratorAwardEvent(@JsonProperty("permlink") val permlink: String, @JsonProperty("award") val award: Award)
    : Event(EventType.CURATOR_AWARD.toString())

class MessageEvent : Event(EventType.MESSAGE.toString())

class WitnessVoteEvent : Event(EventType.WITNESS_VOTE.toString())

class WitnessCancelVoteEvent : Event(EventType.WITNESS_CANCEL_VOTE.toString())


private fun String.makeLinkFromPermlink(): PostLinkExtractedData? {
    return PostLinkExtractedData(Repository.get.appUserData.value?.userName
            ?: return null, null, this)
}

sealed class GolosEvent(val id: String, val creationTime: Long, val counter: Int) : Comparable<GolosEvent> {

    var avatarPath: String? = null

    override fun compareTo(other: GolosEvent) = -Longs.compare(this.creationTime, other.creationTime)

    override fun toString(): String {
        return "GolosEvent(id='$id', creationTime=$creationTime, counter=$counter, avatarPath=$avatarPath)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosEvent) return false

        if (id != other.id) return false
        if (creationTime != other.creationTime) return false
        if (counter != other.counter) return false
        if (avatarPath != other.avatarPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + creationTime.hashCode()
        result = 31 * result + counter
        result = 31 * result + (avatarPath?.hashCode() ?: 0)
        return result
    }


    companion object {
        fun fromEvent(event: Event): GolosEvent {
            return when (event) {
                is VoteEvent -> GolosVoteEvent(event._id, event.createdAt, event.counter, event.permlink, event.fromUsers)
                is FlagEvent -> GolosFlagEvent(event._id, event.createdAt, event.counter, event.permlink, event.fromUsers)
                is TransferEvent -> GolosTransferEvent(event._id, event.createdAt, event.counter, event.amount, event.fromUsers)
                is SubscribeEvent -> GolosSubscribeEvent(event._id, event.createdAt, event.counter, event.fromUsers)
                is UnSubscribeEvent -> GolosUnSubscribeEvent(event._id, event.createdAt, event.counter, event.fromUsers)
                is ReplyEvent -> GolosReplyEvent(event._id, event.createdAt, event.counter, event.permlink, event.parentPermlink, event.fromUsers)
                is RepostEvent -> GolosRepostEvent(event._id, event.createdAt, event.counter, event.permlink, event.fromUsers)
                is MentionEvent -> GolosMentionEvent(event._id, event.createdAt, event.counter, event.permlink, event.parentPermlink, event.fromUsers)
                is AwardEvent -> GolosAwardEvent(event._id, event.createdAt, event.counter, event.permlink, event.award)
                is CuratorAwardEvent -> GolosCuratorAwardEvent(event._id, event.createdAt, event.counter, event.permlink, event.award)
                is MessageEvent -> GolosMessageEvent(event._id, event.createdAt, event.counter, event.fromUsers)
                is WitnessVoteEvent -> GolosWitnessVoteEvent(event._id, event.createdAt, event.counter, event.fromUsers)
                is WitnessCancelVoteEvent -> GolosWitnessCancelVoteEvent(event._id, event.createdAt, event.counter, event.fromUsers)
            }
        }
    }
}


class GolosVoteEvent(id: String,
                     creationTime: Long,
                     counter: Int,
                     val permlink: String,
                     val fromUsers: List<String>) : GolosEvent(id, creationTime, counter), PostLinkable, Authorable {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun getAuthors() = fromUsers
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosVoteEvent) return false
        if (!super.equals(other)) return false

        if (permlink != other.permlink) return false
        if (fromUsers != other.fromUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + fromUsers.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosVoteEvent(permlink='$permlink', fromUsers=$fromUsers)"
    }

}

class GolosFlagEvent(id: String, creationTime: Long, counter: Int,
                     val permlink: String, val fromUsers: List<String>) : PostLinkable, GolosEvent(id, creationTime, counter), Authorable {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun getAuthors() = fromUsers
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosFlagEvent) return false
        if (!super.equals(other)) return false

        if (permlink != other.permlink) return false
        if (fromUsers != other.fromUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + fromUsers.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosFlagEvent(permlink='$permlink', fromUsers=$fromUsers)"
    }

}

class GolosTransferEvent(id: String, creationTime: Long, counter: Int,
                         val amount: String, val fromUsers: List<String>) : GolosEvent(id, creationTime, counter), Authorable {
    override fun getAuthors() = fromUsers
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosTransferEvent) return false
        if (!super.equals(other)) return false

        if (amount != other.amount) return false
        if (fromUsers != other.fromUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + fromUsers.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosTransferEvent(amount='$amount', fromUsers=$fromUsers)"
    }

}

class GolosSubscribeEvent(id: String, creationTime: Long, counter: Int,
                          val fromUsers: List<String>) : GolosEvent(id, creationTime, counter), Authorable {
    override fun getAuthors(): List<String> = fromUsers
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosSubscribeEvent) return false
        if (!super.equals(other)) return false

        if (fromUsers != other.fromUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUsers.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosSubscribeEvent(fromUsers=$fromUsers)"
    }

}

class GolosUnSubscribeEvent(id: String, creationTime: Long, counter: Int,
                            val fromUsers: List<String>) : GolosEvent(id, creationTime, counter), Authorable {
    override fun getAuthors(): List<String> = fromUsers
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosUnSubscribeEvent) return false
        if (!super.equals(other)) return false

        if (fromUsers != other.fromUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUsers.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosUnSubscribeEvent(fromUsers=$fromUsers)"
    }

}

class GolosReplyEvent(id: String, creationTime: Long, counter: Int,
                      val permlink: String,
                      val parentPermlink: String,
                      val fromUsers: List<String>) : PostLinkable, GolosEvent(id, creationTime, counter), Authorable {

    override fun getLink() = PostLinkExtractedData(fromUsers.first(), null, permlink)
    override fun getAuthors() = fromUsers
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosReplyEvent) return false
        if (!super.equals(other)) return false

        if (permlink != other.permlink) return false
        if (parentPermlink != other.parentPermlink) return false
        if (fromUsers != other.fromUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + parentPermlink.hashCode()
        result = 31 * result + fromUsers.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosReplyEvent(permlink='$permlink', parentPermlink='$parentPermlink', fromUsers=$fromUsers)"
    }

}

class GolosMentionEvent(id: String, creationTime: Long, counter: Int,
                        val permlink: String, val parentPermlink: String,
                        val fromUsers: List<String>) : PostLinkable, GolosEvent(id, creationTime, counter), Authorable {
    override fun getLink() = PostLinkExtractedData(fromUsers.first(), null, permlink)
    override fun getAuthors() = fromUsers
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosMentionEvent) return false
        if (!super.equals(other)) return false

        if (permlink != other.permlink) return false
        if (parentPermlink != other.parentPermlink) return false
        if (fromUsers != other.fromUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + parentPermlink.hashCode()
        result = 31 * result + fromUsers.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosMentionEvent(permlink='$permlink', parentPermlink='$parentPermlink', fromUsers=$fromUsers)"
    }

}

class GolosRepostEvent(id: String, creationTime: Long, counter: Int,
                       val permlink: String,
                       val fromUsers: List<String>) : PostLinkable, GolosEvent(id, creationTime, counter), Authorable {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun getAuthors() = fromUsers
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosRepostEvent) return false
        if (!super.equals(other)) return false

        if (permlink != other.permlink) return false
        if (fromUsers != other.fromUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + fromUsers.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosRepostEvent(permlink='$permlink', fromUsers=$fromUsers)"
    }

}

class GolosAwardEvent(id: String, creationTime: Long, counter: Int,
                      val permlink: String,
                      val award: Award) : PostLinkable, GolosEvent(id, creationTime, counter) {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosAwardEvent) return false
        if (!super.equals(other)) return false

        if (permlink != other.permlink) return false
        if (award != other.award) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + award.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosAwardEvent(permlink='$permlink', award=$award)"
    }

}

class GolosCuratorAwardEvent(id: String, creationTime: Long, counter: Int,
                             val permlink: String,
                             val award: Award) : PostLinkable, GolosEvent(id, creationTime, counter) {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosCuratorAwardEvent) return false
        if (!super.equals(other)) return false

        if (permlink != other.permlink) return false
        if (award != other.award) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + award.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosCuratorAwardEvent(permlink='$permlink', award=$award)"
    }

}

class GolosMessageEvent(id: String, creationTime: Long, counter: Int,
                        val fromUsers: List<String>) : GolosEvent(id, creationTime, counter), Authorable {
    override fun getAuthors(): List<String> = fromUsers
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosMessageEvent) return false
        if (!super.equals(other)) return false

        if (fromUsers != other.fromUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUsers.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosMessageEvent(fromUsers=$fromUsers)"
    }

}

class GolosWitnessVoteEvent(id: String, creationTime: Long, counter: Int,
                            val fromUsers: List<String>) : GolosEvent(id, creationTime, counter), Authorable {
    override fun getAuthors(): List<String> = fromUsers
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosWitnessVoteEvent) return false
        if (!super.equals(other)) return false

        if (fromUsers != other.fromUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUsers.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosWitnessVoteEvent(fromUsers=$fromUsers)"
    }

}

class GolosWitnessCancelVoteEvent(id: String, creationTime: Long, counter: Int,
                                  val fromUsers: List<String>) : GolosEvent(id, creationTime, counter), Authorable {
    override fun getAuthors(): List<String> = fromUsers
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosWitnessCancelVoteEvent) return false
        if (!super.equals(other)) return false

        if (fromUsers != other.fromUsers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUsers.hashCode()
        return result
    }

    override fun toString(): String {
        return "GolosWitnessCancelVoteEvent(fromUsers=$fromUsers)"
    }

}


interface Authorable {
    fun getAuthors(): List<String>
}






