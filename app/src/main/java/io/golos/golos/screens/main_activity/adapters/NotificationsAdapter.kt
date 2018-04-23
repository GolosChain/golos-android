package io.golos.golos.screens.main_activity.adapters

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import io.golos.golos.R
import io.golos.golos.repository.model.GolosCommentNotification
import io.golos.golos.repository.model.GolosNotification
import io.golos.golos.repository.model.GolosTransferNotification
import io.golos.golos.repository.model.GolosUpVoteNotification
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.utils.*


internal data class NotificationWrapper(val notification: GolosNotification,
                                        val clickListener: (GolosViewHolder) -> Unit,
                                        val onCancelClickListener: (GolosViewHolder) -> Unit)

class NotificationsAdapter(notifications: List<GolosNotification>,
                           var clickListener: (GolosNotification) -> Unit = {},
                           var cancelListener: (GolosNotification) -> Unit = {}) : RecyclerView.Adapter<NotificationsAdapter.NotificationsViewHolder>() {

    var notification = notifications
        set(value) {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition].id == value[newItemPosition].id
                }

                override fun getOldListSize(): Int {
                    return if (field.isNotEmpty()) 1 else 0
                }

                override fun getNewListSize(): Int {
                    return if (value.isNotEmpty()) 1 else 0
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition] == value[newItemPosition]
                }
            }).dispatchUpdatesTo(this)
            field = value

        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationsViewHolder {
        return NotificationsViewHolder(parent)
    }

    override fun getItemCount(): Int = if (notification.isEmpty()) 0 else 1

    override fun onBindViewHolder(holder: NotificationsViewHolder, position: Int) {
        holder.state = NotificationWrapper(notification[position],
                { clickListener.invoke(notification[it.adapterPosition]) },
                { cancelListener.invoke(notification[it.adapterPosition]) })
    }


    class NotificationsViewHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_notification, parent) {
        private val mImage: ImageView = itemView.findViewById(R.id.image_iv)
        private val mImageS: ImageView = itemView.findViewById(R.id.image_iv_second)
        private val mText: TextView = itemView.findViewById(R.id.text)
        private val mCancelBtn = itemView.findViewById<View>(R.id.cancel_ibtn)
        private val mGlide = Glide.with(itemView)

        init {
            mText.movementMethod = GolosMovementMethod.instance
        }

        internal var state: NotificationWrapper? = null
            set(value) {
                field = value
                if (value == null) return

                mCancelBtn.setOnClickListener { value.onCancelClickListener.invoke(this) }
                mText.setOnClickListener { value.clickListener.invoke(this) }
                mImage.setOnClickListener { value.clickListener.invoke(this) }
                itemView.setOnClickListener { value.clickListener.invoke(this) }

                val notification = value.notification
                mImage.background = null
                when (notification) {
                    is GolosUpVoteNotification -> {
                        val voteNotification = notification.voteNotification

                        if (voteNotification.count > 1) {

                            mImage.setViewGone()
                            mImageS.setViewVisible()

                            mText.text = itemView.resources.getString(R.string.users_voted_on_post,
                                    "$siteUrl${voteNotification.parentUrl}",
                                    voteNotification.count.toString(),
                                    itemView.resources.getQuantityString(R.plurals.times, voteNotification.count)).toHtml()


                        } else if (voteNotification.count == 1) {
                            setAvatar(voteNotification.from.avatar)
                            val text = itemView.resources.getString(R.string.user_voted_on_post,
                                    "<b>${voteNotification.from.name.capitalize()}</b>", "$siteUrl${voteNotification.parentUrl}").toHtml()
                            mText.text = text
                        }
                    }
                    is GolosTransferNotification -> {

                        val transferNotification = notification.transferNotification
                        setAvatar(transferNotification.from.avatar)
                        val text = itemView.resources.getString(R.string.user_transferred_you,
                                "<b>${transferNotification.from.name.capitalize()}</b>", transferNotification.amount).toHtml()
                        mText.text = text
                    }
                    is GolosCommentNotification -> {

                        val commentNotification = notification.commentNotification
                        setAvatar(commentNotification.author.avatar)
                        val textId = if (notification.isCommentToPost()) R.string.user_answered_on_post else R.string.user_answered_on_comment
                        val text = itemView.resources.getString(textId,
                                "<b>${commentNotification.author.name.capitalize()}</b>",
                                "$siteUrl${commentNotification.parentUrl}").toHtml()

                        mText.text = text

                    }
                }
            }

        private fun setAvatar(path: String?) {
            mImage.setViewVisible()
            mImageS.setViewGone()
            if (path != null) {
                mImage.scaleType = ImageView.ScaleType.CENTER_CROP
                mGlide
                        .load(ImageUriResolver.resolveImageWithSize(
                                path,
                                wantedwidth = mImage.width))
                        .error(mGlide.load(itemView.getVectorDrawable(R.drawable.ic_person_gray_32dp)))
                        .into(mImage)
            } else mImage.setImageDrawable(itemView.getVectorDrawable(R.drawable.ic_person_gray_32dp))
        }
    }
}