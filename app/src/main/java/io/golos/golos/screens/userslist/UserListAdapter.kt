package io.golos.golos.screens.userslist

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.screens.userslist.model.UserListRowData
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.utils.ImageUriResolver
import io.golos.golos.utils.UpdatingState
import io.golos.golos.utils.setViewGone
import io.golos.golos.utils.setViewVisible

/**
 * Created by yuri on 22.12.17.
 */
data class UserListItemState(val item: UserListRowData,
                             val onUserClick: (RecyclerView.ViewHolder) -> Unit,
                             val onSubscribeClick: (RecyclerView.ViewHolder) -> Unit)

class UserListAdapter(private var onUserClick: (UserListRowData) -> Unit = { _ -> },
                      private var onSubscribeClick: (UserListRowData) -> Unit = { _ -> }) : RecyclerView.Adapter<UserListViewHolder>() {
    private val mHashes = HashMap<String, Int>()
    var listItems: List<UserListRowData> = ArrayList()
        set(value) {
            if (field.isEmpty()) {
                field = value
                field.forEach {
                    mHashes.put(it.name, it.hashCode())
                }
                notifyDataSetChanged()
            } else {
                DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return field[oldItemPosition].name == value[newItemPosition].name
                    }

                    override fun getOldListSize() = field.size
                    override fun getNewListSize() = value.size
                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val oldItemHash = mHashes[field[oldItemPosition].name]
                        return oldItemHash == value[newItemPosition].hashCode()
                    }
                }).dispatchUpdatesTo(this)
                field = value
                field.forEach {
                    mHashes.put(it.name, it.hashCode())
                }
            }

        }


    fun setUserClickListener(listener: (UserListRowData) -> Unit = { _ -> }) {
        onUserClick = listener
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    override fun onBindViewHolder(holder: UserListViewHolder, position: Int) {
        holder.state = UserListItemState(listItems[position],
                { onUserClick.invoke(listItems[it.adapterPosition]) },
                { onSubscribeClick.invoke(listItems[it.adapterPosition]) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserListViewHolder {
        return UserListViewHolder(parent)
    }
}


class UserListViewHolder(parent: ViewGroup) : GolosViewHolder(R.layout.v_user_list_item, parent) {
    private val mAvatar = itemView.findViewById<ImageView>(R.id.avatar_iv)
    private val mTitleText = itemView.findViewById<TextView>(R.id.name_tv)
    private val mSubTitleTv = itemView.findViewById<TextView>(R.id.sub_tv)
    private val mSubscibeButton = itemView.findViewById<Button>(R.id.follow_btn)
    private val mProgress = itemView.findViewById<View>(R.id.progress)
    private var mLastAvatar: String? = null
    var state: UserListItemState? = null
        set(value) {
            field = value
            if (value == null) {
                mAvatar.setImageDrawable(null)
                mTitleText.text = ""
                mSubscibeButton.text = ""
                mSubscibeButton.setOnClickListener { }
            } else {
                if (value.item.avatar != null) {
                    if (mLastAvatar != value.item.avatar) {
                        Glide
                                .with(itemView)
                                .load(ImageUriResolver.resolveImageWithSize(value.item.avatar
                                        ?: "", wantedwidth = mAvatar.width))
                                .apply(RequestOptions()
                                        .error(R.drawable.ic_person_gray_80dp)
                                        .placeholder(R.drawable.ic_person_gray_80dp))
                                .into(mAvatar)
                    }
                    mLastAvatar = value.item.avatar
                } else {
                    mAvatar.setImageResource(R.drawable.ic_person_gray_80dp)
                }
                mTitleText.text = value.item.name.capitalize()
                mSubTitleTv.text = value.item.shownValue

                if (value.item.shownValue == null) mSubTitleTv.setViewGone()
                else mSubTitleTv.setViewVisible()

                val subscriptionState = value.item.subscribeStatus?.isCurrentUserSubscribed
                if (subscriptionState == null) {
                    mSubscibeButton.setViewGone()
                } else {
                    mSubscibeButton.setViewVisible()
                    mSubscibeButton.text = if (subscriptionState) itemView.context.getString(R.string.unfollow)
                    else itemView.context.getString(R.string.follow)
                }

                mSubscibeButton.setOnClickListener { value.onSubscribeClick.invoke(this) }
                mAvatar.setOnClickListener { value.onUserClick.invoke(this) }
                mTitleText.setOnClickListener { value.onUserClick.invoke(this) }
                itemView.setOnClickListener { value.onUserClick.invoke(this) }

                val updatingState = value.item.subscribeStatus?.updatingState

                if (updatingState == null) {
                    mProgress.setViewGone()
                    mSubscibeButton.setViewGone()
                } else {
                    if (updatingState == UpdatingState.UPDATING) {
                        mProgress.setViewVisible()
                        mSubscibeButton.setViewGone()
                    } else {
                        mProgress.setViewGone()
                        mSubscibeButton.setViewVisible()
                    }
                }
            }
        }
}