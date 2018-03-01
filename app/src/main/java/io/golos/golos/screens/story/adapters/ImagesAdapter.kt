package io.golos.golos.screens.story.adapters

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.widgets.GolosViewHolder
import io.golos.golos.utils.ImageUriResolver

/**
 * Created by yuri on 26.12.17.
 */
data class ImageHolderState(val imageRow: ImageRow,
                            val onImageClick: (RecyclerView.ViewHolder) -> Unit)

class ImagesAdapter(private var onImageClick: (ImageRow) -> Unit = { print(it) },
                    images: ArrayList<ImageRow>) : RecyclerView.Adapter<StoryBottomImagesViewHolder>() {

    private var list = ArrayList<ImageRow>()

    init {
        this.list = ArrayList(images)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): StoryBottomImagesViewHolder {
        return StoryBottomImagesViewHolder(R.layout.v_image, parent!!)
    }

    override fun onBindViewHolder(holder: StoryBottomImagesViewHolder?, position: Int) {
        holder?.state = ImageHolderState(list[position], { onImageClick.invoke(list[it.adapterPosition]) })
    }
}


class StoryBottomImagesViewHolder(resId: Int, parent: ViewGroup) : GolosViewHolder(resId, parent) {
    private var mImage = itemView as ImageView
    var state: ImageHolderState? = null
        set(value) {
            field = value
            if (value == null) {
                mImage.setImageResource(R.drawable.error)
            } else {
                Glide.with(mImage)
                        .load(ImageUriResolver.resolveImageWithSize(value.imageRow.src, wantedwidth = mImage.width))
                        .apply(RequestOptions().placeholder(R.drawable.error).error(R.drawable.error))
                        .into(mImage)
                mImage.setOnClickListener { value.onImageClick.invoke(this) }
            }
        }
}