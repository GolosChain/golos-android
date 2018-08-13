package io.golos.golos.notifications

import android.net.Uri
import android.support.annotation.DrawableRes
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.utils.isCommentToPost
import io.golos.golos.utils.toHtml

data class NotificationAppearance(val title: CharSequence? = null,
                                  val body: CharSequence,
                                  val iconUrl: String)

interface NotificationAppearanceManager {
    fun makeAppearance(golosNotification: GolosNotification): NotificationAppearance
}

object NotificationAppearanceManagerImpl : NotificationAppearanceManager {
    override fun makeAppearance(golosNotification: GolosNotification): NotificationAppearance {
        val context = App.context
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
                val text = context.getString(R.string.userr_voted, golosNotification.fromUser)
                return NotificationAppearance(title = null, body = text, iconUrl = pathFromDrawable(R.drawable.ic_like_40dp_white_on_blue))
            }
            is GolosDownVoteNotificationNew -> {
                val text = context.getString(R.string.user_downvoted, golosNotification.fromUser)
                return NotificationAppearance(title = null, body = text, iconUrl = pathFromDrawable(R.drawable.ic_downvote_white_on_blue_40dp))
            }
            is GolosTransferNotificationNew -> {
                val text = context.getString(R.string.user_transfered, golosNotification.fromUser, String.format("%.0f", golosNotification.amount))
                return NotificationAppearance(title = null, body = text, iconUrl = pathFromDrawable(R.drawable.ic_thank_w))
            }
            is GolosCommentNotificationNew -> {
                val text = context.getString(R.string.user_replied, golosNotification.fromUser)
                return NotificationAppearance(title = null, body = text, iconUrl = "")
            }
            is GolosSubscribeNotificationNew -> {
                val text = context.getString(R.string.user_subscribed, golosNotification.fromUser)
                return NotificationAppearance(title = null, body = text, iconUrl = "")
            }
            is GolosUnSubscribeNotificationNew -> {
                val text = context.getString(R.string.user_unsubscribed, golosNotification.fromUser)
                return NotificationAppearance(title = null, body = text, iconUrl = "")
            }
            is GolosMentionNotificationNew -> {
                val text = context.getString(R.string.user_mentioned, golosNotification.fromUser)
                return NotificationAppearance(title = null, body = text, iconUrl = "")
            }
            is GolosRepostNotificationNew -> {
                val text = context.getString(R.string.user_reposted, golosNotification.fromUser)
                return NotificationAppearance(title = null, body = text, iconUrl = "")
            }
            is GolosWitnessVoteNotificationNew -> {
                val text = context.getString(R.string.user_voted_for_you, golosNotification.fromUser)
                return NotificationAppearance(title = null, body = text, iconUrl = "")
            }
            is WitnessCancelVoteGolosNotificationNew -> {
                val text = context.getString(R.string.user_canceled_vote_for_you, golosNotification.fromUser)
                return NotificationAppearance(title = null, body = text, iconUrl = "")
            }
        }
    }

    private fun pathFromDrawable(@DrawableRes drawableRes: Int) = Uri.parse("android.resource://${App.context.packageName}/$drawableRes").path
}



