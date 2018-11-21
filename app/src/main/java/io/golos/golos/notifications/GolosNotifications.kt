package io.golos.golos.notifications

import io.golos.golos.repository.Repository
import io.golos.golos.repository.services.model.*
import java.util.*

sealed class GolosNotification(open val numberOfSameType: Int = 0) {
    abstract val id: String


    companion object {
        fun fromEvent(event: Event): GolosNotification {
            return when (event) {
                is VoteEvent -> GolosUpVoteNotification(event.permlink, event.fromUsers.firstOrNull().orEmpty(), event.counter)
                is ReplyEvent -> GolosCommentNotification(event.permlink, event.fromUsers.firstOrNull().orEmpty(), event.counter)
                is TransferEvent -> GolosTransferNotification(event.fromUsers.firstOrNull().orEmpty(), event.amount, event.counter)
                is FlagEvent -> GolosDownVoteNotification(event.permlink, event.fromUsers.firstOrNull().orEmpty(), event.counter)
                is SubscribeEvent -> GolosSubscribeNotification(event.fromUsers.firstOrNull().orEmpty(), event.counter)
                is UnSubscribeEvent -> GolosUnSubscribeNotification(event.fromUsers.firstOrNull().orEmpty(), event.counter)
                is MentionEvent -> GolosMentionNotification(event.permlink, event.fromUsers.firstOrNull().orEmpty(), event.counter)
                is WitnessVoteEvent -> GolosWitnessVoteNotification(event.fromUsers.firstOrNull().orEmpty(), event.counter)
                is WitnessCancelVoteEvent -> WitnessCancelVoteGolosNotification(event.fromUsers.firstOrNull().orEmpty(), event.counter)
                is RepostEvent -> GolosRepostNotification(event.fromUsers.firstOrNull().orEmpty(), event.permlink, event.counter)
                is AwardEvent -> GolosRewardNotification(event.permlink, event.reward.golos, event.reward.golosPower, event.reward.gbg, event.counter)
                is CuratorAwardEvent -> GolosCuratorRewardNotification(event.curatorTargetAuthor, event.permlink, event.curatorReward, event.counter)
                is MessageEvent -> GolosMessageNotification(event.fromUsers.firstOrNull().orEmpty(), event.counter)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GolosNotification

        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        return numberOfSameType
    }

}

class GolosRepostNotification(val fromUser: String,
                              val permlink: String,
                              override val numberOfSameType: Int) : GolosNotification(), PostLinkable {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GolosRepostNotification

        if (fromUser != other.fromUser) return false
        if (permlink != other.permlink) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + numberOfSameType
        return result
    }

    override val id: String = UUID.randomUUID().toString()


}

class GolosMessageNotification(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
}

class GolosRewardNotification(val permlink: String, val golosAward: Double,
                              val golosPowerAward: Double, val gbgAward: Double, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GolosRewardNotification

        if (permlink != other.permlink) return false
        if (golosAward != other.golosAward) return false
        if (golosPowerAward != other.golosPowerAward) return false
        if (gbgAward != other.gbgAward) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + golosAward.hashCode()
        result = 31 * result + golosPowerAward.hashCode()
        result = 31 * result + gbgAward.hashCode()
        result = 31 * result + numberOfSameType
        return result
    }

}

class GolosCuratorRewardNotification(val author: String,
                                     val permlink: String,
                                     val golosPowerReward: Double,
                                     override val numberOfSameType: Int) : GolosNotification() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GolosCuratorRewardNotification

        if (author != other.author) return false
        if (permlink != other.permlink) return false
        if (golosPowerReward != other.golosPowerReward) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override val id: String = UUID.randomUUID().toString()

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + golosPowerReward.hashCode()
        result = 31 * result + numberOfSameType
        return result
    }
}

class WitnessCancelVoteGolosNotification(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as WitnessCancelVoteGolosNotification

        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        return result
    }

}

class GolosWitnessVoteNotification(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GolosWitnessVoteNotification

        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        return result
    }

}

class GolosMentionNotification(val permlink: String,
                               val fromUser: String,
                               override val numberOfSameType: Int) : GolosNotification(), PostLinkable {

    override fun getLink() = PostLinkExtractedData(fromUser, null, permlink)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GolosMentionNotification

        if (permlink != other.permlink) return false
        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        return result
    }


    override val id: String = UUID.randomUUID().toString()

}

class GolosSubscribeNotification(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GolosSubscribeNotification

        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        return result
    }


}

class GolosUnSubscribeNotification(val fromUser: String, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GolosUnSubscribeNotification

        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        return result
    }

}

class GolosUpVoteNotification(val permlink: String, val fromUser: String, override val numberOfSameType: Int) : GolosNotification(), PostLinkable {
    override fun getLink() = permlink.makeLinkFromPermlink()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GolosUpVoteNotification

        if (permlink != other.permlink) return false
        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
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
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GolosDownVoteNotification

        if (permlink != other.permlink) return false
        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
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
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GolosCommentNotification

        if (permlink != other.permlink) return false
        if (fromUser != other.fromUser) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + permlink.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + numberOfSameType
        return result
    }


}


class GolosTransferNotification(val fromUser: String, val amount: String, override val numberOfSameType: Int) : GolosNotification() {
    override val id: String = UUID.randomUUID().toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as GolosTransferNotification

        if (fromUser != other.fromUser) return false
        if (amount != other.amount) return false
        if (numberOfSameType != other.numberOfSameType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fromUser.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + numberOfSameType
        return result
    }

}


data class GolosNotifications(val notifications: List<GolosNotification>)

data class PostLinkExtractedData(val author: String, val blog: String?, val permlink: String)

interface PostLinkable {
    fun getLink(): PostLinkExtractedData?
}

