package io.golos.golos.utils

import android.os.Build
import android.support.annotation.DrawableRes
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.notifications.*
import io.golos.golos.repository.Repository

data class NotificationAppearance(val title: CharSequence? = null,
                                  val body: CharSequence,
                                  val profileImage: String? = null,
                                  @DrawableRes val iconId: Int)

interface NotificationsAndEventsAppearanceMaker {
    fun makeAppearance(golosNotification: GolosNotification,
                       currentUserName: String = Repository.get.appUserData.value?.userName.orEmpty()): NotificationAppearance
}

object NotificationsAndEventsAppearanceMakerImpl : NotificationsAndEventsAppearanceMaker {
    private val mEmojisMap = HashMap<Int, String>().apply {
        put(R.string.userr_voted, "\uD83D\uDC4D")
        put(R.string.userr_voted_several, "\uD83D\uDC4D")
        put(R.string.user_reposted, "\uD83D\uDE0E")
        put(R.string.user_reposted_several, "\uD83D\uDE0E")
        put(R.string.user_downvoted, "\uD83D\uDE35")
        put(R.string.user_downvoted_several, "\uD83D\uDE35")
        put(R.string.user_transfered, "\uD83D\uDCB8")
        put(R.string.user_transfered_several, "\uD83D\uDCB8")
        put(R.string.user_replied, "✌")
        put(R.string.user_replied_several, "✌")
        put(R.string.user_subscribed, "\uD83D\uDE0A")
        put(R.string.user_subscribed_several, "\uD83D\uDE0A")
        put(R.string.user_unsubscribed, "\uD83D\uDE14")
        put(R.string.user_unsubscribed_several, "\uD83D\uDE14")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            put(R.string.user_mentioned, "\uD83E\uDD14")
            put(R.string.user_mentioned_several, "\uD83E\uDD14")
        } else {
            put(R.string.user_mentioned, "\uD83D\uDE0F")
            put(R.string.user_mentioned_several, "\uD83D\uDE0F")

        }

        put(R.string.user_voted_for_you, "\uD83D\uDD25")
        put(R.string.user_voted_for_you_several, "\uD83D\uDD25")
        put(R.string.user_canceled_vote_for_you, "\uD83D\uDE48")
        put(R.string.user_canceled_vote_for_you_several, "\uD83D\uDE48")

    }

    override fun makeAppearance(golosNotification: GolosNotification,
                                currentUserName: String): NotificationAppearance {
        val context = App.context
        val numOfSameNotifications = golosNotification.numberOfSameType
        val numOfAdditionalNotifications = numOfSameNotifications - 1



        return when (golosNotification) {
            is GolosUpVoteNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.userr_voted, golosNotification.fromUser)
                        .plus(" ${mEmojisMap[R.string.userr_voted]}")
                else context.getString(R.string.userr_voted_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.other, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.userr_voted_several]}")
                return NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_like_40dp_white_on_blue)
            }
            is GolosDownVoteNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_downvoted, golosNotification.fromUser)
                        .plus(" ${mEmojisMap[R.string.user_downvoted]}")
                else context.getString(R.string.user_downvoted_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.negative_reacted, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_downvoted_several]}")
                return NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_downvote_white_on_blue_40dp)
            }
            is GolosTransferNotification -> {
                val title = context.getString(R.string.transfer_title)
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_transfered, golosNotification.fromUser, golosNotification.amount)
                        .plus(" ${mEmojisMap[R.string.user_transfered]}")
                else context.getString(R.string.user_transfered_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.transfered, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()),
                        golosNotification.amount)
                        .plus(" ${mEmojisMap[R.string.user_transfered_several]}")

                return NotificationAppearance(title = title, body = text, iconId = R.drawable.ic_thank_w)
            }
            is GolosCommentNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_replied, golosNotification.fromUser)
                        .plus(" ${mEmojisMap[R.string.user_replied]}")
                else context.getString(R.string.user_replied_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.answered, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_replied_several]}")
                return NotificationAppearance(title = currentUserName.capitalize(),
                        body = text
                        , iconId = R.drawable.ic_message_w_40dp)
            }
            is GolosSubscribeNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_subscribed, golosNotification.fromUser)
                        .plus(" ${mEmojisMap[R.string.user_subscribed]}")
                else context.getString(R.string.user_subscribed_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.subscribed, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_subscribed_several]}")
                return NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_subscribe_w_40dp)
            }
            is GolosUnSubscribeNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_unsubscribed, golosNotification.fromUser)
                        .plus(" ${mEmojisMap[R.string.user_unsubscribed]}")
                else context.getString(R.string.user_unsubscribed_several,
                        golosNotification.fromUser,
                        context.getQuantityString(R.plurals.unsubscribed, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_unsubscribed_several]}")
                return NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_unsubscribe_w_40dp)
            }
            is GolosMentionNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_mentioned, golosNotification.fromUser)
                        .plus(" ${mEmojisMap[R.string.user_mentioned]}")
                else context.getString(R.string.user_mentioned_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.mentioned,
                                numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_mentioned_several]}")
                return NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_mention_w_40dp)
            }
            is GolosRepostNotification -> {

                val text = if (numOfSameNotifications == 1)
                    context.getString(R.string.user_reposted, golosNotification.fromUser)
                            .plus(" ${mEmojisMap[R.string.user_reposted]}")
                else context.getString(R.string.user_reposted_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.made_repost,
                                numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_reposted_several]}")

                return NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_repost_w_40dp)
            }
            is GolosWitnessVoteNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_voted_for_you, golosNotification.fromUser)
                        .plus(" ${mEmojisMap[R.string.user_voted_for_you]}")
                else context.getString(R.string.user_voted_for_you_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.voted_for_witness,
                                numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_voted_for_you_several]}")

                return NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_witnessvote_w_40dp)
            }
            is WitnessCancelVoteGolosNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_canceled_vote_for_you,
                        golosNotification.fromUser)
                        .plus(" ${mEmojisMap[R.string.user_canceled_vote_for_you]}")
                else context.getString(R.string.user_canceled_vote_for_you_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.canceled_vote_for_witness,
                                numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_canceled_vote_for_you_several]}")
                return NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_witnesscancelvote_w_40dp)
            }
        }
    }
}



