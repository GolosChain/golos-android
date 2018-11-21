package io.golos.golos.screens.events

import io.golos.golos.notifications.*
import io.golos.golos.repository.services.*
import io.golos.golos.repository.services.model.*

fun GolosEvent.getPermlinkForTitle(): String? {
    return when (this) {
        is GolosVoteEvent -> permlink
        is GolosFlagEvent -> permlink
        is GolosTransferEvent -> null
        is GolosSubscribeEvent -> null
        is GolosUnSubscribeEvent -> null
        is GolosReplyEvent -> parentPermlink
        is GolosRepostEvent -> permlink
        is GolosMentionEvent -> permlink
        is GolosAwardEvent -> permlink
        is GolosCuratorAwardEvent -> permlink
        is GolosMessageEvent -> null
        is GolosWitnessVoteEvent -> null
        is GolosWitnessCancelVoteEvent -> null
    }
}

fun GolosNotification.toEventType(): EventType {
    return when (this) {
        is GolosUpVoteNotification -> EventType.VOTE
        is GolosCommentNotification -> EventType.REPLY
        is GolosTransferNotification -> EventType.TRANSFER
        is GolosDownVoteNotification -> EventType.FLAG
        is GolosSubscribeNotification -> EventType.SUBSCRIBE
        is GolosUnSubscribeNotification -> EventType.UNSUBSCRIBE
        is GolosMentionNotification -> EventType.MENTION
        is GolosWitnessVoteNotification -> EventType.WITNESS_VOTE
        is WitnessCancelVoteGolosNotification -> EventType.WITNESS_CANCEL_VOTE
        is GolosRepostNotification -> EventType.REPOST
        is GolosRewardNotification -> EventType.REWARD
        is GolosCuratorRewardNotification -> EventType.CURATOR_AWARD
        is GolosMessageNotification -> EventType.MESSAGE
    }
}

public inline fun <reified T> T.toSingletoneList():List<T> = listOf(this)