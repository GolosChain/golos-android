package io.golos.golos.notifications

import android.support.annotation.DrawableRes
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.utils.getQuantityString
import io.golos.golos.utils.isCommentToPost
import io.golos.golos.utils.toHtml

data class NotificationAppearance(val title: CharSequence? = null,
                                  val body: CharSequence,
                                  val iconUrl: String)

interface NotificationAppearanceManager {
    fun makeAppearance(golosNotification: GolosNotification, currentUserName: String = Repository.get.appUserData.value?.userName.orEmpty()): NotificationAppearance
}

object NotificationAppearanceManagerImpl : NotificationAppearanceManager {
    override fun makeAppearance(golosNotification: GolosNotification,
                                currentUserName: String): NotificationAppearance {
        val context = App.context
        val numOfSameNotifications = golosNotification.numberOfSameType
        val numOfAdditionalNotifications = numOfSameNotifications - 1


        return when (golosNotification) {
            is GolosUpVoteNotification -> {
                val voteNotification = golosNotification.voteNotification
                if (voteNotification.count == 1) {
                    val textId = if (isCommentToPost(golosNotification)) R.string.user_voted_on_post
                    else R.string.user_voted_on_comment

                    val text = context.getString(textId, voteNotification.from.name.capitalize()).toHtml()

                    return NotificationAppearance(body = text,
                            iconUrl = voteNotification.from.avatar
                                    ?: pathFromDrawable(R.drawable.ic_person_gray_32dp))
                } else {
                    val textId = if (isCommentToPost(golosNotification)) R.string.users_voted_on_post
                    else R.string.users_voted_on_comment

                    return NotificationAppearance(body = context.getString(textId, voteNotification.count.toString(), context.resources.getQuantityString(R.plurals.times, voteNotification.count)).toHtml(),
                            iconUrl = pathFromDrawable(R.drawable.ic_like_40dp_white_on_blue))

                }
            }
            is GolosDownVoteNotification -> {

                val voteNotification = golosNotification.voteNotification

                if (voteNotification.count == 1) {
                    val textId = if (isCommentToPost(golosNotification)) R.string.user_downvoted_on_post
                    else R.string.user_downvoted_on_comment

                    val text = context.getString(textId, voteNotification.from.name.capitalize()).toHtml()

                    return NotificationAppearance(voteNotification.from.name.capitalize(),
                            text,
                            voteNotification.from.avatar
                                    ?: pathFromDrawable(R.drawable.ic_person_gray_32dp))
                } else {
                    val textId = if (isCommentToPost(golosNotification)) R.string.users_downvoted_on_post
                    else R.string.users_downvoted_on_comment

                    val text = context.getString(textId, Math.abs(voteNotification.count).toString(),
                            context.resources.getQuantityString(R.plurals.plural_for_downvoted, Math.abs(voteNotification.count))).toHtml()

                    return NotificationAppearance(body = text, iconUrl = pathFromDrawable(R.drawable.ic_downvote_white_on_blue_40dp))
                }

            }
            is GolosTransferNotification -> {

                val transferNotification = golosNotification.transferNotification
                val text = context.getString(R.string.user_transferred_you, transferNotification.amount)

                return NotificationAppearance(context.getString(R.string.transfer_income, transferNotification.from.name.capitalize()).toHtml(),
                        text,
                        transferNotification.from.avatar
                                ?: pathFromDrawable((R.drawable.ic_person_gray_32dp)))
            }
            is GolosCommentNotification -> {

                val commentNotification = golosNotification.commentNotification
                val textId = if (isCommentToPost(golosNotification)) R.string.user_answered_on_post_for_notification
                else R.string.user_answered_on_comment_for_notification
                val text = context.getString(textId, commentNotification.author.name.capitalize()).toHtml()

                return NotificationAppearance(commentNotification.parentAuthor.capitalize(), text, commentNotification.author.avatar
                        ?: pathFromDrawable(R.drawable.ic_person_gray_32dp))
            }
            is GolosUpVoteNotificationNew -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.userr_voted, golosNotification.fromUser)
                else context.getString(R.string.userr_voted_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.other, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))
                return NotificationAppearance(title = null, body = text, iconUrl = pathFromDrawable(R.drawable.ic_like_40dp_white_on_blue))
            }
            is GolosDownVoteNotificationNew -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_downvoted, golosNotification.fromUser)
                else context.getString(R.string.user_downvoted_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.negative_reacted, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))
                return NotificationAppearance(title = null, body = text, iconUrl = pathFromDrawable(R.drawable.ic_downvote_white_on_blue_40dp))
            }
            is GolosTransferNotificationNew -> {
                val title = context.getString(R.string.transfer_title)
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_transfered, golosNotification.fromUser, golosNotification.amount)
                else context.getString(R.string.user_transfered_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.transfered, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()),
                        golosNotification.amount)

                return NotificationAppearance(title = title, body = text, iconUrl = pathFromDrawable(R.drawable.ic_thank_w))
            }
            is GolosCommentNotificationNew -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_replied, golosNotification.fromUser)
                else context.getString(R.string.user_replied_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.answered, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))

                return NotificationAppearance(title = currentUserName.capitalize(),
                        body = text
                        , iconUrl = pathFromDrawable(R.drawable.ic_message_w_40dp))
            }
            is GolosSubscribeNotificationNew -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_subscribed, golosNotification.fromUser)
                else context.getString(R.string.user_subscribed_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.subscribed, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))

                return NotificationAppearance(title = null, body = text, iconUrl = pathFromDrawable(R.drawable.ic_subscribe_w_40dp))
            }
            is GolosUnSubscribeNotificationNew -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_unsubscribed, golosNotification.fromUser)
                else context.getString(R.string.user_unsubscribed_several,
                        golosNotification.fromUser,
                        context.getQuantityString(R.plurals.unsubscribed, numOfAdditionalNotifications, numOfAdditionalNotifications.toString()))

                return NotificationAppearance(title = null, body = text, iconUrl = pathFromDrawable(R.drawable.ic_unsubscribe_w_40dp))
            }
            is GolosMentionNotificationNew -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_mentioned, golosNotification.fromUser)
                else context.getString(R.string.user_mentioned_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.mentioned,
                                numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()))
                return NotificationAppearance(title = null, body = text, iconUrl = pathFromDrawable(R.drawable.ic_mention_w_40dp))
            }
            is GolosRepostNotificationNew -> {

                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_reposted, golosNotification.fromUser)
                else context.getString(R.string.user_reposted_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.made_repost,
                                numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()))

                return NotificationAppearance(title = null, body = text, iconUrl = pathFromDrawable(R.drawable.ic_repost_w_40dp))
            }
            is GolosWitnessVoteNotificationNew -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_voted_for_you, golosNotification.fromUser)
                else context.getString(R.string.user_voted_for_you_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.voted_for_witness,
                                numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()))

                return NotificationAppearance(title = null, body = text, iconUrl = pathFromDrawable(R.drawable.ic_witnessvote_w_40dp))
            }
            is WitnessCancelVoteGolosNotificationNew -> {
                val text = if (numOfSameNotifications == 1) context.getString(R.string.user_canceled_vote_for_you,
                        golosNotification.fromUser)
                else context.getString(R.string.user_canceled_vote_for_you_several, golosNotification.fromUser,
                        context.getQuantityString(R.plurals.canceled_vote_for_witness,
                                numOfAdditionalNotifications,
                                numOfAdditionalNotifications.toString()))
                return NotificationAppearance(title = null, body = text, iconUrl = pathFromDrawable(R.drawable.ic_witnesscancelvote_w_40dp))
            }
        }
    }

    private fun pathFromDrawable(@DrawableRes drawableRes: Int) = "android.resource://${App.context.packageName}/$drawableRes"
}



