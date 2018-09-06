package io.golos.golos.screens.events

import io.golos.golos.repository.services.*
import io.golos.golos.screens.story.model.SubscribeStatus

sealed class EventListItem(open val golosEvent: GolosEvent, open val avatarPath: String?) {


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
                             override val avatarPath: String? = null,
                             val title: String = "") : EventListItem(golosEvent, avatarPath) {

    companion object {
        fun create(golosEvent: GolosVoteEvent,
                   avatarPath: String? = null,
                   title: String = ""): VoteEventListItem {

            val list = getCashedItems<VoteEventListItem>(golosEvent)
            var item = list.find { it.title == title && avatarPath == it.avatarPath }
            if (item == null) {
                item = VoteEventListItem(golosEvent, avatarPath, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }

}

data class FlagEventListItem(override val golosEvent: GolosFlagEvent,
                             override val avatarPath: String? = null,
                             val title: String = "") : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosFlagEvent,
                   avatarPath: String? = null,
                   title: String = ""): FlagEventListItem {

            val list = getCashedItems<FlagEventListItem>(golosEvent)
            var item = list.find { it.title == title && it.avatarPath == avatarPath }
            if (item == null) {
                item = FlagEventListItem(golosEvent, avatarPath, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class TransferEventListItem(override val golosEvent: GolosTransferEvent,
                                 override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosTransferEvent,
                   avatarPath: String? = null): TransferEventListItem {

            val list = getCashedItems<TransferEventListItem>(golosEvent)
            var item = list.find { it.avatarPath == avatarPath }
            if (item == null) {
                item = TransferEventListItem(golosEvent, avatarPath)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class SubscribeEventListItem(override val golosEvent: GolosSubscribeEvent,
                                  override val avatarPath: String? = null,
                                  val authorSubscriptionState: SubscribeStatus = SubscribeStatus.UnsubscribedStatus,
                                  val showSubscribeButton: Boolean = false) : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosSubscribeEvent,
                   avatarPath: String? = null,
                   authorSubscriptionState: SubscribeStatus = SubscribeStatus.UnsubscribedStatus,
                   showSubscribeButton: Boolean = false): SubscribeEventListItem {

            val list = getCashedItems<SubscribeEventListItem>(golosEvent)
            var item = list.find {
                it.avatarPath == avatarPath
                        && it.authorSubscriptionState == authorSubscriptionState
                        && showSubscribeButton == it.showSubscribeButton
            }
            if (item == null) {
                item = SubscribeEventListItem(golosEvent, avatarPath, authorSubscriptionState, showSubscribeButton)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class UnSubscribeEventListItem(override val golosEvent: GolosUnSubscribeEvent,
                                    override val avatarPath: String? = null)
    : EventListItem(golosEvent, avatarPath) {

    companion object {
        fun create(golosEvent: GolosUnSubscribeEvent,
                   avatarPath: String? = null): UnSubscribeEventListItem {

            val list = getCashedItems<UnSubscribeEventListItem>(golosEvent)
            var item = list.find { it.avatarPath == avatarPath }
            if (item == null) {
                item = UnSubscribeEventListItem(golosEvent, avatarPath)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class ReplyEventListItem(override val golosEvent: GolosReplyEvent,
                              override val avatarPath: String? = null,
                              val title: String = "") : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosReplyEvent,
                   avatarPath: String? = null,
                   title: String = ""): ReplyEventListItem {

            val list = getCashedItems<ReplyEventListItem>(golosEvent)
            var item = list.find { it.title == title && it.avatarPath == avatarPath }
            if (item == null) {
                item = ReplyEventListItem(golosEvent, avatarPath, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class MentionEventListItem(override val golosEvent: GolosMentionEvent,
                                override val avatarPath: String? = null,
                                val title: String = "") : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosMentionEvent,
                   avatarPath: String? = null,
                   title: String = ""): MentionEventListItem {

            val list = getCashedItems<MentionEventListItem>(golosEvent)
            var item = list.find { it.title == title && it.avatarPath == avatarPath }
            if (item == null) {
                item = MentionEventListItem(golosEvent, avatarPath, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class RepostEventListItem(override val golosEvent: GolosRepostEvent,
                               override val avatarPath: String? = null,
                               val title: String = "") : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosRepostEvent,
                   avatarPath: String? = null,
                   title: String = ""): RepostEventListItem {

            val list = getCashedItems<RepostEventListItem>(golosEvent)
            var item = list.find { it.title == title && it.avatarPath == avatarPath }
            if (item == null) {
                item = RepostEventListItem(golosEvent, avatarPath, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class AwardEventListItem(override val golosEvent: GolosAwardEvent,
                              val actualGolosPowerAward: Float,
                              val title: String = "")
    : EventListItem(golosEvent, null) {

    companion object {
        fun create(golosEvent: GolosAwardEvent,
                   actualGolosPowerAward: Float,
                   title: String = ""): AwardEventListItem {

            val list = getCashedItems<AwardEventListItem>(golosEvent)
            var item = list.find { it.title == title && it.actualGolosPowerAward == actualGolosPowerAward }
            if (item == null) {
                item = AwardEventListItem(golosEvent, actualGolosPowerAward, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class CuratorAwardEventListItem(override val golosEvent: GolosCuratorAwardEvent,
                                     val actualGolosPowerAward: Float,
                                     val title: String = "")
    : EventListItem(golosEvent, null) {

    companion object {
        fun create(golosEvent: GolosCuratorAwardEvent,
                   actualGolosPowerAward: Float,
                   title: String = ""): CuratorAwardEventListItem {

            val list = getCashedItems<CuratorAwardEventListItem>(golosEvent)
            var item = list.find { it.title == title && it.actualGolosPowerAward == actualGolosPowerAward }
            if (item == null) {
                item = CuratorAwardEventListItem(golosEvent, actualGolosPowerAward, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class MessageEventListItem(override val golosEvent: GolosMessageEvent,
                                override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosMessageEvent,
                   avatarPath: String? = null): MessageEventListItem {

            val list = getCashedItems<MessageEventListItem>(golosEvent)
            var item = list.find { it.avatarPath == avatarPath }
            if (item == null) {
                item = MessageEventListItem(golosEvent, avatarPath)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class WitnessVoteEventListItem(override val golosEvent: GolosWitnessVoteEvent,
                                    override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosWitnessVoteEvent,
                   avatarPath: String? = null): WitnessVoteEventListItem {

            val list = getCashedItems<WitnessVoteEventListItem>(golosEvent)
            var item = list.find { it.avatarPath == avatarPath }
            if (item == null) {
                item = WitnessVoteEventListItem(golosEvent, avatarPath)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class WitnessCancelVoteEventListItem(override val golosEvent: GolosWitnessCancelVoteEvent,
                                          override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosWitnessCancelVoteEvent,
                   avatarPath: String? = null): WitnessCancelVoteEventListItem {

            val list = getCashedItems<WitnessCancelVoteEventListItem>(golosEvent)
            var item = list.find { it.avatarPath == avatarPath }
            if (item == null) {
                item = WitnessCancelVoteEventListItem(golosEvent, avatarPath)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}