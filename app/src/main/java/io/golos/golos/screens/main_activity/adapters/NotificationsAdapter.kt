package io.golos.golos.screens.main_activity.adapters

import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.*
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.utils.*


internal data class NotificationWrapper(val notification: GolosNotification,
                                        val clickListener: (GolosViewHolder) -> Unit,
                                        val onCancelClickListener: (GolosViewHolder) -> Unit)

class NotificationsAdapter(notifications: List<GolosNotification>,
                           var clickListener: (GolosNotification) -> Unit = {},
                           var cancelListener: (GolosNotification) -> Unit = {},
                           private val showOnlyFirst: Boolean) : RecyclerView.Adapter<NotificationsAdapter.NotificationsViewHolder>() {

    var notification = notifications
        set(value) {
            if (showOnlyFirst) {
                field = value
                notifyDataSetChanged()
            } else {
                val oldValue = field
                field = value

                DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val theSame = oldValue[oldItemPosition] == value[newItemPosition]

                        return theSame
                    }


                    override fun getOldListSize() =
                            oldValue.size


                    override fun getNewListSize() =
                            value.size


                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                            oldValue[oldItemPosition] == value[newItemPosition]
                }).dispatchUpdatesTo(this)

            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationsViewHolder {
        return NotificationsViewHolder(parent)
    }

    override fun getItemCount(): Int =
            if (showOnlyFirst)
                if (notification.isEmpty()) 0 else 1
            else notification.size


    override fun onBindViewHolder(holder: NotificationsViewHolder, position: Int) {
        holder.state = NotificationWrapper(notification[position],
                { clickListener.invoke(notification[it.adapterPosition]) },
                { cancelListener.invoke(notification[it.adapterPosition]) })
    }


    class NotificationsViewHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_notification, parent) {
        private val mImage: ImageView = itemView.findViewById(R.id.image_iv)
        private val mSecondaryImage: ImageView = itemView.findViewById(R.id.secondary_icon)
        private val mText: TextView = itemView.findViewById(R.id.text)
        private val mTitle: TextView = itemView.findViewById(R.id.title)
        private val mCancelBtn = itemView.findViewById<View>(R.id.cancel_ibtn)
        private val mTextLinearLo = itemView.findViewById<LinearLayout>(R.id.text_lo)
        private val mGlide = Glide.with(itemView)

        init {
            mText.movementMethod = GolosMovementMethod.instance
        }

        internal var state: NotificationWrapper? = null
            set(value) {
                field = value
                if (value == null) return

                mCancelBtn.setOnClickListener { value.onCancelClickListener.invoke(this) }
                mTextLinearLo.setOnClickListener { value.clickListener.invoke(this) }
                mText.setOnClickListener { value.clickListener.invoke(this) }
                mTitle.setOnClickListener { value.clickListener.invoke(this) }
                mImage.setOnClickListener { value.clickListener.invoke(this) }

                val notification = value.notification
                mImage.background = null
                when (notification) {
                    is GolosUpVoteNotification -> {

                        val voteNotification = notification.voteNotification
                        mText.maxLines = 2
                        mTextLinearLo.setPadding(0, 0, 0, itemView.resources.getDimension(R.dimen.material_big).toInt())
                        if (voteNotification.count > 1) {

                            mTextLinearLo.gravity = Gravity.CENTER_VERTICAL
                            mTitle.setViewGone()
                            mSecondaryImage.setViewGone()

                            mImage.setImageResource(R.drawable.ic_like_40dp_white_on_blue)

                            val textId = if (isCommentToPost(notification)) R.string.users_voted_on_post else R.string.users_voted_on_comment

                            mText.text = itemView.resources.getString(textId,
                                    "$siteUrl${voteNotification.parentUrl}",
                                    voteNotification.count.toString(),
                                    itemView.resources.getQuantityString(R.plurals.times, voteNotification.count)).toHtml()


                        } else if (voteNotification.count == 1) {
                            mTextLinearLo.gravity = Gravity.CENTER_VERTICAL
                            mTitle.setViewGone()
                            setAvatar(voteNotification.from.avatar)
                            val textId = if (isCommentToPost(notification)) R.string.user_voted_on_post else R.string.user_voted_on_comment

                            val text = itemView.resources.getString(textId, voteNotification.from.name.capitalize(),
                                    "$siteUrl${voteNotification.parentUrl}").toHtml()
                            mText.text = text
                        }
                    }
                    is GolosDownVoteNotification -> {
                        mSecondaryImage.setViewGone()
                        val voteNotification = notification.voteNotification
                        mText.maxLines = 2
                        mTextLinearLo.setPadding(0, 0, 0, itemView.resources.getDimension(R.dimen.material_big).toInt())
                        if (voteNotification.count > 1) {

                            mTextLinearLo.gravity = Gravity.CENTER_VERTICAL
                            mTitle.setViewGone()
                            val textId = if (isCommentToPost(notification)) R.string.users_downvoted_on_post else R.string.users_downvoted_on_comment

                            mImage.setImageResource(R.drawable.ic_downvote_white_on_blue_40dp)

                            mText.text = itemView.resources.getString(textId,
                                    "$siteUrl${voteNotification.parentUrl}",
                                    Math.abs(voteNotification.count).toString(),
                                    itemView.resources.getQuantityString(R.plurals.plural_for_downvoted, Math.abs(voteNotification.count))).toHtml()


                        } else if (voteNotification.count == 1) {
                            mTextLinearLo.gravity = Gravity.CENTER_VERTICAL
                            mTitle.setViewGone()

                            setAvatar(voteNotification.from.avatar)

                            val textId = if (isCommentToPost(notification)) R.string.user_downvoted_on_post else R.string.user_downvoted_on_comment

                            val text = itemView.resources.getString(textId,
                                    voteNotification.from.name.capitalize(),
                                    "$siteUrl${voteNotification.parentUrl}").toHtml()
                            mText.text = text
                        }
                    }
                    is GolosTransferNotification -> {

                        val transferNotification = notification.transferNotification
                        mTextLinearLo.gravity = Gravity.TOP
                        val title = itemView.resources.getString(R.string.transfer_income, transferNotification.from.name.capitalize()).toHtml()
                        mTitle.text = title
                        mTitle.setViewVisible()
                        mText.maxLines = 1
                        mTextLinearLo.setPadding(0, 0, 0, 0)

                        mSecondaryImage.background = ContextCompat.getDrawable(itemView.context, R.drawable.shape_notifications_small_icon_back)

                        mSecondaryImage.setImageResource(R.drawable.ic_coins_14dp)
                        mSecondaryImage.setViewVisible()

                        setAvatar(transferNotification.from.avatar)

                        val text = itemView.resources.getString(R.string.user_transferred_you,
                                transferNotification.amount)
                        mText.text = text
                    }
                    is GolosCommentNotification -> {
                        mSecondaryImage.background = ContextCompat.getDrawable(itemView.context, R.drawable.shape_notifications_small_icon_back)
                        mSecondaryImage.setImageResource(if (isCommentToPost(notification)) R.drawable.ic_comment_small else R.drawable.ic_answer_on_comment)
                        mSecondaryImage.setViewVisible()
                        mTextLinearLo.setPadding(0, 0, 0, 0)
                        mTextLinearLo.gravity = Gravity.TOP
                        mTitle.setViewVisible()
                        mTitle.text = notification.commentNotification.parentAuthor.capitalize()
                        mText.maxLines = 1

                        val commentNotification = notification.commentNotification
                        setAvatar(commentNotification.author.avatar)
                        val textId = if (isCommentToPost(notification)) R.string.user_answered_on_post else R.string.user_answered_on_comment
                        val text = itemView.resources.getString(textId,
                                commentNotification.author.name.capitalize(),
                                "$siteUrl${commentNotification.parentUrl}").toHtml()

                        mText.text = text
                    }
                }
            }

        private fun setAvatar(path: String?) {
            mImage.setViewVisible()
            if (path != null) {
                mImage.scaleType = ImageView.ScaleType.CENTER_CROP
                mGlide
                        .load(ImageUriResolver.resolveImageWithSize(
                                path,
                                wantedwidth = mImage.width))
                        .error(mGlide.load(itemView.getVectorDrawable(R.drawable.ic_person_gray_52dp)))
                        .into(mImage)
            } else mImage.setImageDrawable(itemView.getVectorDrawable(R.drawable.ic_person_gray_52dp))
        }
    }
}

class DissmissTouchHelper(adapter: NotificationsAdapter) : ItemTouchHelper(object : ItemTouchHelper.Callback() {
    override fun getMovementFlags(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int {
        return ItemTouchHelper.Callback.makeMovementFlags(0, ItemTouchHelper.END)
    }

    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
        val position = viewHolder?.adapterPosition ?: -1
        val adapter = adapter.notification
        if (position > -1) Repository.get.notificationsRepository.dismissNotification(adapter[position])
    }
})