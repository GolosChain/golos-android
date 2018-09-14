package io.golos.golos.notifications

import io.golos.golos.repository.Repository
import java.util.*

sealed class GolosNotification(open val numberOfSameType: Int = 0) {
    abstract val id: String


    companion object {
        fun fromNotification(notification: Notification): GolosNotification {
            return when (notification) {
                is VoteNotification -> GolosUpVoteNotification(notification.permlink, notification.voter, notification.counter)
                is ReplyNotification -> GolosCommentNotification(notification.permlink, notification.author, notification.counter)
                is TransferNotification -> GolosTransferNotification(notification.from, notification.amount, notification.counter)
                is FlagNotification -> GolosDownVoteNotification(notification.permlink, notification.voter, notification.counter)
                is SubscribeNotification -> GolosSubscribeNotification(notification.follower, notification.counter)
                is UnSubscribeNotification -> GolosUnSubscribeNotification(notification.follower, notification.counter)
                is MentionNotification -> GolosMentionNotification(notification.permlink, notification.author, notification.counter)
                is WitnessVoteNotification -> GolosWitnessVoteNotification(notification.from, notification.counter)
                is WitnessCancelVoteNotification -> WitnessCancelVoteGolosNotification(notification.from, notification.counter)
                is RepostNotification -> GolosRepostNotification(notification.reposter, notification.permlink, notification.counter)
                is RewardNotification -> GolosRewardNotification(notification.permlink, notification.golos, notification.golosPower, notification.gbg, notification.counter)
                is CuratorRewardNotification -> GolosCuratorRewardNotification(notification.author, notification.permlink, notification.reward, notification.counter)
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

class GolosRepostNotification(val fromUser: String,
                              val permlink: String,
                              override val numberOfSameType: Int) : GolosNotification(), PostLinkable {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosRepostNotification) return false
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

data class GolosRewardNotification(val permlink: String, val golosAward: Double,
                                   val golosPowerAward: Double, val gbgAward: Double, override val numberOfSameType: Int) : GolosNotification() {
    private val _id = UUID.randomUUID().toString()
    override val id: String
        get() = _id
}

data class GolosCuratorRewardNotification(val author: String,
                                          val permlink: String,
                                          val golosPowerReward: Double,
                                          override val numberOfSameType: Int) : GolosNotification() {
    private val _id = UUID.randomUUID().toString()
    override val id: String
        get() = _id
}

class WitnessCancelVoteGolosNotification(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WitnessCancelVoteGolosNotification) return false
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

class GolosWitnessVoteNotification(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosWitnessVoteNotification) return false
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

class GolosMentionNotification(val permlink: String,
                               val fromUser: String,
                               override val numberOfSameType: Int) : GolosNotification(), PostLinkable {

    override fun getLink() = PostLinkExtractedData(fromUser, null, permlink)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosMentionNotification) return false
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

class GolosSubscribeNotification(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosSubscribeNotification) return false
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

class GolosUnSubscribeNotification(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosUnSubscribeNotification) return false
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

class GolosUpVoteNotification(val permlink: String, val fromUser: String, override val numberOfSameType: Int) : GolosNotification(), PostLinkable {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosUpVoteNotification) return false
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


private fun String.makeLinkFromPermlink(): PostLinkExtractedData? {
    return PostLinkExtractedData(Repository.get.appUserData.value?.name
            ?: return null, null, this)
}


class GolosDownVoteNotification(val permlink: String,
                                val fromUser: String,
                                override val numberOfSameType: Int) : GolosNotification(), PostLinkable {
    override val id: String = UUID.randomUUID().toString()
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosDownVoteNotification) return false
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


class GolosCommentNotification(val permlink: String,
                               val fromUser: String,
                               override val numberOfSameType: Int) :

        GolosNotification(), PostLinkable {

    override val id: String = UUID.randomUUID().toString()

    override fun getLink() = PostLinkExtractedData(fromUser, null, permlink)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosCommentNotification) return false
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


class GolosTransferNotification(val fromUser: String, val amount: String, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GolosTransferNotification) return false
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

