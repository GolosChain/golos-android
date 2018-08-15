package io.golos.golos.notifications

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes(value = [(JsonSubTypes.Type(value = ReplyNotificationNew::class, name = "reply")),
    JsonSubTypes.Type(value = VoteNotificationNew::class, name = "vote"),
    JsonSubTypes.Type(value = TransferNotificationNew::class, name = "transfer"),
    JsonSubTypes.Type(value = FlagNotificationNew::class, name = "flag"),
    JsonSubTypes.Type(value = SubscribeNotificationNew::class, name = "subscribe"),
    JsonSubTypes.Type(value = UnSubscribeNotificationNew::class, name = "unsubscribe"),
    JsonSubTypes.Type(value = MentionNotificationNew::class, name = "mention"),
    JsonSubTypes.Type(value = RepostNotificationNew::class, name = "repost"),
    JsonSubTypes.Type(value = WitnessVoteNotificationNew::class, name = "witnessVote"),
    JsonSubTypes.Type(value = WitnessCancelVoteNotificationNew::class, name = "witnessCancelVote")])


sealed class NotificationNew(@JsonProperty(value = "eventType") val eventType: String,
                             @JsonProperty(value = "counter") val counter: Int = 0)

data class VoteNotificationNew(@JsonProperty("permlink") val permlink: String,
                               @JsonProperty("voter") val voter: String) : NotificationNew("vote")

data class FlagNotificationNew(@JsonProperty("permlink") val permlink: String,
                               @JsonProperty("voter") val voter: String) : NotificationNew("flag")

data class TransferNotificationNew(@JsonProperty("fromUsers") val fromUsers: List<String>,
                                   @JsonProperty("amount") val amount: String) : NotificationNew("transfer")

data class ReplyNotificationNew(@JsonProperty("permlink") val permlink: String,
                                @JsonProperty("parentPermlink") val parentPermlink: String,
                                @JsonProperty("fromUsers") val fromUsers: List<String>) : NotificationNew("reply")

data class SubscribeNotificationNew(@JsonProperty("fromUsers") val fromUsers: List<String>) : NotificationNew("subscribe")

data class UnSubscribeNotificationNew(@JsonProperty("fromUsers") val fromUsers: List<String>) : NotificationNew("unsubscribe")

data class MentionNotificationNew(@JsonProperty("permlink") val permlink: String,
                                  @JsonProperty("parentPermlink") val parentPermlink: String,
                                  @JsonProperty("fromUsers") val fromUsers: List<String>) : NotificationNew("mention")

data class RepostNotificationNew(@JsonProperty("permlink") val permlink: String,
                                 @JsonProperty("fromUsers") val fromUsers: List<String>) : NotificationNew("repost")

data class WitnessVoteNotificationNew(@JsonProperty("fromUsers") val fromUsers: List<String>) : NotificationNew("witnessVote")

data class WitnessCancelVoteNotificationNew(@JsonProperty("fromUsers") val fromUsers: List<String>) : NotificationNew("witnessCancelVote")
