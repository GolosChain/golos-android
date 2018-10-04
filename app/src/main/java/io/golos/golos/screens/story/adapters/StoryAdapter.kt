package io.golos.golos.screens.story.adapters

import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.ViewTarget
import io.golos.golos.R
import io.golos.golos.screens.editor.knife.KnifeParser
import io.golos.golos.screens.editor.knife.SpanFactory
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.Row
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.utils.GolosMovementMethod
import io.golos.golos.utils.ImageUriResolver
import io.golos.golos.utils.createGolosSpan

data class TextRowWrapper(val row: TextRow)

data class ImageRowWrapper(val row: ImageRow,
                           val imageInsets: Int,
                           val clickListener: (androidx.recyclerview.widget.RecyclerView.ViewHolder, View) -> Unit,
                           val onPopup: LongClick?) {
    data class LongClick(val onLinkSaveClick: (RecyclerView.ViewHolder) -> Unit, val onImageSaveClick: (RecyclerView.ViewHolder) -> Unit)
}

interface OnImagePopup {
    fun onLinkSave(row: ImageRow)
    fun onImageSave(row: ImageRow)
}

class StoryAdapter(private val imageStartAndEndInsets: Int = 0,
                   private val onImageLongClick: OnImagePopup? = null) : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
    var onRowClick: ((Row, ImageView?) -> Unit)? = null

    var items: List<Row> = emptyList()
        set(value) {
            val newItems = value

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        return if (viewType == 0) TextBlockHolder(parent)
        else ImageBlockHolder(parent)
    }

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TextBlockHolder -> holder.state = TextRowWrapper(items[position] as TextRow)
            is ImageBlockHolder -> holder.state = ImageRowWrapper(items[position] as ImageRow, imageStartAndEndInsets, { vh, v ->
                if (vh.adapterPosition == -1) return@ImageRowWrapper
                onRowClick?.invoke(items[vh.adapterPosition], v as? ImageView)
            }, if (onImageLongClick == null) null else ImageRowWrapper.LongClick(
                    {
                        val row = items[it.adapterPosition] as ImageRow
                        onImageLongClick.onLinkSave(row)

                    },
                    {
                        val row = items[it.adapterPosition] as ImageRow
                        onImageLongClick.onImageSave(row)
                    }))
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

class ImageBlockHolder(parent: ViewGroup) : androidx.recyclerview.widget.RecyclerView.ViewHolder(this.inflate(parent)) {
    private val mImageFullWidth: ImageView = itemView.findViewById(R.id.image_full_width)
    private val mSmallImage: ImageView = itemView.findViewById(R.id.small_image)
    private val mMediumImage: ImageView = itemView.findViewById(R.id.medium_image)

    private val mGlide = Glide.with(itemView)
    private val handler = android.os.Handler(Looper.getMainLooper())
    private var mTarget: ViewTarget<View, Bitmap>? = null

    var state: ImageRowWrapper? = null
        set(value) {
            mGlide.clear(mSmallImage)
            mGlide.clear(mMediumImage)
            mGlide.clear(mImageFullWidth)


            field = value
            if (field == null) return
            if (mImageFullWidth.paddingStart != field!!.imageInsets) {
                mImageFullWidth.setPadding(field!!.imageInsets, 0, field!!.imageInsets, 0)
            }
            handler.removeCallbacksAndMessages(null)
            val imageRow = field!!.row
            if (imageRow.src.isEmpty()) {
                mSmallImage.visibility = View.GONE
                mMediumImage.visibility = View.GONE
                mImageFullWidth.visibility = View.VISIBLE
                mGlide.load(R.drawable.error)
                        .into(mImageFullWidth)
            } else {
                var src = imageRow.src
                mImageFullWidth.setImageResource(R.drawable.error)

                mSmallImage.visibility = View.GONE
                mMediumImage.visibility = View.GONE
                mImageFullWidth.visibility = View.VISIBLE


                if (src.endsWith("/")) src = src.substring(0 until src.lastIndex)
                src = ImageUriResolver.resolveImageWithSize(src)

                mTarget = object : ViewTarget<View, Bitmap>(itemView) {

                    override fun getSize(cb: SizeReadyCallback?) {
                        cb?.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    }


                    override fun onResourceReady(resource: Bitmap?, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>) {
                        if (resource == null || resource.isRecycled) return

                        if (resource.width < 150 || resource.height < 150) {
                            mSmallImage.visibility = View.VISIBLE
                            mImageFullWidth.visibility = View.GONE
                            mMediumImage.visibility = View.GONE
                            handler.post {
                                mGlide.load(src)
                                        .into(mSmallImage)
                            }
                        } else if (resource.width < 250 || resource.height < 250) {
                            mMediumImage.visibility = View.VISIBLE
                            mImageFullWidth.visibility = View.GONE

                            mSmallImage.visibility = View.GONE
                            mMediumImage.setImageBitmap(null)
                            handler.post {
                                mGlide.load(src)
                                        .into(mMediumImage)
                            }
                        } else {
                            mImageFullWidth.visibility = View.VISIBLE

                            mMediumImage.visibility = View.GONE
                            mSmallImage.visibility = View.GONE
                            mImageFullWidth.setImageBitmap(null)
                            handler.post {
                                mGlide
                                        .load(src)
                                        .apply(RequestOptions()
                                                .fitCenter()
                                                .placeholder(R.drawable.error))
                                        .into(mImageFullWidth)
                            }

                        }
                        handler.post {
                            mGlide.clear(this)
                        }
                    }
                }
                mGlide
                        .asBitmap()
                        .load(src)
                        .into(mTarget ?: return)
            }
            mImageFullWidth.setOnClickListener { field?.clickListener?.invoke(this, mImageFullWidth) }
            mSmallImage.setOnClickListener { field?.clickListener?.invoke(this, mSmallImage) }

            val popupListener = field?.onPopup ?: return
            val longClickListener = View.OnLongClickListener {
                val popup = PopupWindow(LayoutInflater.from(itemView.context).inflate(R.layout.v_save_dialo, null),
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT)
                popup.setBackgroundDrawable(ColorDrawable())
                popup.isOutsideTouchable = true

                popup.showAtLocation(itemView, Gravity.CENTER, 0, 0)

                popup.contentView.findViewById<View>(R.id.copy_to_cb_btn).setOnClickListener { popupListener.onLinkSaveClick(this) ;popup.dismiss()}
                popup.contentView.findViewById<View>(R.id.save_btn).setOnClickListener { popupListener.onImageSaveClick(this);popup.dismiss() }
                true
            }
            mImageFullWidth.setOnLongClickListener(longClickListener)
            mSmallImage.setOnLongClickListener(longClickListener)
            mMediumImage.setOnLongClickListener(longClickListener)
        }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_main_story_image, parent, false)
    }
}

class TextBlockHolder(parent: ViewGroup) : androidx.recyclerview.widget.RecyclerView.ViewHolder(this.inflate(parent)), SpanFactory {
    override fun <T : Any?> produceOfType(type: Class<*>): T {
        return itemView.context.createGolosSpan(type)
    }

    private val mText: TextView = itemView.findViewById(R.id.text)

    init {
        mText.movementMethod = GolosMovementMethod.instance
    }

    var state: TextRowWrapper? = null
        set(value) {
            field = value
            if (field == null) return
            val textRow = field!!.row
            val text = KnifeParser.fromHtml(textRow.text, this)
            if (text.isEmpty()) {
                mText.visibility = View.GONE
            } else {
                mText.visibility = View.VISIBLE
                mText.text = text
            }
        }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_story_text_block, parent, false)
    }
}