package io.golos.golos.screens.tags.adapters

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.screens.widgets.GolosViewHolder

/**
 * Created by yuri on 06.01.18.
 */


data class SearchTagState(val tag: LocalizedTag,
                          val onTagClickListener: (RecyclerView.ViewHolder) -> Unit)

class SearchTagAdapter(var onTagClick: (LocalizedTag) -> Unit) : RecyclerView.Adapter<TagWithSearchIconVh>() {

    var tags: List<LocalizedTag> = ArrayList()
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

    override fun onBindViewHolder(holder: TagWithSearchIconVh?, position: Int) {
        holder?.state = SearchTagState(tags[position],
                { onTagClick.invoke(tags[it.adapterPosition]) })

    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): TagWithSearchIconVh {
        return TagWithSearchIconVh(parent!!)
    }

    override fun getItemCount(): Int {
        return tags.size
    }
}

class TagWithSearchIconVh(parent: ViewGroup) : GolosViewHolder(R.layout.vh_tag_search, parent) {
    private val mTagNameTv = itemView as TextView

    var state: SearchTagState? = null
        set(value) {
            field = value
            if (field == null) {
                mTagNameTv.setOnClickListener(null)
                mTagNameTv.text = ""
            } else {
                mTagNameTv.text = field?.tag?.getLocalizedName()?.capitalize() ?: ""
                mTagNameTv.setOnClickListener { field?.onTagClickListener?.invoke(this) }
            }
        }
}