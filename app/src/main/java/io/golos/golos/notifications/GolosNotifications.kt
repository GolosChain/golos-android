package io.golos.golos.notifications

import io.golos.golos.model.*
import io.golos.golos.repository.Repository
import java.util.*

sealed class GolosNotification(open val numberOfSameType: Int = 0) {
   abstract val id: String



    companion object {
        fun fromNotification(notification: NotificationNew): GolosNotification {
            return when (notification) {
                is VoteNotificationNew -> GolosUpVoteNotificationNew(notification.permlink, notification.voter, notification.counter)
                is ReplyNotificationNew -> GolosCommentNotificationNew(notification.permlink, notification.parentPermlink, notification.fromUsers.firstOrNull().orEmpty(), notification.counter)
                is TransferNotificationNew -> GolosTransferNotificationNew(notification.fromUsers.firstOrNull().orEmpty(), notification.amount, notification.counter)
                is FlagNotificationNew -> GolosDownVoteNotificationNew(notification.permlink, notification.voter, notification.counter)
                is SubscribeNotificationNew -> GolosSubscribeNotificationNew(notification.fromUsers.firstOrNull().orEmpty(), notification.counter)
                is UnSubscribeNotificationNew -> GolosUnSubscribeNotificationNew(notification.fromUsers.firstOrNull().orEmpty(), notification.counter)
                is MentionNotificationNew -> GolosMentionNotificationNew(notification.permlink, notification.parentPermlink, notification.fromUsers.firstOrNull().orEmpty(), notification.counter)
                is WitnessVoteNotificationNew -> GolosWitnessVoteNotificationNew(notification.fromUsers.firstOrNull().orEmpty(), notification.counter)
                is WitnessCancelVoteNotificationNew -> WitnessCancelVoteGolosNotificationNew(notification.fromUsers.firstOrNull().orEmpty(), notification.counter)
                is RepostNotificationNew -> GolosRepostNotificationNew(notification.fromUsers.firstOrNull().orEmpty(), notification.permlink, notification.counter)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosNotification) return false

        if (numberOfSameType != other.numberOfSameType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = numberOfSameType
        result = 31 * result + id.hashCode()
        return result
    }
}

class GolosRepostNotificationNew(val fromUser: String,
                                      val permlink: String,
                                      override val numberOfSameType: Int) : GolosNotification(), PostLinkable {
    override fun getLink() = permlink.extractLink()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosRepostNotificationNew) return false
        if (!super.equals(other)) return false

        if (fromUser != other.fromUser) return false
        if (permlink != other.permlink) return false
        if (numberOfSameType != other.numberOfSameType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + numberOfSameType
        result = 31 * result + id.hashCode()
        return result
    }

    override val id: String = UUID.randomUUID().toString()


}

class WitnessCancelVoteGolosNotificationNew(val fromUser: String, override val numberOfSameType: Int) : GolosNotification(){
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WitnessCancelVoteGolosNotificationNew) return false
        if (!super.equals(other)) return false

        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        result = 31 * result + id.hashCode()
        return result
    }

}

class GolosWitnessVoteNotificationNew(val fromUser: String, override val numberOfSameType: Int) : GolosNotification(){
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosWitnessVoteNotificationNew) return false
        if (!super.equals(other)) return false

        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        result = 31 * result + id.hashCode()
        return result
    }

}

class GolosMentionNotificationNew(val permlink: String, val parentPermlink: String,
                                       val fromUser: String, override val numberOfSameType: Int) : GolosNotification(), PostLinkable {
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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosMentionNotificationNew) return false
        if (!super.equals(other)) return false

        if (permlink != other.permlink) return false
        if (parentPermlink != other.parentPermlink) return false
        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + parentPermlink.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        result = 31 * result + id.hashCode()
        return result
    }

    override val id: String = UUID.randomUUID().toString()

}

class GolosSubscribeNotificationNew(val fromUser: String, override val numberOfSameType: Int) : GolosNotification(){
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosSubscribeNotificationNew) return false
        if (!super.equals(other)) return false

        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        result = 31 * result + id.hashCode()
        return result
    }


}
class GolosUnSubscribeNotificationNew(val fromUser: String, override val numberOfSameType: Int) : GolosNotification(){
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosUnSubscribeNotificationNew) return false
        if (!super.equals(other)) return false

        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        result = 31 * result + id.hashCode()
        return result
    }

}

class GolosUpVoteNotificationNew(val permlink: String, val fromUser: String, override val numberOfSameType: Int) : GolosNotification(), PostLinkable {
    override fun getLink() = permlink.extractLink()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosUpVoteNotificationNew) return false
        if (!super.equals(other)) return false

        if (permlink != other.permlink) return false
        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        result = 31 * result + id.hashCode()
        return result
    }

    override val id: String = UUID.randomUUID().toString()

}

class GolosUpVoteNotification(val voteNotification: VoteNotification) : GolosNotification(), PostLinkable {
    override val id: String = UUID.randomUUID().toString()
    override fun getLink() = voteNotification.parentUrl.extractLink()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosUpVoteNotification) return false
        if (!super.equals(other)) return false

        if (voteNotification != other.voteNotification) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + voteNotification.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }


}

private fun String.extractLink(): PostLinkExtractedData? {
    val parts = this.split("/")
    return if (parts.size == 4) {
        return PostLinkExtractedData(parts[2].substring(1), parts[1], parts[3])
    } else null
}

private fun String.extractLinkFromNew(): PostLinkExtractedData? {
    return PostLinkExtractedData(Repository.get.appUserData.value?.userName
            ?: return null, null, this)
}

class GolosDownVoteNotification(val voteNotification: DownVoteNotification) : GolosNotification(), PostLinkable {
    override val id: String = UUID.randomUUID().toString()
    override fun getLink() = voteNotification.parentUrl.extractLinkFromNew()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosDownVoteNotification) return false
        if (!super.equals(other)) return false

        if (voteNotification != other.voteNotification) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + voteNotification.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

}

class GolosDownVoteNotificationNew(val permlink: String,
                                   val fromUser: String,
                                   override val numberOfSameType: Int) : GolosNotification(), PostLinkable {
    override val id: String = UUID.randomUUID().toString()
    override fun getLink() = permlink.extractLinkFromNew()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosDownVoteNotificationNew) return false
        if (!super.equals(other)) return false

        if (permlink != other.permlink) return false
        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        result = 31 * result + id.hashCode()
        return result
    }
}


class GolosCommentNotification(val commentNotification: CommentNotification) : GolosNotification(), PostLinkable {
    override val id: String = UUID.randomUUID().toString()
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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosCommentNotification) return false
        if (!super.equals(other)) return false

        if (commentNotification != other.commentNotification) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + commentNotification.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

}

class GolosCommentNotificationNew(val permlink: String, val parentPermlink: String, val fromUser: String, override val numberOfSameType: Int) :

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
    override val id: String = UUID.randomUUID().toString()

    override fun getLink() = parentUrlCleared().extractLink()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosCommentNotificationNew) return false
        if (!super.equals(other)) return false

        if (permlink != other.permlink) return false
        if (parentPermlink != other.parentPermlink) return false
        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + parentPermlink.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        result = 31 * result + id.hashCode()
        return result
    }


}


class GolosTransferNotification(val transferNotification: TransferNotification) : GolosNotification(){
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosTransferNotification) return false
        if (!super.equals(other)) return false

        if (transferNotification != other.transferNotification) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + transferNotification.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

}

class GolosTransferNotificationNew(val fromUser: String, val amount: String, override val numberOfSameType: Int) : GolosNotification(){
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosTransferNotificationNew) return false
        if (!super.equals(other)) return false

        if (fromUser != other.fromUser) return false
        if (amount != other.amount) return false
        if (numberOfSameType != other.numberOfSameType) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + numberOfSameType
        result = 31 * result + id.hashCode()
        return result
    }

}


data class GolosNotifications(val notifications: List<GolosNotification>)

data class PostLinkExtractedData(val author: String, val blog: String?, val permlink: String)

interface PostLinkable {
    fun getLink(): PostLinkExtractedData?
}

