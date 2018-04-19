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

data class GolosUpVoteNotification(val voteNotification: VoteNotification) : GolosNotification(null)
data class GolosDownVoteNotification(val voteNotification: DownVoteNotification) : GolosNotification(null)
data class GolosCommentNotification(val commentNotification: CommentNotification) : GolosNotification(null) {
    fun isCommentToPost() = commentNotification.parentDepth == 0
    fun parentUrlCleared(): String {
        val strings = commentNotification.commentUrl.split("#")

        if (strings.size < 2) return commentNotification.parentUrl
        val secondPart = strings[1]
        val pathParts = commentNotification.commentUrl.split("/")
        if (pathParts.size < 2) return commentNotification.parentUrl
        val out = "/${pathParts[1]}/$secondPart"
        Timber.e(out)
        return out
    }
}

data class GolosTransferNotification(val transferNotification: TransferNotification) : GolosNotification(null)


class GolosNotifications(val notifications: List<GolosNotification>)