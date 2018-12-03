package io.golos.golos.screens.events

import io.golos.golos.repository.services.model.*
import io.golos.golos.screens.story.model.SubscribeStatus

sealed class EventListItem(open val golosEvent: GolosEvent,
                           open val avatarPath: String?,
                           open val isFresh: Boolean,
                           open val isAuthorClickable: Boolean) {


    protected companion object {
        @JvmStatic
        private val map = HashMap<GolosEvent, List<EventListItem>>(100)

        fun <T : EventListItem> getCashedItems(event: GolosEvent): List<T> {
            if (!map.containsKey(event)) {
                map[event] = ArrayList(100)
            }
            return map[event]!! as List<T>
        }

        fun <T : EventListItem> putItemToCash(event: GolosEvent, item: T) {
            (getCashedItems<T>(event) as ArrayList<T>).add(item)

        }
    }
}


data class VoteEventListItem(override val golosEvent: GolosVoteEvent,
                             override val avatarPath: String?,
                             override val isFresh: Boolean,
                             val title: String,
                             override val isAuthorClickable: Boolean) : EventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable) {

    companion object {
        fun create(golosEvent: GolosVoteEvent,
                   avatarPath: String?,
                   isFresh: Boolean,
                   title: String,
                   isAuthorClickable: Boolean): VoteEventListItem {

            val list = getCashedItems<VoteEventListItem>(golosEvent)
            var item = list.find {
                it.title == title
                        && isFresh == it.isFresh
                        && avatarPath == it.avatarPath
                        && it.isAuthorClickable == isAuthorClickable
            }
            if (item == null) {
                item = VoteEventListItem(golosEvent, avatarPath, isFresh, title, isAuthorClickable)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }

}

data class FlagEventListItem(override val golosEvent: GolosFlagEvent,
                             override val avatarPath: String?,
                             override val isFresh: Boolean,
                             val title: String,
                             override val isAuthorClickable: Boolean) : EventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable) {
    companion object {
        fun create(golosEvent: GolosFlagEvent,
                   avatarPath: String?,
                   isFresh: Boolean,
                   title: String,
                   isAuthorClickable: Boolean): FlagEventListItem {

            val list = getCashedItems<FlagEventListItem>(golosEvent)
            var item = list.find {
                it.title == title
                        && isFresh == it.isFresh
                        && it.avatarPath === avatarPath
                        && it.isAuthorClickable == isAuthorClickable
            }
            if (item == null) {
                item = FlagEventListItem(golosEvent, avatarPath, isFresh, title, isAuthorClickable)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class TransferEventListItem(override val golosEvent: GolosTransferEvent,
                                 override val avatarPath: String?,
                                 override val isFresh: Boolean,
                                 override val isAuthorClickable: Boolean) : EventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable) {
    companion object {
        fun create(golosEvent: GolosTransferEvent,
                   avatarPath: String?,
                   isFresh: Boolean,
                   isAuthorClickable: Boolean): TransferEventListItem {

            val list = getCashedItems<TransferEventListItem>(golosEvent)
            var item = list.find {
                it.avatarPath == avatarPath
                        && isAuthorClickable == it.isAuthorClickable
                        && isFresh == it.isFresh
            }
            if (item == null) {
                item = TransferEventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class SubscribeEventListItem(override val golosEvent: GolosSubscribeEvent,
                                  override val avatarPath: String?,
                                  override val isAuthorClickable: Boolean,
                                  override val isFresh: Boolean,
                                  val authorSubscriptionState: SubscribeStatus = SubscribeStatus.UnsubscribedStatus,
                                  val showSubscribeButton: Boolean = false) : EventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable) {
    companion object {
        fun create(golosEvent: GolosSubscribeEvent,
                   avatarPath: String?,
                   isFresh: Boolean,
                   isAuthorClickable: Boolean,
                   authorSubscriptionState: SubscribeStatus = SubscribeStatus.UnsubscribedStatus,
                   showSubscribeButton: Boolean = false): SubscribeEventListItem {

            val list = getCashedItems<SubscribeEventListItem>(golosEvent)
            var item = list.find {
                it.avatarPath == avatarPath
                        && it.authorSubscriptionState == authorSubscriptionState
                        && isFresh == it.isFresh
                        && showSubscribeButton == it.showSubscribeButton
                        && it.isAuthorClickable == isAuthorClickable
            }
            if (item == null) {
                item = SubscribeEventListItem(golosEvent, avatarPath, isAuthorClickable, isFresh, authorSubscriptionState, showSubscribeButton)
                putItemToCash(golosEvent, item)
            }

            return item
        }
    }
}

data class UnSubscribeEventListItem(override val golosEvent: GolosUnSubscribeEvent,
                                    override val avatarPath: String?,
                                    override val isFresh: Boolean,
                                    override val isAuthorClickable: Boolean)
    : EventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable) {

    companion object {
        fun create(golosEvent: GolosUnSubscribeEvent,
                   avatarPath: String?,
                   isFresh: Boolean,
                   isAuthorClickable: Boolean): UnSubscribeEventListItem {

            val list = getCashedItems<UnSubscribeEventListItem>(golosEvent)
            var item = list.find {
                it.avatarPath == avatarPath &&
                        it.isFresh == isFresh &&
                        it.isAuthorClickable == isAuthorClickable
            }
            if (item == null) {
                item = UnSubscribeEventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class ReplyEventListItem(override val golosEvent: GolosReplyEvent,
                              override val avatarPath: String?,
                              override val isFresh: Boolean,
                              override val isAuthorClickable: Boolean,
                              val title: String = "") : EventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable) {
    companion object {
        fun create(golosEvent: GolosReplyEvent,
                   isAuthorClickable: Boolean,
                   avatarPath: String?,
                   isFresh: Boolean,
                   title: String): ReplyEventListItem {

            val list = getCashedItems<ReplyEventListItem>(golosEvent)
            var item = list.find { it.title == title && it.avatarPath == avatarPath && it.isAuthorClickable == isAuthorClickable && isFresh == it.isFresh }
            if (item == null) {
                item = ReplyEventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class MentionEventListItem(override val golosEvent: GolosMentionEvent,
                                override val isAuthorClickable: Boolean,
                                override val avatarPath: String?,
                                override val isFresh: Boolean,
                                val title: String) : EventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable) {
    companion object {
        fun create(golosEvent: GolosMentionEvent,
                   isAuthorClickable: Boolean,
                   avatarPath: String?,
                   isFresh: Boolean,
                   title: String): MentionEventListItem {

            val list = getCashedItems<MentionEventListItem>(golosEvent)
            var item = list.find {
                it.title == title
                        && it.avatarPath == avatarPath
                        && it.isFresh == isFresh
                        && it.isAuthorClickable == isAuthorClickable
            }
            if (item == null) {
                item = MentionEventListItem(golosEvent, isAuthorClickable, avatarPath, isFresh, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class RepostEventListItem(override val golosEvent: GolosRepostEvent,
                               override val isAuthorClickable: Boolean,
                               override val isFresh: Boolean,
                               override val avatarPath: String?,
                               val title: String) : EventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable) {
    companion object {
        fun create(golosEvent: GolosRepostEvent,
                   isAuthorClickable: Boolean,
                   isFresh: Boolean,
                   avatarPath: String?,
                   title: String): RepostEventListItem {

            val list = getCashedItems<RepostEventListItem>(golosEvent)
            var item = list.find { it.isFresh == isFresh && it.title == title && it.avatarPath == avatarPath && it.isAuthorClickable == isAuthorClickable }
            if (item == null) {
                item = RepostEventListItem(golosEvent, isAuthorClickable, isFresh, avatarPath, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class AwardEventListItem(override val golosEvent: GolosAwardEvent,
                              override val isFresh: Boolean,
                              val actualGolosPowerAward: Float,
                              val title: String = "")
    : EventListItem(golosEvent, null, false, isFresh) {

    companion object {
        fun create(golosEvent: GolosAwardEvent,
                   actualGolosPowerAward: Float,
                   isFresh: Boolean,
                   title: String): AwardEventListItem {

            val list = getCashedItems<AwardEventListItem>(golosEvent)
            var item = list.find { isFresh == it.isFresh && it.title == title && it.actualGolosPowerAward == actualGolosPowerAward }
            if (item == null) {
                item = AwardEventListItem(golosEvent, isFresh, actualGolosPowerAward, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class CuratorAwardEventListItem(override val golosEvent: GolosCuratorAwardEvent,
                                     override val isFresh: Boolean,
                                     val actualGolosPowerAward: Float,
                                     val title: String)
    : EventListItem(golosEvent, null, isAuthorClickable = false, isFresh = isFresh) {

    companion object {
        fun create(golosEvent: GolosCuratorAwardEvent,
                   actualGolosPowerAward: Float,
                   isFresh: Boolean,
                   title: String): CuratorAwardEventListItem {

            val list = getCashedItems<CuratorAwardEventListItem>(golosEvent)
            var item = list.find { it.isFresh == isFresh && it.title == title && it.actualGolosPowerAward == actualGolosPowerAward }
            if (item == null) {
                item = CuratorAwardEventListItem(golosEvent, isFresh, actualGolosPowerAward, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class MessageEventListItem(override val golosEvent: GolosMessageEvent,
                                override val isFresh: Boolean,
                                override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath, isFresh, false) {
    companion object {
        fun create(golosEvent: GolosMessageEvent,
                   isFresh: Boolean,
                   avatarPath: String? = null): MessageEventListItem {

            val list = getCashedItems<MessageEventListItem>(golosEvent)
            var item = list.find { it.avatarPath == avatarPath && it.isFresh == isFresh }
            if (item == null) {
                item = MessageEventListItem(golosEvent, isFresh, avatarPath)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class WitnessVoteEventListItem(override val golosEvent: GolosWitnessVoteEvent,
                                    override val isFresh: Boolean,
                                    override val isAuthorClickable: Boolean,
                                    override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable) {
    companion object {
        fun create(golosEvent: GolosWitnessVoteEvent,
                   isAuthorClickable: Boolean,
                   isFresh: Boolean,
                   avatarPath: String?): WitnessVoteEventListItem {

            val list = getCashedItems<WitnessVoteEventListItem>(golosEvent)
            var item = list.find { it.avatarPath == avatarPath && it.isAuthorClickable == isAuthorClickable && it.isFresh == isFresh }
            if (item == null) {
                item = WitnessVoteEventListItem(golosEvent, isAuthorClickable, isFresh, avatarPath)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class WitnessCancelVoteEventListItem(override val golosEvent: GolosWitnessCancelVoteEvent,
                                          override val isAuthorClickable: Boolean,
                                          override val isFresh: Boolean,
                                          override val avatarPath: String?) : EventListItem(golosEvent, avatarPath, isFresh, isAuthorClickable) {
    companion object {
        fun create(golosEvent: GolosWitnessCancelVoteEvent,
                   isAuthorClickable: Boolean,
                   isFresh: Boolean,
                   avatarPath: String?): WitnessCancelVoteEventListItem {

            val list = getCashedItems<WitnessCancelVoteEventListItem>(golosEvent)
            var item = list.find { it.avatarPath == avatarPath && it.isAuthorClickable == isAuthorClickable && isFresh == it.isFresh }
            if (item == null) {
                item = WitnessCancelVoteEventListItem(golosEvent, isAuthorClickable, isFresh, avatarPath)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}