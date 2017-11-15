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

/**
 * Created by yuri on 08.11.17.
 */

class MainStoryAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
            is TextBlockHolder -> holder.text = (items[position] as TextRow).text
            is ImageBlockHolder -> holder.imageSrc = (items[position] as ImageRow).src
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

    var imageSrc: String = ""
        set(value) {
            field = value

            if (field.isEmpty()) {
                mGlide.load(R.drawable.error).apply(RequestOptions().fitCenter()).into(mImage)
            } else {
                mImage.scaleType = ImageView.ScaleType.FIT_CENTER
                val error = mGlide.load(R.drawable.error)
                mGlide.load(field).error(error).apply(RequestOptions().fitCenter().placeholder(R.drawable.error)).into(mImage)
            }
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

    var text: String = ""
        set(value) {
            val width = itemView.resources.displayMetrics.widthPixels - 2 * (itemView.resources.getDimension(R.dimen.margin_material)).toInt()
            field = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mText.text = Html.fromHtml(field, Html.FROM_HTML_MODE_LEGACY, GlideImageGetter(mText, width), null)
            } else {
                mText.text = Html.fromHtml(field, GlideImageGetter(mText, width), null)
            }
        }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_story_text_block, parent, false)
    }
}