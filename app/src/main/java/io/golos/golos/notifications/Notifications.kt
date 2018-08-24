package io.golos.golos.notifications

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes(value = [(JsonSubTypes.Type(value = ReplyNotification::class, name = "reply")),
    JsonSubTypes.Type(value = VoteNotification::class, name = "vote"),
    JsonSubTypes.Type(value = TransferNotification::class, name = "transfer"),
    JsonSubTypes.Type(value = FlagNotification::class, name = "flag"),
    JsonSubTypes.Type(value = SubscribeNotification::class, name = "subscribe"),
    JsonSubTypes.Type(value = UnSubscribeNotification::class, name = "unsubscribe"),
    JsonSubTypes.Type(value = MentionNotification::class, name = "mention"),
    JsonSubTypes.Type(value = RepostNotification::class, name = "repost"),
    JsonSubTypes.Type(value = WitnessVoteNotification::class, name = "witnessVote"),
    JsonSubTypes.Type(value = WitnessCancelVoteNotification::class, name = "witnessCancelVote")])


sealed class Notification(@JsonProperty(value = "eventType") val eventType: String,
                          @JsonProperty(value = "counter") val counter: Int = 1)

data class VoteNotification(@JsonProperty("permlink") val permlink: String,
                            @JsonProperty("voter") val voter: String) : Notification("vote")

data class FlagNotification(@JsonProperty("permlink") val permlink: String,
                            @JsonProperty("voter") val voter: String) : Notification("flag")

data class TransferNotification(@JsonProperty("from") val from: String,
                                @JsonProperty("amount") val amount: String) : Notification("transfer")

data class ReplyNotification(@JsonProperty("permlink") val permlink: String,
                             @JsonProperty("author") val author: String) : Notification("reply")

data class SubscribeNotification(@JsonProperty("follower") val follower: String) : Notification("subscribe")

data class UnSubscribeNotification(@JsonProperty("follower") val follower: String) : Notification("unsubscribe")

data class MentionNotification(@JsonProperty("permlink") val permlink: String,
                               @JsonProperty("author") val author: String) : Notification("mention")

data class RepostNotification(@JsonProperty("permlink") val permlink: String,
                              @JsonProperty("reposter") val reposter: String) : Notification("repost")

data class WitnessVoteNotification(@JsonProperty("from") val from: String) : Notification("witnessVote")

data class WitnessCancelVoteNotification(@JsonProperty("from") val from: String) : Notification("witnessCancelVote")
