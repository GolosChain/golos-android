package io.golos.golos.screens.events

import io.golos.golos.repository.services.*
import io.golos.golos.screens.story.model.SubscribeStatus

sealed class EventListItem(open val golosEvent: GolosEvent, open val avatarPath: String?)


data class VoteEventListItem(override val golosEvent: GolosVoteEvent,
                             override val avatarPath: String? = null,
                             val title: String = "") : EventListItem(golosEvent, avatarPath)

data class FlagEventListItem(override val golosEvent: GolosFlagEvent,
                             override val avatarPath: String? = null,
                             val title: String = "") : EventListItem(golosEvent, avatarPath)

data class TransferEventListItem(override val golosEvent: GolosTransferEvent,
                                 override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath)

data class SubscribeEventListItem(override val golosEvent: GolosSubscribeEvent,
                                  override val avatarPath: String? = null,
                                  val authorSubscriptionState: SubscribeStatus = SubscribeStatus.UnsubscribedStatus,
                                  val showSubscribeButton: Boolean = false) : EventListItem(golosEvent, avatarPath)

data class UnSubscribeEventListItem(override val golosEvent: GolosUnSubscribeEvent,
                                    override val avatarPath: String? = null)
    : EventListItem(golosEvent, avatarPath)

data class ReplyEventListItem(override val golosEvent: GolosReplyEvent,
                              override val avatarPath: String? = null,
                              val title: String = "") : EventListItem(golosEvent, avatarPath)

data class MentionEventListItem(override val golosEvent: GolosMentionEvent,
                                override val avatarPath: String? = null,
                                val title: String = "") : EventListItem(golosEvent, avatarPath)

data class RepostEventListItem(override val golosEvent: GolosRepostEvent,
                               override val avatarPath: String? = null,
                               val title: String = "") : EventListItem(golosEvent, avatarPath)

data class AwardEventListItem(override val golosEvent: GolosAwardEvent,
                              val actualGolosPowerAward: Float,
                              val title: String = "")
    : EventListItem(golosEvent, null)

data class CuratorAwardEventListItem(override val golosEvent: GolosCuratorAwardEvent,
                                     val actualGolosPowerAward: Float,
                                     val title: String = "")
    : EventListItem(golosEvent, null)

data class MessageEventListItem(override val golosEvent: GolosMessageEvent,
                                override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath)

data class WitnessVoteEventListItem(override val golosEvent: GolosWitnessVoteEvent,
                                    override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath)

data class WitnessCancelVoteEventListItem(override val golosEvent: GolosWitnessCancelVoteEvent,
                                          override val avatarPath: String? = null) : EventListItem(golosEvent, avatarPath)