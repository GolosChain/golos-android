package io.golos.golos.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(value = [(JsonSubTypes.Type(value = CommentNotification::class, name = "comment")),
    (JsonSubTypes.Type(value = VoteNotification::class, name = "upvote")),
    (JsonSubTypes.Type(value = TransferNotification::class, name = "transfer")),
    (JsonSubTypes.Type(value = DownVoteNotification::class, name = "downvote"))]
)
sealed class Notification(val type: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentNotification(
        @JsonProperty("author")
        val author: NotificationUser,
        @JsonProperty("comment_url")
        val commentUrl: String,
        @JsonProperty("permlink")
        val permLink: String,
        @JsonProperty("parent_author")
        val parentAuthor: String,
        @JsonProperty("parent_depth")
        val parentDepth: Int?,
        @JsonProperty("parent_permlink")
        val parentPermlink: String,
        @JsonProperty("parent_title")
        val parentTitle: String,
        @JsonProperty("parent_body")
        val parentBody: String?,
        @JsonProperty("parent_url")
        val parentUrl: String,
        @JsonProperty("count")
        val parentCommentsCount: Int

) : Notification("comment")

@JsonIgnoreProperties(ignoreUnknown = true)

data class VoteNotification(@JsonProperty("voter")
                            val from: NotificationUser,
                            @JsonProperty("parent_permlink")
                            val parentPermlink: String,
                            @JsonProperty("parent_author")
                            val parentAuthor: String,
                            @JsonProperty("parent_depth")
                            val parentDepth: Int?,
                            @JsonProperty("parent_title")
                            val parentTitle: String,
                            @JsonProperty("parent_body")
                            val parentBody: String?,
                            @JsonProperty("parent_url")
                            val parentUrl: String,
                            @JsonProperty("count")
                            val count: Int) : Notification("upvote")

data class DownVoteNotification(@JsonProperty("voter")
                                val from: NotificationUser,
                                @JsonProperty("parent_permlink")
                                val parentPermlink: String,
                                @JsonProperty("parent_author")
                                val parentAuthor: String,
                                @JsonProperty("parent_depth")
                                val parentDepth: Int?,
                                @JsonProperty("parent_title")
                                val parentTitle: String,
                                @JsonProperty("parent_body")
                                val parentBody: String?,
                                @JsonProperty("parent_url")
                                val parentUrl: String,
                                @JsonProperty("count")
                                val count: Int,
                                @JsonProperty("weight")
                                val weight: Int) : Notification("downvote")

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransferNotification(
        @JsonProperty("_from")
        val from: NotificationUser,

        @JsonProperty("_to")
        val to: String,
        @JsonProperty("amount")
        val amount: String,
        @JsonProperty("memo")
        val memo: String?
) : Notification("transfer")

data class NotificationUser(
        @JsonProperty("account")
        val name: String,
        @JsonProperty("profile_image")
        val avatar: String?)

