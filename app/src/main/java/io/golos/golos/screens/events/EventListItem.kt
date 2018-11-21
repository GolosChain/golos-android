package io.golos.golos.screens.events

import io.golos.golos.repository.services.model.*
import io.golos.golos.screens.story.model.SubscribeStatus

sealed class EventListItem(open val golosEvent: GolosEvent,
                           open val avatarPath: String?,
                           open val isAuthorClickable: Boolean = false) {


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
                             val title: String = "",
                             override val isAuthorClickable: Boolean) : EventListItem(golosEvent, avatarPath) {

    companion object {
        fun create(golosEvent: GolosVoteEvent,
                   avatarPath: String? = null,
                   title: String = "",
                   isAuthorClickable: Boolean): VoteEventListItem {

            val list = getCashedItems<VoteEventListItem>(golosEvent)
            var item = list.find {
                it.title == title
                        && avatarPath == it.avatarPath
                        && it.isAuthorClickable == isAuthorClickable
            }
            if (item == null) {
                item = VoteEventListItem(golosEvent, avatarPath, title, isAuthorClickable)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }

}

data class FlagEventListItem(override val golosEvent: GolosFlagEvent,
                             override val avatarPath: String? = null,
                             val title: String = "",
                             override val isAuthorClickable: Boolean) : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosFlagEvent,
                   avatarPath: String? = null,
                   title: String = "",
                   isAuthorClickable: Boolean): FlagEventListItem {

            val list = getCashedItems<FlagEventListItem>(golosEvent)
            var item = list.find {
                it.title == title
                        && it.avatarPath === avatarPath
                        && it.isAuthorClickable == isAuthorClickable
            }
            if (item == null) {
                item = FlagEventListItem(golosEvent, avatarPath, title, isAuthorClickable)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class TransferEventListItem(override val golosEvent: GolosTransferEvent,
                                 override val avatarPath: String? = null,
                                 override val isAuthorClickable: Boolean) : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosTransferEvent,
                   avatarPath: String? = null,
                   isAuthorClickable: Boolean): TransferEventListItem {

            val list = getCashedItems<TransferEventListItem>(golosEvent)
            var item = list.find { it.avatarPath == avatarPath && isAuthorClickable == it.isAuthorClickable }
            if (item == null) {
                item = TransferEventListItem(golosEvent, avatarPath, isAuthorClickable)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class SubscribeEventListItem(override val golosEvent: GolosSubscribeEvent,
                                  override val avatarPath: String? = null,
                                  override val isAuthorClickable: Boolean,
                                  val authorSubscriptionState: SubscribeStatus = SubscribeStatus.UnsubscribedStatus,
                                  val showSubscribeButton: Boolean = false) : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosSubscribeEvent,
                   avatarPath: String? = null,
                   isAuthorClickable: Boolean,
                   authorSubscriptionState: SubscribeStatus = SubscribeStatus.UnsubscribedStatus,
                   showSubscribeButton: Boolean = false): SubscribeEventListItem {

            val list = getCashedItems<SubscribeEventListItem>(golosEvent)
            var item = list.find {
                it.avatarPath == avatarPath
                        && it.authorSubscriptionState == authorSubscriptionState
                        && showSubscribeButton == it.showSubscribeButton
                        && it.isAuthorClickable == isAuthorClickable
            }
            if (item == null) {
                item = SubscribeEventListItem(golosEvent, avatarPath, isAuthorClickable, authorSubscriptionState, showSubscribeButton)
                putItemToCash(golosEvent, item)
            }

            return item
        }
    }
}

data class UnSubscribeEventListItem(override val golosEvent: GolosUnSubscribeEvent,
                                    override val avatarPath: String? = null,
                                    override val isAuthorClickable: Boolean)
    : EventListItem(golosEvent, avatarPath) {

    companion object {
        fun create(golosEvent: GolosUnSubscribeEvent,
                   avatarPath: String? = null,
                   isAuthorClickable: Boolean): UnSubscribeEventListItem {

            val list = getCashedItems<UnSubscribeEventListItem>(golosEvent)
            var item = list.find {
                it.avatarPath == avatarPath &&
                        it.isAuthorClickable == isAuthorClickable
            }
            if (item == null) {
                item = UnSubscribeEventListItem(golosEvent, avatarPath, isAuthorClickable)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class ReplyEventListItem(override val golosEvent: GolosReplyEvent,
                              override val avatarPath: String? = null,
                              override val isAuthorClickable: Boolean,
                              val title: String = "") : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosReplyEvent,
                   isAuthorClickable: Boolean,
                   avatarPath: String? = null,
                   title: String = ""): ReplyEventListItem {

            val list = getCashedItems<ReplyEventListItem>(golosEvent)
            var item = list.find { it.title == title && it.avatarPath == avatarPath && it.isAuthorClickable == isAuthorClickable }
            if (item == null) {
                item = ReplyEventListItem(golosEvent, avatarPath, isAuthorClickable, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class MentionEventListItem(override val golosEvent: GolosMentionEvent,
                                override val isAuthorClickable: Boolean,
                                override val avatarPath: String? = null,
                                val title: String = "") : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosMentionEvent,
                   isAuthorClickable: Boolean,
                   avatarPath: String? = null,
                   title: String = ""): MentionEventListItem {

            val list = getCashedItems<MentionEventListItem>(golosEvent)
            var item = list.find {
                it.title == title
                        && it.avatarPath == avatarPath
                        && it.isAuthorClickable == isAuthorClickable
            }
            if (item == null) {
                item = MentionEventListItem(golosEvent, isAuthorClickable, avatarPath, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class RepostEventListItem(override val golosEvent: GolosRepostEvent,
                               override val isAuthorClickable: Boolean,
                               override val avatarPath: String? = null,
                               val title: String = "") : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosRepostEvent,
                   isAuthorClickable: Boolean,
                   avatarPath: String? = null,
                   title: String = ""): RepostEventListItem {

            val list = getCashedItems<RepostEventListItem>(golosEvent)
            var item = list.find { it.title == title && it.avatarPath == avatarPath && it.isAuthorClickable == isAuthorClickable }
            if (item == null) {
                item = RepostEventListItem(golosEvent, isAuthorClickable, avatarPath, title)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class AwardEventListItem(override val golosEvent: GolosAwardEvent,
                              val actualGolosPowerAward: Float,
                              val title: String = "")
    : EventListItem(golosEvent, null, isAuthorClickable = false) {

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
    : EventListItem(golosEvent, null, isAuthorClickable = false) {

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
                                    override val isAuthorClickable: Boolean,
                                    override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosWitnessVoteEvent,
                   isAuthorClickable: Boolean,
                   avatarPath: String? = null): WitnessVoteEventListItem {

            val list = getCashedItems<WitnessVoteEventListItem>(golosEvent)
            var item = list.find { it.avatarPath == avatarPath && it.isAuthorClickable == isAuthorClickable }
            if (item == null) {
                item = WitnessVoteEventListItem(golosEvent, isAuthorClickable, avatarPath)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}

data class WitnessCancelVoteEventListItem(override val golosEvent: GolosWitnessCancelVoteEvent,
                                          override val isAuthorClickable: Boolean,
                                          override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath) {
    companion object {
        fun create(golosEvent: GolosWitnessCancelVoteEvent,
                   isAuthorClickable: Boolean,
                   avatarPath: String? = null): WitnessCancelVoteEventListItem {

            val list = getCashedItems<WitnessCancelVoteEventListItem>(golosEvent)
            var item = list.find { it.avatarPath == avatarPath && it.isAuthorClickable == isAuthorClickable }
            if (item == null) {
                item = WitnessCancelVoteEventListItem(golosEvent, isAuthorClickable, avatarPath)
                putItemToCash(golosEvent, item)
            }
            return item
        }
    }
}