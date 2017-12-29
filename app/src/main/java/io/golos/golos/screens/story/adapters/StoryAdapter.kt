package io.golos.golos.screens.story.adapters

import android.graphics.drawable.Drawable
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.ViewTarget
import com.bumptech.glide.request.transition.Transition
import io.golos.golos.R
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.Row
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.utils.GolosMovementMethod
import io.golos.golos.utils.toArrayList
import io.golos.golos.utils.toHtml

class RowWrapper(val row: Row,
                 val clickListener: (RecyclerView.ViewHolder, View) -> Unit = { _, _ -> print("clicked") })

class StoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
    private val mImageFullWidth: ImageView = itemView.findViewById(R.id.image_full_width)
    private val mSmallImage: ImageView = itemView.findViewById(R.id.small_image)
    private val mMediumImage: ImageView = itemView.findViewById(R.id.medium_image)
    private val mFrame: FrameLayout = itemView.findViewById(R.id.frame)
    private val mGlide = Glide.with(parent.context)
    private var mTarget: ViewTarget<View, Drawable>? = null

    var state: RowWrapper? = null
        set(value) {
            mGlide.clear(mSmallImage)
            mGlide.clear(mMediumImage)
            mGlide.clear(mImageFullWidth)
            mGlide.clear(itemView)
            mTarget?.let {
                mGlide.clear(it)
            }
            field = value
            if (field == null) return
            val imageRow = field!!.row as ImageRow
            if (imageRow.src.isEmpty()) {
                mSmallImage.visibility = View.GONE
                mImageFullWidth.visibility = View.VISIBLE
                mGlide.load(R.drawable.error)
                        .into(mImageFullWidth)
            } else {
                var src = imageRow.src
                mSmallImage.visibility = View.GONE
                mMediumImage.visibility = View.GONE
                mImageFullWidth.visibility = View.VISIBLE

                mImageFullWidth.setImageBitmap(null)
                mImageFullWidth.setImageResource(R.drawable.error)

                if (src.endsWith("/")) src = src.substring(0..src.lastIndex - 1)

                mTarget = object : ViewTarget<View, Drawable>(itemView) {

                    override fun getSize(cb: SizeReadyCallback?) {
                        cb?.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    }

                    override fun onDestroy() {
                        mGlide.clear(mSmallImage)
                        mGlide.clear(mMediumImage)
                        mGlide.clear(mImageFullWidth)
                        mGlide.clear(itemView)
                        mTarget?.let {
                            mGlide.clear(it)
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        super.onLoadCleared(placeholder)
                        mGlide.clear(mSmallImage)
                        mGlide.clear(mMediumImage)
                        mGlide.clear(mImageFullWidth)
                    }

                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>) {
                        resource?.let {
                            if (it.intrinsicWidth < 150 || it.intrinsicHeight < 150) {
                                mImageFullWidth.visibility = View.GONE
                                mMediumImage.visibility = View.GONE

                                mSmallImage.visibility = View.VISIBLE
                                mGlide.load(it)

                                        .into(mSmallImage)
                            } else if (it.intrinsicWidth < 250 || it.intrinsicHeight < 250) {
                                mImageFullWidth.visibility = View.GONE
                                mSmallImage.visibility = View.GONE

                                mMediumImage.visibility = View.VISIBLE

                                mMediumImage.setImageBitmap(null)
                                mGlide.load(it)

                                        .into(mMediumImage)
                            } else {
                                mMediumImage.visibility = View.GONE
                                mSmallImage.visibility = View.GONE

                                mImageFullWidth.visibility = View.VISIBLE

                                mImageFullWidth.setImageBitmap(null)
                                mGlide
                                        .load(it)

                                        .apply(RequestOptions()
                                                .fitCenter()
                                                .placeholder(R.drawable.error))
                                        .into(mImageFullWidth)
                            }
                        }

                    }
                }

                mGlide
                        .load(src)
                        .into(mTarget ?: return)
            }
            mImageFullWidth.setOnClickListener({ field?.clickListener?.invoke(this, mImageFullWidth) })
            mSmallImage.setOnClickListener({ field?.clickListener?.invoke(this, mSmallImage) })
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