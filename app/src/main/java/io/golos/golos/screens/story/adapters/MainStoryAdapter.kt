package io.golos.golos.screens.story.adapters

import android.os.Build
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.screens.story.GlideImageGetter
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.Row
import io.golos.golos.screens.story.model.TextRow

class RowWrapper(val row: Row,
                 val clickListener: (RecyclerView.ViewHolder, View) -> Unit = { _, _ -> print("clicked") })

class MainStoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onRowClick: ((Row, ImageView?) -> Unit)? = null
    var items = ArrayList<Row>()
        set(value) {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition] == value[newItemPosition]
                }

                override fun getOldListSize() = field.size
                override fun getNewListSize() = value.size
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return field[oldItemPosition] == value[newItemPosition]
                }
            }).dispatchUpdatesTo(this)
            field = value
        }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 0) return TextBlockHolder(parent!!)
        else return ImageBlockHolder(parent!!)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        when (holder) {
            is TextBlockHolder -> holder.state = RowWrapper(items[position], { vh, v ->
                onRowClick?.invoke(items[vh.adapterPosition], null)
            })
            is ImageBlockHolder -> holder.state = RowWrapper(items[position], { vh, v ->
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
        mText.movementMethod = LinkMovementMethod.getInstance()
    }

    var state: RowWrapper? = null
        set(value) {
            field = value
            if (field == null) return
            val textRow = field!!.row as TextRow
            val width = itemView.resources.displayMetrics.widthPixels - 2 * (itemView.resources.getDimension(R.dimen.margin_material)).toInt()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mText.text = Html.fromHtml(textRow.text, Html.FROM_HTML_MODE_LEGACY, GlideImageGetter(mText, width), null)
            } else {
                mText.text = Html.fromHtml(textRow.text, GlideImageGetter(mText, width), null)
            }
            mText.setOnClickListener({ field?.clickListener?.invoke(this, mText) })
        }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_story_text_block, parent, false)
    }
}