package io.golos.golos.notifications

import io.golos.golos.repository.Repository
import java.util.*

sealed class GolosNotification(open val numberOfSameType: Int = 0) {
    abstract val id: String


    companion object {
        fun fromNotification(notification: NotificationNew): GolosNotification {
            return when (notification) {
                is VoteNotificationNew -> GolosUpVoteNotificationNew(notification.permlink, notification.voter, notification.counter)
                is ReplyNotificationNew -> GolosCommentNotificationNew(notification.permlink, notification.author, notification.counter)
                is TransferNotificationNew -> GolosTransferNotificationNew(notification.from, notification.amount.toString().plus(" GOLOS"), notification.counter)
                is FlagNotificationNew -> GolosDownVoteNotificationNew(notification.permlink, notification.voter, notification.counter)
                is SubscribeNotificationNew -> GolosSubscribeNotificationNew(notification.follower, notification.counter)
                is UnSubscribeNotificationNew -> GolosUnSubscribeNotificationNew(notification.follower, notification.counter)
                is MentionNotificationNew -> GolosMentionNotificationNew(notification.permlink, notification.author, notification.counter)
                is WitnessVoteNotificationNew -> GolosWitnessVoteNotificationNew(notification.voter, notification.counter)
                is WitnessCancelVoteNotificationNew -> WitnessCancelVoteGolosNotificationNew(notification.voter, notification.counter)
                is RepostNotificationNew -> GolosRepostNotificationNew(notification.reposter, notification.permlink, notification.counter)
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
    override fun getLink() = permlink.extractLinkFromNew()
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

class WitnessCancelVoteGolosNotificationNew(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
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

class GolosWitnessVoteNotificationNew(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
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

class GolosMentionNotificationNew(val permlink: String,
                                  val fromUser: String,
                                  override val numberOfSameType: Int) : GolosNotification(), PostLinkable {

    override fun getLink() = PostLinkExtractedData(fromUser, null, permlink)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosMentionNotificationNew) return false
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

class GolosSubscribeNotificationNew(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
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

class GolosUnSubscribeNotificationNew(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
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
    override fun getLink() = permlink.extractLinkFromNew()
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


private fun String.extractLinkFromNew(): PostLinkExtractedData? {
    return PostLinkExtractedData(Repository.get.appUserData.value?.userName
            ?: return null, null, this)
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


class GolosCommentNotificationNew(val permlink: String,
                                  val fromUser: String,
                                  override val numberOfSameType: Int) :

        GolosNotification(), PostLinkable {

    override val id: String = UUID.randomUUID().toString()

    override fun getLink() = PostLinkExtractedData(fromUser, null, permlink)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosCommentNotificationNew) return false
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


class GolosTransferNotificationNew(val fromUser: String, val amount: String, override val numberOfSameType: Int) : GolosNotification() {
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

