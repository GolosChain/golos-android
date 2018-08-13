package io.golos.golos.notifications

import io.golos.golos.model.*
import java.util.*

sealed class GolosNotification {
    val id: String = UUID.randomUUID().toString()

    companion object {
        fun fromNotification(notification: Notification): GolosNotification {
            return when (notification) {
                is VoteNotification -> GolosUpVoteNotification(notification)
                is CommentNotification -> GolosCommentNotification(notification)
                is TransferNotification -> GolosTransferNotification(notification)
                is DownVoteNotification -> GolosDownVoteNotification(notification)
            }
        }

        fun fromNotification(notification: NotificationNew): GolosNotification {
            return when (notification) {
                is VoteNotificationNew -> GolosUpVoteNotificationNew(notification.permlink, notification.fromUsers.firstOrNull().orEmpty())
                is ReplyNotificationNew -> GolosCommentNotificationNew(notification.permlink, notification.parentPermlink, notification.fromUsers.firstOrNull().orEmpty())
                is TransferNotificationNew -> GolosTransferNotificationNew(notification.fromUsers.firstOrNull().orEmpty(), notification.amount)
                is FlagNotificationNew -> GolosDownVoteNotificationNew(notification.permlink, notification.fromUsers.firstOrNull().orEmpty())
                is SubscribeNotificationNew -> GolosSubscribeNotificationNew(notification.fromUsers.firstOrNull().orEmpty())
                is UnSubscribeNotificationNew -> GolosUnSubscribeNotificationNew(notification.fromUsers.firstOrNull().orEmpty())
                is MentionNotificationNew -> GolosMentionNotificationNew(notification.permlink, notification.parentPermlink, notification.fromUsers.firstOrNull().orEmpty())
                is WitnessVoteNotificationNew -> GolosWitnessVoteNotificationNew(notification.fromUsers.firstOrNull().orEmpty())
                is WitnessCancelVoteNotificationNew -> WitnessCancelVoteGolosNotificationNew(notification.fromUsers.firstOrNull().orEmpty())
                is RepostNotificationNew -> GolosRepostNotificationNew(notification.fromUsers.firstOrNull().orEmpty(), notification.permlink)
            }
        }
    }
}

data class GolosRepostNotificationNew(val fromUser: String, val permlink: String) : GolosNotification(), PostLinkable {
    override fun getLink() = permlink.extractLink()
}

data class WitnessCancelVoteGolosNotificationNew(val fromUser: String) : GolosNotification()

data class GolosWitnessVoteNotificationNew(val fromUser: String) : GolosNotification()

data class GolosMentionNotificationNew(val permlink: String, val parentPermlink: String, val fromUser: String) : GolosNotification(), PostLinkable {
    fun parentUrlCleared(): String {
        val strings = permlink.split("#")

        if (strings.size < 2) return parentPermlink
        val secondPart = strings[1]
        val pathParts = permlink.split("/")
        if (pathParts.size < 2) return parentPermlink
        val out = "/${pathParts[1]}/$secondPart"
        return out
    }

    override fun getLink() = parentUrlCleared().extractLink()
}

data class GolosSubscribeNotificationNew(val fromUser: String) : GolosNotification()
data class GolosUnSubscribeNotificationNew(val fromUser: String) : GolosNotification()

data class GolosUpVoteNotificationNew(val permlink: String, val fromUser: String) : GolosNotification(), PostLinkable {
    override fun getLink() = permlink.extractLink()
}

data class GolosUpVoteNotification(val voteNotification: VoteNotification) : GolosNotification(), PostLinkable {
    override fun getLink() = voteNotification.parentUrl.extractLink()
}

private fun String.extractLink(): PostLinkExtractedData? {
    val parts = this.split("/")
    return if (parts.size == 4) {
        return PostLinkExtractedData(parts[2].substring(1), parts[1], parts[3])
    } else null
}

data class GolosDownVoteNotification(val voteNotification: DownVoteNotification) : GolosNotification(), PostLinkable {
    override fun getLink() = voteNotification.parentUrl.extractLink()
}

data class GolosDownVoteNotificationNew(val permlink: String, val fromUser: String) : GolosNotification(), PostLinkable {
    override fun getLink() = permlink.extractLink()
}


data class GolosCommentNotification(val commentNotification: CommentNotification) : GolosNotification(), PostLinkable {
    fun parentUrlCleared(): String {
        val strings = commentNotification.commentUrl.split("#")

        if (strings.size < 2) return commentNotification.parentUrl
        val secondPart = strings[1]
        val pathParts = commentNotification.commentUrl.split("/")
        if (pathParts.size < 2) return commentNotification.parentUrl
        val out = "/${pathParts[1]}/$secondPart"
        return out
    }

    override fun getLink() = parentUrlCleared().extractLink()
}

data class GolosCommentNotificationNew(val permlink: String, val parentPermlink: String, val fromUser: String) :
        GolosNotification(), PostLinkable {
    fun parentUrlCleared(): String {
        val strings = permlink.split("#")

        if (strings.size < 2) return parentPermlink
        val secondPart = strings[1]
        val pathParts = permlink.split("/")
        if (pathParts.size < 2) return parentPermlink
        val out = "/${pathParts[1]}/$secondPart"
        return out
    }

    override fun getLink() = parentUrlCleared().extractLink()
}


data class GolosTransferNotification(val transferNotification: TransferNotification) : GolosNotification()

data class GolosTransferNotificationNew(val fromUser: String, val amount: Double) : GolosNotification()


data class GolosNotifications(val notifications: List<GolosNotification>)

data class PostLinkExtractedData(val author: String, val blog: String, val permlink: String)

interface PostLinkable {
    fun getLink(): PostLinkExtractedData?
}

