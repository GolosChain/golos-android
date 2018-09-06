package io.golos.golos.screens.events

import io.golos.golos.repository.services.*

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