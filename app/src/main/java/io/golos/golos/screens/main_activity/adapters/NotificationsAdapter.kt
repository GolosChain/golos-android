package io.golos.golos.screens.main_activity.adapters

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
import io.golos.golos.notifications.GolosNotification
import io.golos.golos.utils.NotificationsAndEventsAppearanceMaker
import io.golos.golos.utils.NotificationsAndEventsAppearanceMakerImpl
import io.golos.golos.repository.Repository
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.utils.GolosMovementMethod
import io.golos.golos.utils.setViewGone
import io.golos.golos.utils.setViewVisible


class NotificationsAdapter(notifications: List<GolosNotification>,
                           var clickListener: (GolosNotification) -> Unit = {},
                           var cancelListener: (GolosNotification) -> Unit = {},
                           private val showOnlyFirst: Boolean,
                           private val appearanceHandler: NotificationsAndEventsAppearanceMaker = NotificationsAndEventsAppearanceMakerImpl) : RecyclerView.Adapter<NotificationsAdapter.NotificationsViewHolder>() {

    internal data class NotificationWrapper(val notification: GolosNotification,
                                            val clickListener: (GolosViewHolder) -> Unit,
                                            val onCancelClickListener: (GolosViewHolder) -> Unit,
                                            val appearanceHandler: NotificationsAndEventsAppearanceMaker = NotificationsAndEventsAppearanceMakerImpl)

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

                val appearance = value.appearanceHandler.makeAppearance(notification)
                mSecondaryImage.setViewGone()

                if (appearance.title != null) {
                    mText.maxLines = 2
                    mTitle.setViewVisible()
                    mTextLinearLo.gravity = Gravity.TOP
                    mTextLinearLo.setPadding(0, 0, 0, 0)
                    mTitle.text = appearance.title
                } else {
                    mText.maxLines = 2
                    mTitle.setViewGone()
                    mTextLinearLo.gravity = Gravity.CENTER_VERTICAL
                    mTextLinearLo.setPadding(0, 0, 0, 0)
                }
                mText.text = appearance.body
                mImage.setImageResource(appearance.iconId)

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