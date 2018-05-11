package io.golos.golos.repository.model

import io.golos.golos.model.*
import timber.log.Timber
import java.util.*

sealed class GolosNotification(
        id: String?) {
    val id: String = id ?: UUID.randomUUID().toString()

    companion object {
        fun fromNotification(notification: Notification): GolosNotification {
            return when (notification) {
                is VoteNotification -> GolosUpVoteNotification(notification)
                is CommentNotification -> GolosCommentNotification(notification)
                is TransferNotification -> GolosTransferNotification(notification)
                is DownVoteNotification -> GolosDownVoteNotification(notification)
            }
        }
    }
}

data class GolosUpVoteNotification(val voteNotification: VoteNotification) : GolosNotification(null), PostLinkable {
    override fun getLink(): PostLinkExtractedData? {
        val fullLink = voteNotification.parentUrl
        val parts = fullLink.split("/")
        return if (parts.size == 4) {
            return PostLinkExtractedData(parts[2].substring(1), parts[1], parts[3])
        } else null
    }
}

data class GolosDownVoteNotification(val voteNotification: DownVoteNotification) : GolosNotification(null), PostLinkable {
    override fun getLink(): PostLinkExtractedData? {
        val fullLink = voteNotification.parentUrl
        val parts = fullLink.split("/")
        return if (parts.size == 4) {
            return PostLinkExtractedData(parts[2].substring(1), parts[1], parts[3])
        } else null
    }
}

data class GolosCommentNotification(val commentNotification: CommentNotification) : GolosNotification(null), PostLinkable {
    fun parentUrlCleared(): String {
        val strings = commentNotification.commentUrl.split("#")

        if (strings.size < 2) return commentNotification.parentUrl
        val secondPart = strings[1]
        val pathParts = commentNotification.commentUrl.split("/")
        if (pathParts.size < 2) return commentNotification.parentUrl
        val out = "/${pathParts[1]}/$secondPart"
        return out
    }

    override fun getLink(): PostLinkExtractedData? {
        val fullLink = parentUrlCleared()
        val parts = fullLink.split("/")
        return if (parts.size == 4) {
            return PostLinkExtractedData(parts[2].substring(1), parts[1], parts[3])
        } else null
    }
}

data class GolosTransferNotification(val transferNotification: TransferNotification) : GolosNotification(null)


data class GolosNotifications(val notifications: List<GolosNotification>)

data class PostLinkExtractedData(val author: String, val blog: String, val permlink: String)

interface PostLinkable {
    fun getLink(): PostLinkExtractedData?
}

