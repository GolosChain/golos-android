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
import io.golos.golos.repository.model.FollowUserObject
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.utils.UpdatingState

/**
 * Created by yuri on 22.12.17.
 */
data class UserListItemState(val item: FollowUserObject,
                             val onUserClick: (RecyclerView.ViewHolder) -> Unit,
                             val onSubscribeClick: (RecyclerView.ViewHolder) -> Unit)

class UserListAdapter(private val onUserClick: (FollowUserObject) -> Unit = { _ -> },
                      private val onSubscribeClick: (FollowUserObject) -> Unit = { _ -> }) : RecyclerView.Adapter<UserListViewHolder>() {
    private val mHashes = HashMap<String, Int>()
    var listItems: List<FollowUserObject> = ArrayList()
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


    override fun getItemCount(): Int {
        return listItems.size
    }

    override fun onBindViewHolder(holder: UserListViewHolder?, position: Int) {
        holder?.state = UserListItemState(listItems[position],
                { onUserClick.invoke(listItems[it.adapterPosition]) },
                { onSubscribeClick.invoke(listItems[it.adapterPosition]) })
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): UserListViewHolder {
        return UserListViewHolder(parent!!)
    }
}


class UserListViewHolder(parent: ViewGroup) : GolosViewHolder(R.layout.v_user_list_item, parent) {
    private val mAvatar = itemView.findViewById<ImageView>(R.id.avatar_iv)
    private val mTitleText = itemView.findViewById<TextView>(R.id.name_tv)
    private val mSubscibeButton = itemView.findViewById<Button>(R.id.follow_btn)
    private val mProgress = itemView.findViewById<View>(R.id.progress)
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
                    Glide
                            .with(itemView)
                            .load(value.item.avatar)
                            .apply(RequestOptions()
                                    .error(R.drawable.ic_person_gray_80dp)
                                    .placeholder(R.drawable.ic_person_gray_80dp))
                            .into(mAvatar)
                } else {
                    mAvatar.setImageResource(R.drawable.ic_person_gray_80dp)
                }
                mTitleText.text = value.item.name.capitalize()
                mSubscibeButton.text = if (value.item.subscribeStatus.isCurrentUserSubscribed) itemView.context.getString(R.string.unfollow)
                else itemView.context.getString(R.string.follow)
                mSubscibeButton.setOnClickListener { value.onSubscribeClick.invoke(this) }
                mAvatar.setOnClickListener { value.onUserClick.invoke(this) }
                mTitleText.setOnClickListener { value.onUserClick.invoke(this) }
                if (value.item.subscribeStatus.updatingState == UpdatingState.UPDATING) {
                    mProgress.visibility = View.VISIBLE
                    mSubscibeButton.visibility = View.GONE
                } else {
                    mProgress.visibility = View.GONE
                    mSubscibeButton.visibility = View.VISIBLE
                }
            }
        }
}