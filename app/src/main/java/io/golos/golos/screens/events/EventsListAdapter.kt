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

class EventsListAdapter(notifications: List<EventsListItem>,
                        var clickListener: (GolosEvent) -> Unit = {},
                        private val appearanceHandler: NotificationsAndEventsAppearanceMaker = NotificationsAndEventsAppearanceMakerImpl)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal data class NotificationWrapper(val notification: GolosEvent,
                                            val clickListener: (GolosViewHolder) -> Unit,
                                            val appearanceHandler: NotificationsAndEventsAppearanceMaker = NotificationsAndEventsAppearanceMakerImpl)

    var items = notifications
        set(value) {

            val oldValue = field
            field = value
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

                    return oldValue[oldItemPosition] == value[newItemPosition]
                }


                override fun getOldListSize() =
                        oldValue.size


                override fun getNewListSize() =
                        value.size


                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                        oldValue[oldItemPosition] == value[newItemPosition]
            }).dispatchUpdatesTo(this)
        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.vh_event_date_delimeter -> EventDateViewHolder(parent)
            R.layout.vh_event -> EventsListAdapterViewHolder(parent)
            else -> throw IllegalStateException("unknown type")
        }
    }

    override fun getItemCount() = items.size


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (item is EventContainingItem) {
            (holder as EventsListAdapterViewHolder).state = NotificationWrapper(item.event,
                    { clickListener.invoke((items[it.adapterPosition] as EventContainingItem).event) })
        } else if (item is DateMarkContainingItem) {
            (holder as EventDateViewHolder).dateString = item.date
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is EventContainingItem) R.layout.vh_event
        else R.layout.vh_event_date_delimeter
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

    class EventDateViewHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_event_date_delimeter, parent) {
        private val mDateText = itemView.findViewById<TextView>(R.id.text)
        var dateString: String = ""
            set(value) {
                field = value
                mDateText.text = value
            }
    }
}