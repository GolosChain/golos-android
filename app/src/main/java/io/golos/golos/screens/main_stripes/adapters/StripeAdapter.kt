package io.golos.golos.screens.main_stripes.adapters

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
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
import io.golos.golos.screens.story.model.ItemType
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.Translit
import ru.noties.markwon.view.MarkwonViewCompat
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.Executors


private val adapterWorkerExecutor = Executors.newSingleThreadExecutor()
private val adapterMainThreadExecutor: Executor by lazy {
    val handler = Handler(Looper.getMainLooper())
    Executor { handler.post(it) }
}


data class StripeWrapper(val stripe: StoryTree,
                         val onUpvoteClick: (RecyclerView.ViewHolder) -> Unit,
                         val onCardClick: (RecyclerView.ViewHolder) -> Unit,
                         val onCommentsClick: (RecyclerView.ViewHolder) -> Unit,
                         val onShareClick: (RecyclerView.ViewHolder) -> Unit)

class StripeAdapter(private var onCardClick: (StoryTree) -> Unit = { print(it) },
                    private var onCommentsClick: (StoryTree) -> Unit = { print(it) },
                    private var onShareClick: (StoryTree) -> Unit = { print(it) },
                    private var onUpvoteClick: (StoryTree) -> Unit = { print(it) })
    : RecyclerView.Adapter<StripeViewHolder>() {


    private var stripes = ArrayList<StoryTree>()

    fun setStripesCustom(newItems: List<StoryTree>) {
        if (stripes.isEmpty()) {
            stripes = ArrayList(newItems).clone() as ArrayList<StoryTree>
            notifyDataSetChanged()
        } else {
            adapterWorkerExecutor.execute {
                val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return stripes[oldItemPosition].rootStory()?.id == newItems[newItemPosition].rootStory()?.id
                    }

                    override fun getOldListSize() = stripes.size
                    override fun getNewListSize() = newItems.size
                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return stripes[oldItemPosition].rootStoryWrapper() == newItems[newItemPosition].rootStoryWrapper()
                    }
                })
                adapterMainThreadExecutor.execute {
                    result.dispatchUpdatesTo(this)
                    stripes = ArrayList(newItems)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: StripeViewHolder?, position: Int) {
        holder?.state = StripeWrapper(stripes[position],
                onUpvoteClick = { onUpvoteClick.invoke(stripes[it.adapterPosition]) },
                onCardClick = { onCardClick.invoke(stripes[it.adapterPosition]) },
                onCommentsClick = { onCommentsClick.invoke(stripes[it.adapterPosition]) },
                onShareClick = { onShareClick.invoke(stripes[it.adapterPosition]) })
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
                val wrapper = field?.stripe?.rootStory() ?: return
                views.forEach({
                    when (it) {
                        mCommentsButton -> it.setOnClickListener({ field!!.onCommentsClick(this) })
                        mShareBtn -> it.setOnClickListener({ field!!.onShareClick(this) })
                        mUpvoteBtn -> it.setOnClickListener({ field!!.onUpvoteClick(this) })
                        else -> it.setOnClickListener({ field!!.onCardClick(this) })
                    }
                })
                mUpvoteBtn.setOnClickListener({ field!!.onUpvoteClick(this) })
                mUserNameTv.text = wrapper.author
                if (wrapper.firstRebloggedBy.isNotEmpty()) {
                    mRebloggedByTv.text = wrapper.firstRebloggedBy
                } else {
                    mRebloggedByTv.visibility = View.INVISIBLE
                }
                if (wrapper.categoryName.contains("ru--")) {
                    mBlogNameTv.text = Translit.lat2Ru(wrapper.categoryName.substring(4))
                } else {
                    mBlogNameTv.text = wrapper.categoryName
                }
                mTitleTv.text = wrapper.title
                mUpvoteBtn.text = "$ ${String.format("%.2f", wrapper.payoutInDollars)}"
                mCommentsButton.text = wrapper.commentsCount.toString()
                if (wrapper.avatarPath != null) mGlide
                        .load(wrapper.avatarPath)
                        .error(mGlide.load(R.drawable.ic_person_gray_24dp))
                        .apply(RequestOptions.placeholderOf(R.drawable.ic_person_gray_24dp))
                        .into(mAvatar)
                else mAvatar.setImageResource(R.drawable.ic_person_gray_24dp)
                val res = itemView.resources
                if (wrapper.isUserUpvotedOnThis) {
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(res.getDrawable(R.drawable.ic_upvote_green_24dp), null, null, null)
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.upvote_green))
                } else {
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(res.getDrawable(R.drawable.ic_upvote_gray_24dp), null, null, null)
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_1))
                }

                when {
                    wrapper.type == ItemType.PLAIN -> {
                        mMainImageBig.visibility = View.GONE
                        mSecondaryImage.visibility = View.GONE
                        mBodyTextMarkwon.visibility = View.VISIBLE
                        mBodyTextMarkwon.markdown = wrapper.cleanedFromImages()
                    }
                    wrapper.type == ItemType.PLAIN_WITH_IMAGE -> {

                        mMainImageBig.visibility = View.GONE
                        mSecondaryImage.visibility = View.VISIBLE
                        mBodyTextMarkwon.visibility = View.VISIBLE
                        val options = RequestOptions()
                        options.centerInside()
                        mSecondaryImage.setImageBitmap(null)
                        if (wrapper.images.size > 0) {
                            val error = mGlide.load(R.drawable.error)
                            mGlide.load(wrapper.images[0])
                                    .apply(options)
                                    .error(error)

                                    .into(mSecondaryImage)
                        }
                        if (wrapper.format == io.golos.golos.screens.story.model.Format.HTML) {
                            val result: Spanned
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                result = Html.fromHtml(wrapper.body, Html.FROM_HTML_MODE_LEGACY)

                            } else {
                                result = Html.fromHtml(wrapper.body)
                            }
                            mBodyTextMarkwon.text = result
                        } else {
                            mBodyTextMarkwon.markdown = wrapper.cleanedFromImages().replace("\n", "")
                        }
                    }
                    else -> {
                        val error = mGlide.load(R.drawable.error)
                        var nextImage: RequestBuilder<Drawable>? = null
                        if (wrapper.images.size > 1) {
                            nextImage = mGlide.load(wrapper.images[1]).error(error)
                        }
                        mMainImageBig.scaleType = ImageView.ScaleType.FIT_CENTER
                        mMainImageBig.visibility = View.VISIBLE
                        mSecondaryImage.visibility = View.GONE
                        mBodyTextMarkwon.visibility = View.GONE
                        if (wrapper.images.size > 0) {
                            mGlide.load(wrapper.images[0])
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