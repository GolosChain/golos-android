package io.golos.golos.screens.events

import android.support.constraint.ConstraintLayout
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.repository.services.GolosEvent
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.utils.*

class EventsListAdapter(notifications: List<EventsListItem>,
                        var clickListener: (GolosEvent) -> Unit = {},
                        var onBottomReach: () -> Unit = {},
                        private val appearanceHandler: NotificationsAndEventsAppearanceMaker = NotificationsAndEventsAppearanceMakerImpl)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal data class NotificationWrapper(val notification: GolosEvent,
                                            val clickListener: (GolosViewHolder) -> Unit,
                                            val appearanceHandler: NotificationsAndEventsAppearanceMaker = NotificationsAndEventsAppearanceMakerImpl)

    private val mHashes = HashMap<EventsListItem, Int>()
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


                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return mHashes[oldValue[oldItemPosition]] == value[newItemPosition].hashCode()
                }

            }).dispatchUpdatesTo(this)
            field.forEach {
                mHashes[it] = it.hashCode()
            }
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
        if (position >= items.lastIndex) onBottomReach()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is EventContainingItem) R.layout.vh_event
        else R.layout.vh_event_date_delimeter
    }

    class EventsListAdapterViewHolder(parent: ViewGroup) : GolosViewHolder(R.layout.vh_event, parent) {
        private val mImage: ImageView = itemView.findViewById(R.id.image_iv)
        private val mText: TextView = itemView.findViewById(R.id.text)
        private val mTitle: TextView = itemView.findViewById(R.id.title)
        private val mSecondaryImage = itemView.findViewById<ImageView>(R.id.secondary_icon)
        private val mGlide = Glide.with(itemView)
        private val mView = itemView.findViewById<ConstraintLayout>(R.id.card)


        init {
            mText.movementMethod = GolosMovementMethod.instance
        }

        internal var state: NotificationWrapper? = null
            set(value) {
                field = value
                if (value == null) return
                val onClickListener = View.OnClickListener { value.clickListener.invoke(this@EventsListAdapterViewHolder) }


                itemView.setOnClickListener(onClickListener)
                itemView.findViewById<ViewGroup>(R.id.card).setOnClickListener(onClickListener)

                val notification = value.notification
                mImage.background = null

                val appearance = value.appearanceHandler.makeAppearance(notification)

                if (appearance.title != null) {
                    mText.maxLines = 2
                    mTitle.setViewVisible()
                    mTitle.text = appearance.title
                    (mText.layoutParams as ConstraintLayout.LayoutParams).topToBottom = mTitle.id
                    (mText.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom = -1
                    (mText.layoutParams as ConstraintLayout.LayoutParams).topToTop = -1
                    mText.requestLayout()
                } else {
                    mText.maxLines = 4
                    mTitle.setViewGone()
                    (mText.layoutParams as ConstraintLayout.LayoutParams).topToTop = mView.id
                    (mText.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom = mView.id
                    (mText.layoutParams as ConstraintLayout.LayoutParams).topToBottom = -1
                    mText.requestLayout()
                }

                mText.text = appearance.body
                if (appearance.showProfileImage) {
                    mSecondaryImage.setViewVisible()
                    if (appearance.profileImage != null) {
                        mGlide
                                .load(ImageUriResolver.resolveImageWithSize(appearance.profileImage, wantedwidth = mImage.height))
                                .apply(RequestOptions().placeholder(R.drawable.ic_person_gray_40dp))
                                .into(mImage)
                    } else {
                        mImage.setImageResource(R.drawable.ic_person_gray_40dp)
                    }
                    mSecondaryImage.setViewVisible()
                    mSecondaryImage.setImageResource(appearance.smallIconId)
                } else {
                    mImage.setImageResource(appearance.bigIconId)
                    mSecondaryImage.setViewGone()
                }
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