package io.golos.golos.screens.main_stripes.adapters

import android.graphics.drawable.Drawable
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.screens.main_stripes.model.Format
import io.golos.golos.screens.main_stripes.model.StripeItem
import io.golos.golos.screens.main_stripes.model.StripeItemType
import io.golos.golos.utils.Translit
import ru.noties.markwon.view.MarkwonViewCompat


data class StripeWrapper(val stripe: StripeItem,
                         val onCardClick: (RecyclerView.ViewHolder) -> Unit,
                         val onCommentsClick: (RecyclerView.ViewHolder) -> Unit,
                         val onShareClick: (RecyclerView.ViewHolder) -> Unit)

class StripeAdapter(private var onCardClick: (StripeItem) -> Unit = { print(it.body) },
                    private var onCommentsClick: (StripeItem) -> Unit = { print(it.body) },
                    private var onShareClick: (StripeItem) -> Unit = { print(it.body) })
    : RecyclerView.Adapter<StripeViewHolder>() {


    private var stripes = ArrayList<StripeItem>()
    private var stripes_old = ArrayList<StripeItem>()

    fun setStripesCustom(newItems: List<StripeItem>) {
        if (stripes.isEmpty()) {
            stripes = ArrayList(newItems)
            stripes_old = ArrayList(newItems)
            notifyDataSetChanged()
        } else {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return stripes[oldItemPosition].id == newItems[newItemPosition].id
                }

                override fun getOldListSize() = stripes.size
                override fun getNewListSize() = newItems.size
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return stripes[oldItemPosition] == newItems[newItemPosition]
                }
            }).dispatchUpdatesTo(this)
            stripes = ArrayList(newItems)
        }
    }

    override fun onBindViewHolder(holder: StripeViewHolder?, position: Int) {
        holder?.state = StripeWrapper(stripes[position],
                { onCardClick.invoke(stripes[it.adapterPosition]) },
                { onCommentsClick.invoke(stripes[it.adapterPosition]) },
                { onShareClick.invoke(stripes[it.adapterPosition]) })
    }

    override fun getItemCount() = stripes.size

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) = StripeViewHolder(parent!!)

}


class StripeViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(this.inflate(parent)) {
    private val mAvatar: ImageView = itemView.findViewById(R.id.avatar_iv)
    private val mUserNameTv: TextView = itemView.findViewById(R.id.user_name)
    private val mRebloggedByTv: TextView = itemView.findViewById(R.id.reblogged_tv)
    private val mBlogNameTv: TextView = itemView.findViewById(R.id.blog_name_tv)
    private val mTitleTv: TextView = itemView.findViewById(R.id.title)
    private val mBodyTextMarkwon: MarkwonViewCompat = itemView.findViewById(R.id.text)
    private val mSecondaryImage: ImageView = itemView.findViewById(R.id.additional_image)
    private val mMainImageBig: ImageView = itemView.findViewById(R.id.image_main)
    private val mUpvoteBtn: Button = itemView.findViewById(R.id.vote_btn)
    private val mCommentsButton: Button = itemView.findViewById(R.id.comments_btn)
    private val mShareBtn: ImageButton = itemView.findViewById(R.id.share_btn)
    private val views = listOf<View>(mAvatar, mUserNameTv, mRebloggedByTv,
            mBlogNameTv, mTitleTv, mBodyTextMarkwon, mSecondaryImage, mMainImageBig, mUpvoteBtn, mCommentsButton, mShareBtn, itemView)
    private val mGlide = Glide.with(parent.context)


    var state: StripeWrapper? = null
        set(value) {
            field = value
            if (field != null) {
                val wrapper = field!!
                views.forEach({
                    when (it) {
                        mCommentsButton -> it.setOnClickListener({ wrapper.onCommentsClick(this) })
                        mShareBtn -> it.setOnClickListener({ wrapper.onShareClick(this) })
                        else -> it.setOnClickListener({ wrapper.onCardClick(this) })
                    }
                })
                mUserNameTv.text = wrapper.stripe.author
                if (wrapper.stripe.firstRebloggedBy.isNotEmpty()) {
                    mRebloggedByTv.text = wrapper.stripe.firstRebloggedBy
                } else {
                    mRebloggedByTv.visibility = View.INVISIBLE
                }
                if (wrapper.stripe.categoryName.contains("ru--")) {
                    mBlogNameTv.text = Translit.lat2Ru(wrapper.stripe.categoryName.substring(4))
                } else {
                    mBlogNameTv.text = wrapper.stripe.categoryName
                }
                mTitleTv.text = wrapper.stripe.title
                mUpvoteBtn.text = wrapper.stripe.payoutInDollats
                mCommentsButton.text = wrapper.stripe.votesNum.toString()
                if (wrapper.stripe.avatarPath != null) mGlide
                        .load(wrapper.stripe.avatarPath)
                        .error(mGlide.load(R.drawable.ic_person_gray_24dp))
                        .apply(RequestOptions.placeholderOf(R.drawable.ic_person_gray_24dp))
                        .into(mAvatar)
                else mAvatar.setImageResource(R.drawable.ic_person_gray_24dp)
                when {
                    wrapper.stripe.type == StripeItemType.PLAIN -> {
                        mMainImageBig.visibility = View.GONE
                        mSecondaryImage.visibility = View.GONE
                        mBodyTextMarkwon.visibility = View.VISIBLE
                        mBodyTextMarkwon.markdown = wrapper.stripe.body
                    }
                    wrapper.stripe.type == StripeItemType.PLAIN_WITH_IMAGE -> {

                        mMainImageBig.visibility = View.GONE
                        mSecondaryImage.visibility = View.VISIBLE
                        mBodyTextMarkwon.visibility = View.VISIBLE
                        val options = RequestOptions()
                        options.centerInside()
                        mSecondaryImage.setImageBitmap(null)
                        if (wrapper.stripe.images.size > 0) {
                            val error = mGlide.load(R.drawable.error)
                            mGlide.load(wrapper.stripe.images[0])
                                    .apply(options)
                                    .error(error)

                                    .into(mSecondaryImage)
                        }
                        if (wrapper.stripe.format == Format.HTML) {
                            val result: Spanned
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                result = Html.fromHtml(wrapper.stripe.body, Html.FROM_HTML_MODE_LEGACY)

                            } else {
                                result = Html.fromHtml(wrapper.stripe.body)
                            }
                            mBodyTextMarkwon.text = result
                        } else {
                            mBodyTextMarkwon.markdown = wrapper.stripe.body.replace("\n", "")
                        }
                    }
                    else -> {
                        val error = mGlide.load(R.drawable.error)
                        var nextImage: RequestBuilder<Drawable>? = null
                        if (wrapper.stripe.images.size > 1) {
                            nextImage = mGlide.load(wrapper.stripe.images[1]).error(error)
                        }
                        mMainImageBig.scaleType = ImageView.ScaleType.FIT_CENTER
                        mMainImageBig.visibility = View.VISIBLE
                        mSecondaryImage.visibility = View.GONE
                        mBodyTextMarkwon.visibility = View.GONE
                        if (wrapper.stripe.images.size > 0) {
                            mGlide.load(wrapper.stripe.images[0])
                                    .error(nextImage ?: error)
                                    .apply(RequestOptions.placeholderOf(R.drawable.error))
                                    .into(mMainImageBig)
                        }
                    }
                }
            } else {
                views.forEach {
                    it.setOnClickListener(null)
                    if (it is TextView) it.text = ""
                    else (it as? ImageView)?.setImageBitmap(null)
                }
            }
        }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_stripe_full_size, parent, false)
    }
}