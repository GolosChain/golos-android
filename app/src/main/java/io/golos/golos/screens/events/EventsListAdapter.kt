package io.golos.golos.screens.events

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.repository.services.GolosEvent
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.utils.*

class EventsListAdapter(notifications: List<GolosEvent>,
                        var clickListener: (GolosEvent) -> Unit = {},
                        private val appearanceHandler: NotificationsAndEventsAppearanceMaker = NotificationsAndEventsAppearanceMakerImpl)
    : RecyclerView.Adapter<EventsListAdapter.EventsListAdapterViewHolder>() {

    internal data class NotificationWrapper(val notification: GolosEvent,
                                            val clickListener: (GolosViewHolder) -> Unit,
                                            val appearanceHandler: NotificationsAndEventsAppearanceMaker = NotificationsAndEventsAppearanceMakerImpl)

    var notification = notifications
        set(value) {

            val oldValue = field
            field = value
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

                    return oldValue[oldItemPosition].id == value[newItemPosition].id
                }


                override fun getOldListSize() =
                        oldValue.size


                override fun getNewListSize() =
                        value.size


                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                        oldValue[oldItemPosition] == value[newItemPosition]
            }).dispatchUpdatesTo(this)
        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventsListAdapterViewHolder {
        return EventsListAdapterViewHolder(parent)
    }

    override fun getItemCount() = notification.size


    override fun onBindViewHolder(holder: EventsListAdapterViewHolder, position: Int) {
        holder.state = NotificationWrapper(notification[position],
                { clickListener.invoke(notification[it.adapterPosition]) })
    }


    class EventsListAdapterViewHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_event, parent) {
        private val mImage: ImageView = itemView.findViewById(R.id.image_iv)
        private val mText: TextView = itemView.findViewById(R.id.text)
        private val mTitle: TextView = itemView.findViewById(R.id.title)
        private val mTextLinearLo = itemView.findViewById<LinearLayout>(R.id.text_lo)


        init {
            mText.movementMethod = GolosMovementMethod.instance
        }

        internal var state: NotificationWrapper? = null
            set(value) {
                field = value
                if (value == null) return
                val onClickListener = object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        value.clickListener.invoke(this@EventsListAdapterViewHolder)
                    }
                }


                itemView.setOnClickListener(onClickListener)
                itemView.findViewById<ViewGroup>(R.id.card).setOnClickListener(onClickListener)

                val notification = value.notification
                mImage.background = null

                val appearance = value.appearanceHandler.makeAppearance(notification)

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