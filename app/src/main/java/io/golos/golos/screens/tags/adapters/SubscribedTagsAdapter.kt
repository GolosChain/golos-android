package io.golos.golos.screens.tags.adapters

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.screens.widgets.GolosViewHolder

/**
 * Created by yuri on 06.01.18.
 */
data class AddButtonState(val clickListener: (androidx.recyclerview.widget.RecyclerView.ViewHolder) -> Unit)

data class SubscribedTagState(val tag: LocalizedTag,
                              val onTagClickListener: (androidx.recyclerview.widget.RecyclerView.ViewHolder) -> Unit,
                              val onTagDeleteClickListener: (androidx.recyclerview.widget.RecyclerView.ViewHolder) -> Unit)

class SubscribedTagsAdapter(var onAddClick: (Unit) -> Unit,
                            var onTagClick: (LocalizedTag) -> Unit,
                            var onDeleteTagClick: (LocalizedTag) -> Unit) : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

    var tags: ArrayList<LocalizedTag> = ArrayList()
        set(value) {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition].tag.name == value[newItemPosition].tag.name
                }

                override fun getOldListSize() = field.size
                override fun getNewListSize() = value.size
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition] == value[newItemPosition]
                }
            }).dispatchUpdatesTo(this)
            field = value
        }

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        if (holder is AddButtonVh) {
            holder.state = AddButtonState(clickListener = { onAddClick.invoke(Unit) })
        } else if (holder is TagVh) {
            val realPosition = position
            holder.state = SubscribedTagState(tags[realPosition],
                    { onTagClick.invoke(tags[it.adapterPosition]) },
                    { onDeleteTagClick.invoke(tags[it.adapterPosition]) })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        if (viewType == R.layout.vh_add_btn) return AddButtonVh(parent!!)
        else return TagVh(parent!!)
    }

    override fun getItemCount(): Int {
        return tags.size
    }

    override fun getItemViewType(position: Int) = if (position == 0) R.layout.vh_add_btn else R.layout.vh_tag_white
}


class AddButtonVh(parent: ViewGroup) : GolosViewHolder(R.layout.vh_add_btn, parent) {
    var state: AddButtonState? = null
        set(value) {
            field = value
            if (field == null) itemView.setOnClickListener(null)
            else {
                itemView.setOnClickListener { field?.clickListener?.invoke(this) }
            }
        }
}

class TagVh(parent: ViewGroup) : GolosViewHolder(R.layout.vh_tag_white, parent) {
    private val mTagBtn = itemView.findViewById<TextView>(R.id.tag_name)
    private val mMinusBtn = itemView.findViewById<View>(R.id.minus_ibtn)
    var state: SubscribedTagState? = null
        set(value) {
            field = value
            if (field == null) {
                mTagBtn.setOnClickListener(null)
                mMinusBtn.setOnClickListener(null)
                mTagBtn.text = ""
            } else {
                mTagBtn.text = field?.tag?.getLocalizedName() ?: ""
                mTagBtn.setOnClickListener { field?.onTagClickListener?.invoke(this) }
                mMinusBtn.setOnClickListener { field?.onTagDeleteClickListener?.invoke(this) }
            }
        }
}