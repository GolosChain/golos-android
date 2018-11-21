package io.golos.golos.repository.model

/**
 * Created by yuri yurivladdurain@gmail.com on 14/11/2018.
 */
data class GolosNotificationSettings(val showUpvoteNotifs: Boolean,
                                     val showFlagNotifs: Boolean,
                                     val showNewCommentNotifs: Boolean,
                                     val showTransferNotifs: Boolean,
                                     val showSubscribeNotifs: Boolean,
                                     val showUnSubscribeNotifs: Boolean,
                                     val showMentions: Boolean,
                                     val showReblog: Boolean,
                                     val showMessageNotifs: Boolean,
                                     val showWitnessVote: Boolean,
                                     val showWitnessCancelVote: Boolean,
                                     val showAward: Boolean,
                                     val showCurationAward: Boolean)