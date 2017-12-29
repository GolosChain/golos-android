package io.golos.golos.screens.stories.adapters

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.repository.model.ItemType
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.utils.*
import java.util.concurrent.Executor


private val adapterWorkerExecutor = App.computationExecutor
private val adapterMainThreadExecutor: Executor by lazy {
    val handler = Handler(Looper.getMainLooper())
    Executor { handler.post(it) }
}


data class StripeWrapper(val stripe: StoryTree,
                         val onUpvoteClick: (RecyclerView.ViewHolder) -> Unit,
                         val onCardClick: (RecyclerView.ViewHolder) -> Unit,
                         val onCommentsClick: (RecyclerView.ViewHolder) -> Unit,
                         val onShareClick: (RecyclerView.ViewHolder) -> Unit,
                         val onBlogClick: (RecyclerView.ViewHolder) -> Unit,
                         val onUserClick: (RecyclerView.ViewHolder) -> Unit)

class StoriesRecyclerAdapter(private var onCardClick: (StoryTree) -> Unit = { print(it) },
                             private var onCommentsClick: (StoryTree) -> Unit = { print(it) },
                             private var onShareClick: (StoryTree) -> Unit = { print(it) },
                             private var onUpvoteClick: (StoryTree) -> Unit = { print(it) },
                             private var onTagClick: (StoryTree) -> Unit = { print(it) },
                             private var onUserClick: (StoryTree) -> Unit = { print(it) })
    : RecyclerView.Adapter<StripeViewHolder>() {


    private var mStripes = ArrayList<StoryTree>()
    private val mItemsMap = HashMap<Long, Int>()

    fun setStripesCustom(newItems: List<StoryTree>) {
        if (mStripes.isEmpty()) {
            mStripes = ArrayList(newItems).clone() as ArrayList<StoryTree>
            notifyDataSetChanged()
            mStripes.forEach {
                mItemsMap.put(it.rootStory()?.id ?: 0L, it.hashCode())
            }
        } else {
            adapterWorkerExecutor.execute {
                val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        if (mStripes == null
                                || newItems == null
                                || mStripes.lastIndex < oldItemPosition
                                || newItems.size < newItemPosition)return false
                        return mStripes[oldItemPosition].rootStory()?.id == newItems[newItemPosition].rootStory()?.id
                    }

                    override fun getOldListSize() = mStripes.size
                    override fun getNewListSize() = newItems.size
                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        if (mStripes == null
                                || newItems == null
                                || mStripes.lastIndex < oldItemPosition
                                || newItems.size < newItemPosition)return false
                        val oldHash = mItemsMap[mStripes[oldItemPosition].rootStory()?.id ?: 0L]
                        return oldHash == newItems[newItemPosition].rootStory()?.hashCode()
                        // return mStripes[oldItemPosition] == newItems[newItemPosition]
                    }
                })
                adapterMainThreadExecutor.execute {
                    result.dispatchUpdatesTo(this)
                    mStripes = ArrayList(newItems)
                    mStripes.forEach {
                        mItemsMap.put(it.rootStory()?.id ?: 0L, it.hashCode())
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: StripeViewHolder?, position: Int) {
        holder?.state = StripeWrapper(mStripes[position],
                onUpvoteClick = { onUpvoteClick.invoke(mStripes[it.adapterPosition]) },
                onCardClick = { onCardClick.invoke(mStripes[it.adapterPosition]) },
                onCommentsClick = { onCommentsClick.invoke(mStripes[it.adapterPosition]) },
                onShareClick = { onShareClick.invoke(mStripes[it.adapterPosition]) },
                onBlogClick = { onTagClick.invoke(mStripes[it.adapterPosition]) },
                onUserClick = { onUserClick.invoke(mStripes[it.adapterPosition]) })
    }

    override fun getItemCount() = mStripes.size

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) = StripeViewHolder(parent!!)

}


class StripeViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(this.inflate(parent)) {
    private val mAvatar: ImageView = itemView.findViewById(R.id.avatar_iv)
    private val mUserNameTv: TextView = itemView.findViewById(R.id.user_name)
    private val mRebloggedByTv: TextView = itemView.findViewById(R.id.reblogged_tv)
    private val mBlogNameTv: TextView = itemView.findViewById(R.id.blog_name_tv)
    private val mTitleTv: TextView = itemView.findViewById(R.id.title)
    private val mBodyTextMarkwon: TextView = itemView.findViewById(R.id.text)
    private val mSecondaryImage: ImageView = itemView.findViewById(R.id.additional_image)
    private val mMainImageBig: ImageView = itemView.findViewById(R.id.image_main)
    private val mUpvoteBtn: Button = itemView.findViewById(R.id.vote_btn)
    private val mVotingProgress: ProgressBar = itemView.findViewById(R.id.progress)
    private val mCommentsButton: Button = itemView.findViewById(R.id.comments_btn)
    private val mShareBtn: ImageButton = itemView.findViewById(R.id.share_btn)
    private val views = listOf<View>(mAvatar, mUserNameTv, mRebloggedByTv,
            mBlogNameTv, mTitleTv, mBodyTextMarkwon, mSecondaryImage, mMainImageBig, mUpvoteBtn, mCommentsButton, mShareBtn, itemView)
    private val mGlide = Glide.with(parent.context)

    init {
        mRebloggedByTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)
        mBlogNameTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_bullet_20dp), null, null, null)
        mCommentsButton.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_chat_gray_24dp), null, null, null)
        if (noAvatarDrawable == null) noAvatarDrawable = itemView.getVectorDrawable(R.drawable.ic_person_gray_24dp)
        if (userNotvotedDrarawble == null) userNotvotedDrarawble = itemView.getVectorDrawable(R.drawable.ic_triangle_in_cricle_gray_outline_24dp)
        if (userVotedvotedDrarawble == null) userVotedvotedDrarawble = itemView.getVectorDrawable(R.drawable.ic_triangle_in_circle_green_outline_24dp)
        if (errorDrawable == null) errorDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.error)!!

        mUpvoteBtn.setCompoundDrawablesWithIntrinsicBounds(userNotvotedDrarawble, null, null, null)
    }

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
                        mBlogNameTv -> it.setOnClickListener({ field!!.onBlogClick(this) })
                        mAvatar, mUserNameTv -> it.setOnClickListener({ field!!.onUserClick(this) })
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
                mUpvoteBtn.text = "$ ${String.format("%.3f", wrapper.payoutInDollars)}"
                mCommentsButton.text = wrapper.commentsCount.toString()
                if (wrapper.avatarPath != null) mGlide
                        .load(wrapper.avatarPath)
                        .error(mGlide.load(noAvatarDrawable))
                        .apply(RequestOptions.placeholderOf(noAvatarDrawable))
                        .into(mAvatar)
                else mAvatar.setImageDrawable(noAvatarDrawable)
                val res = itemView.resources
                if (wrapper.isUserUpvotedOnThis) {
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(userVotedvotedDrarawble, null, null, null)
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.upvote_green))
                } else {
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(userNotvotedDrarawble, null, null, null)
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_4f))
                }

                if (field?.stripe?.storyWithState()?.updatingState == UpdatingState.UPDATING) {
                    mVotingProgress.visibility = View.VISIBLE
                    mUpvoteBtn.visibility = View.GONE
                } else {
                    mVotingProgress.visibility = View.GONE
                    mUpvoteBtn.visibility = View.VISIBLE
                }
                when {
                    wrapper.type == ItemType.PLAIN -> {
                        mMainImageBig.visibility = View.GONE
                        mSecondaryImage.visibility = View.GONE
                        mBodyTextMarkwon.visibility = View.VISIBLE
                        mBodyTextMarkwon.text = wrapper.cleanedFromImages.toHtml()
                        mBodyTextMarkwon.movementMethod = GolosMovementMethod.instance
                    }
                    wrapper.type == ItemType.PLAIN_WITH_IMAGE -> {
                        mMainImageBig.visibility = View.GONE
                        mSecondaryImage.visibility = View.GONE
                        mBodyTextMarkwon.visibility = View.VISIBLE
                        val options = RequestOptions()
                        options.centerInside()
                        mSecondaryImage.setImageBitmap(null)
                        /*if (wrapper.images.size > 0) {
                            mGlide.load(wrapper.images[0])
                                    .apply(options)
                                    .error(mGlide.load(errorDrawable))
                                    .into(mSecondaryImage)
                        }*/
                        mBodyTextMarkwon.text = wrapper.cleanedFromImages.toHtml()
                        mBodyTextMarkwon.movementMethod = GolosMovementMethod.instance
                    }
                    else -> {
                        val error = mGlide.load(errorDrawable)
                        mBodyTextMarkwon.text = ""
                        var nextImage: RequestBuilder<Drawable>? = null
                        if (wrapper.images.size > 1) {
                            nextImage = mGlide.load(wrapper.images[1]).error(error)
                        }
                        mMainImageBig.setImageDrawable(errorDrawable)
                        mMainImageBig.scaleType = ImageView.ScaleType.FIT_CENTER
                        mMainImageBig.visibility = View.VISIBLE
                        mSecondaryImage.visibility = View.GONE
                        mBodyTextMarkwon.visibility = View.GONE
                        if (wrapper.images.size > 0) {
                            mGlide.load(wrapper.images[0])
                                    .error(nextImage ?: error)
                                    .apply(RequestOptions.placeholderOf(errorDrawable))
                                    .into(mMainImageBig)
                        } else {
                            val image = wrapper.parts.find { it is ImageRow }
                            image?.let {
                                mGlide.load((it as ImageRow).src)
                                        .error(nextImage ?: error)
                                        .apply(RequestOptions.placeholderOf(errorDrawable))
                                        .into(mMainImageBig)
                            }
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
        @JvmStatic
        var noAvatarDrawable: Drawable? = null
        @JvmStatic
        var userNotvotedDrarawble: Drawable? = null
        @JvmStatic
        var userVotedvotedDrarawble: Drawable? = null
        @JvmStatic
        var errorDrawable: Drawable? = null
    }
}