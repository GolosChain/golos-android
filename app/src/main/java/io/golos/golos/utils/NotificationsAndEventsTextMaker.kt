package io.golos.golos.utils

import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.notifications.*
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.ExchangeValues
import io.golos.golos.repository.services.model.*
import io.golos.golos.screens.editor.knife.KnifeURLSpan
import io.golos.golos.screens.events.*
import java.util.*

data class NotificationAppearance(val title: CharSequence? = null,
                                  val body: CharSequence,
                                  @DrawableRes val iconId: Int) {
}

data class EventAppearance(val title: CharSequence?,
                           val body: CharSequence,
                           val profileImage: String?,
                           val showProfileImage: Boolean,
                           @DrawableRes val bigIconId: Int,
                           @DrawableRes val smallIconId: Int)

interface NotificationsAndEventsAppearanceMaker {
    fun makeAppearance(golosNotification: GolosNotification,
                       currentUserName: String = Repository.get.appUserData.value?.name.orEmpty(),
                       title: String = ""): NotificationAppearance

    fun makeAppearance(eventListItem: EventListItem,
                       currentUserName: String = Repository.get.appUserData.value?.name.orEmpty()): EventAppearance
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


    override fun makeAppearance(eventListItem: EventListItem,
                                currentUserName: String): EventAppearance {
        val golosEvent = eventListItem.golosEvent

        val eventApp = makeAppearance(when (golosEvent) {
            is GolosVoteEvent ->
                GolosUpVoteNotification(golosEvent.permlink, golosEvent.fromUsers.first(), golosEvent.counter)
            is GolosFlagEvent -> GolosDownVoteNotification(golosEvent.permlink, golosEvent.fromUsers.first(), golosEvent.counter)
            is GolosTransferEvent -> GolosTransferNotification(golosEvent.fromUsers.first(), golosEvent.amount, golosEvent.counter)
            is GolosReplyEvent -> GolosCommentNotification(golosEvent.permlink, golosEvent.fromUsers.first(), golosEvent.counter)
            is GolosSubscribeEvent -> GolosSubscribeNotification(golosEvent.fromUsers.first(), golosEvent.counter)
            is GolosUnSubscribeEvent -> GolosUnSubscribeNotification(golosEvent.fromUsers.first(), golosEvent.counter)
            is GolosMentionEvent -> GolosMentionNotification(golosEvent.permlink, golosEvent.fromUsers.first(), golosEvent.counter)
            is GolosRepostEvent -> GolosRepostNotification(golosEvent.fromUsers.first(), golosEvent.permlink, golosEvent.counter)
            is GolosWitnessVoteEvent -> GolosWitnessVoteNotification(golosEvent.fromUsers.first(), golosEvent.counter)
            is GolosWitnessCancelVoteEvent -> GolosWitnessVoteNotification(golosEvent.fromUsers.first(), golosEvent.counter)
            is GolosAwardEvent -> GolosRewardNotification(golosEvent.permlink, golosEvent.award.golos, golosEvent.award.golosPower, golosEvent.award.gbg, golosEvent.counter)
            is GolosCuratorAwardEvent -> GolosCuratorRewardNotification(golosEvent.author, golosEvent.permlink, golosEvent.awardInVShares, golosEvent.counter)//unsupported

            is GolosMessageEvent -> GolosWitnessVoteNotification("", golosEvent.counter)//unsupported
        }, currentUserName, when (eventListItem) {
            is VoteEventListItem -> eventListItem.title
            is FlagEventListItem -> eventListItem.title
            is TransferEventListItem -> ""
            is SubscribeEventListItem -> ""
            is UnSubscribeEventListItem -> ""
            is ReplyEventListItem -> eventListItem.title
            is MentionEventListItem -> eventListItem.title
            is RepostEventListItem -> eventListItem.title
            is AwardEventListItem -> eventListItem.title
            is CuratorAwardEventListItem -> eventListItem.title
            is MessageEventListItem -> ""
            is WitnessVoteEventListItem -> ""
            is WitnessCancelVoteEventListItem -> ""
        })

        val needToShowAvatar = golosEvent is Authorable && golosEvent.counter == 1
        val smallIconId = when (golosEvent) {
            is GolosVoteEvent -> R.drawable.ic_liked_events_20dp
            is GolosFlagEvent -> R.drawable.ic_dizliked_events_20dp//4469AF
            is GolosTransferEvent -> R.drawable.ic_coins_20dp
            is GolosReplyEvent -> R.drawable.ic_comment_20dp
            is GolosSubscribeEvent -> R.drawable.ic_subscribe_20dp
            is GolosUnSubscribeEvent -> R.drawable.ic_unsubscribe_20dp
            is GolosMentionEvent -> R.drawable.ic_avatar_20dp
            is GolosRepostEvent -> R.drawable.ic_repost_gr_20dp
            is GolosWitnessVoteEvent -> R.drawable.ic_delegat_vote_20dp
            is GolosWitnessCancelVoteEvent -> R.drawable.ic_no_delegat_20dp

            is GolosAwardEvent -> R.drawable.ic_award_20dp //unsupported
            is GolosCuratorAwardEvent -> R.drawable.ic_avatar_20dp//unsupported
            is GolosMessageEvent -> R.drawable.ic_avatar_20dp//unsupported
        }
        return EventAppearance(eventApp.title, eventApp.body, eventListItem.avatarPath, needToShowAvatar, eventApp.iconId, smallIconId)
    }

    fun CharSequence.setLinkSpan(forString: String): CharSequence {
        val context = App.context
        val color = ContextCompat.getColor(context, R.color.blue_light)
        val index = lastIndexOf(forString)
        if (forString.isNotEmpty() && index > -1) {
            val ssb = SpannableStringBuilder.valueOf(this)
            ssb.setSpan(KnifeURLSpan("", color, true), index,
                    index + forString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return ssb
        }
        return this
    }


    override fun makeAppearance(golosNotification: GolosNotification,
                                currentUserName: String,
                                title: String): NotificationAppearance {

        val numOfSameNotifications = golosNotification.numberOfSameType
        val numOfAdditionalNotifications = numOfSameNotifications - 1
        val context = App.context

        return when (golosNotification) {
            is GolosMessageNotification -> {
                //unsupported
                NotificationAppearance(title = "", body = ""
                        , iconId = R.drawable.ic_like_40dp_white_on_blue)
            }
            is GolosUpVoteNotification -> {
                val text = if (numOfSameNotifications == 1)
                    context.getString(R.string.userr_voted, golosNotification.fromUser, title, mEmojisMap[R.string.userr_voted])
                else context.getString(R.string.userr_voted_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.other, numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()), title,
                        mEmojisMap[R.string.userr_voted_several])

                NotificationAppearance(title = null, body = text.setLinkSpan(title)
                        , iconId = R.drawable.ic_like_40dp_white_on_blue)
            }
            is GolosDownVoteNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_downvoted,
                        golosNotification.fromUser, title, mEmojisMap[R.string.user_downvoted])
                else context.getString(R.string.user_downvoted_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.negative_reacted, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()),
                        title, mEmojisMap[R.string.user_downvoted_several])

                NotificationAppearance(title = null, body = text.setLinkSpan(title), iconId = R.drawable.ic_dizlike_40dp_white_on_blue)
            }
            is GolosTransferNotification -> {
                val title = context.getString(R.string.transfer_title)
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_transfered,
                        golosNotification.fromUser, golosNotification.amount)
                        .plus(" ${mEmojisMap[R.string.user_transfered]}")
                else context.getString(R.string.user_transfered_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.transfered, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()),
                        golosNotification.amount)
                        .plus(" ${mEmojisMap[R.string.user_transfered_several]}")

                NotificationAppearance(title = title, body = text, iconId = R.drawable.ic_thank_w)
            }
            is GolosCommentNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_replied,
                        golosNotification.fromUser,
                        title, mEmojisMap[R.string.user_replied])
                else context.getString(R.string.user_replied_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.answered, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()),
                        title, mEmojisMap[R.string.user_replied_several])

                NotificationAppearance(title = currentUserName.capitalize(),
                        body = text.setLinkSpan(title)
                        , iconId = R.drawable.ic_message_w_40dp)
            }
            is GolosSubscribeNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_subscribed, golosNotification.fromUser)
                        .plus(" ${mEmojisMap[R.string.user_subscribed]}")
                else context.getString(R.string.user_subscribed_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.subscribed, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_subscribed_several]}")
                NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_subscribe_w_40dp)
            }
            is GolosUnSubscribeNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_unsubscribed, golosNotification.fromUser)
                        .plus(" ${mEmojisMap[R.string.user_unsubscribed]}")
                else context.getString(R.string.user_unsubscribed_several,
                        golosNotification.fromUser,
                        context.getQuantityString(R.plurals.unsubscribed, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_unsubscribed_several]}")
                NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_unsubscribe_w_40dp)
            }
            is GolosMentionNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_mentioned,
                        golosNotification.fromUser,
                        title,
                        mEmojisMap[R.string.user_mentioned])
                else context.getString(R.string.user_mentioned_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.mentioned,
                                numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_mentioned_several]}")
                NotificationAppearance(title = null, body = text.setLinkSpan(title), iconId = R.drawable.ic_mention_w_40dp)
            }
            is GolosRepostNotification -> {

                val text = if (numOfSameNotifications == 1)
                    context.getString(R.string.user_reposted, golosNotification.fromUser, title, mEmojisMap[R.string.user_reposted])
                else context.getString(R.string.user_reposted_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.made_repost,
                                numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_reposted_several]}")

                NotificationAppearance(title = null, body = text.setLinkSpan(title), iconId = R.drawable.ic_repost_w_40dp)
            }
            is GolosWitnessVoteNotification -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_voted_for_you, golosNotification.fromUser)
                        .plus(" ${mEmojisMap[R.string.user_voted_for_you]}")
                else context.getString(R.string.user_voted_for_you_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.voted_for_witness,
                                numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()))
                        .plus(" ${mEmojisMap[R.string.user_voted_for_you_several]}")

                NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_witnessvote_w_40dp)
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
                NotificationAppearance(title = null, body = text, iconId = R.drawable.ic_witnesscancelvote_w_40dp)
            }

            is GolosRewardNotification -> {
                val gpModifier = (Repository.get.getExchangeLiveData().value
                        ?: ExchangeValues.nullValues).vSharesToGolosPowerMultiplier

                val rewardString = "".let { if (golosNotification.gbgAward != 0.0) it + "${golosNotification.gbgAward} GBG, " else it }
                        .let { if (golosNotification.golosAward != 0.0) it + "${golosNotification.golosAward} GOLOS, " else it }
                        .let { if (golosNotification.golosPowerAward != 0.0) it + "${String.format(Locale.US, "%.3f", golosNotification.golosPowerAward * gpModifier)} ${context.getString(R.string.golos_power_for_events)} " else it }
                val text = if (numOfSameNotifications == 1) context.getString(R.string.reward_for_entry,
                        rewardString, title)
                else context.getString(R.string.reward_for_entries, rewardString)

                NotificationAppearance(title = null, body = text.setLinkSpan(title), iconId = R.drawable.ic_award_40dp)
            }

            is GolosCuratorRewardNotification -> {
                val gpModifier = (Repository.get.getExchangeLiveData().value
                        ?: ExchangeValues.nullValues).vSharesToGolosPowerMultiplier

                val rewardString = "${String.format(Locale.US, "%.3f", golosNotification.golosPowerReward * gpModifier)} ${context.getString(R.string.golos_power_for_events)}"

                val text = if (numOfSameNotifications == 1) context.getString(R.string.curator_reward_for_entry,
                        rewardString, title)
                else context.getString(R.string.curator_reward_for_entries, rewardString)

                NotificationAppearance(title = null, body = text.setLinkSpan(title), iconId = R.drawable.ic_curator_award_40dp)
            }
        }
    }
}



