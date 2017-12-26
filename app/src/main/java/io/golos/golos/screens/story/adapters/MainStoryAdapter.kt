package io.golos.golos.screens.story.adapters

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.Row
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.utils.GolosMovementMethod
import io.golos.golos.utils.toArrayList
import io.golos.golos.utils.toHtml

class RowWrapper(val row: Row,
                 val clickListener: (RecyclerView.ViewHolder, View) -> Unit = { _, _ -> print("clicked") })

class MainStoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onRowClick: ((Row, ImageView?) -> Unit)? = null
    var items = ArrayList<Row>()
        set(value) {
            val newItems = value.filter {
                it is ImageRow
                        || (it is TextRow && it.text.toHtml().isNotEmpty())
            }.toArrayList()

            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition] == newItems[newItemPosition]
                }

                override fun getOldListSize() = field.size
                override fun getNewListSize() = newItems.size
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition] == newItems[newItemPosition]
                }
            }).dispatchUpdatesTo(this)
            field = newItems
        }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 0) return TextBlockHolder(parent!!)
        else return ImageBlockHolder(parent!!)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        when (holder) {
            is TextBlockHolder -> holder.state = RowWrapper(items[position], { vh, v ->
                if (vh.adapterPosition == -1) return@RowWrapper
                onRowClick?.invoke(items[vh.adapterPosition], null)
            })
            is ImageBlockHolder -> holder.state = RowWrapper(items[position], { vh, v ->
                if (vh.adapterPosition == -1) return@RowWrapper
                onRowClick?.invoke(items[vh.adapterPosition], v as? ImageView)
            })
        }
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun getItemViewType(position: Int): Int {
        if (items[position] is TextRow) return 0
        else return 1
    }
}

class ImageBlockHolder(parent: ViewGroup) : RecyclerView.ViewHolder(this.inflate(parent)) {
    private val mImage: ImageView = itemView.findViewById(R.id.image)
    private val mGlide = Glide.with(parent.context)

    var state: RowWrapper? = null
        set(value) {
            field = value
            if (field == null) return
            val imageRow = field!!.row as ImageRow
            if (imageRow.src.isEmpty()) {
                mGlide.load(R.drawable.error).apply(RequestOptions().fitCenter()).into(mImage)
            } else {
                mImage.scaleType = ImageView.ScaleType.FIT_CENTER
                val error = mGlide.load(R.drawable.error)
                var src = imageRow.src
                if (src.endsWith("/")) src = src.substring(0..src.lastIndex - 1)
                mGlide.load(src).error(error).apply(RequestOptions().fitCenter().placeholder(R.drawable.error)).into(mImage)
            }
            mImage.setOnClickListener({ field?.clickListener?.invoke(this, mImage) })
        }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_main_story_image, parent, false)
    }
}

class TextBlockHolder(parent: ViewGroup) : RecyclerView.ViewHolder(this.inflate(parent)) {
    private val mText: TextView = itemView.findViewById(R.id.text)

    init {
        mText.movementMethod = GolosMovementMethod.instance
    }

    var state: RowWrapper? = null
        set(value) {
            field = value
            if (field == null) return
            val textRow = field!!.row as TextRow
            val text = textRow.text.toHtml()
            if (text.isEmpty()) {
                mText.visibility = View.GONE
            } else {
                mText.visibility = View.VISIBLE
                mText.text = text
            }
            mText.setOnClickListener({ field?.clickListener?.invoke(this, mText) })
        }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_story_text_block, parent, false)
    }
}