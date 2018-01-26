package io.golos.golos.screens.stories.adapters

import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.repository.model.ItemType
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.utils.*
import java.util.concurrent.Executors


data class StripeWrapper(val stripe: StoryWithComments,
                         val onUpvoteClick: (RecyclerView.ViewHolder) -> Unit,
                         val onCardClick: (RecyclerView.ViewHolder) -> Unit,
                         val onCommentsClick: (RecyclerView.ViewHolder) -> Unit,
                         val onShareClick: (RecyclerView.ViewHolder) -> Unit,
                         val onBlogClick: (RecyclerView.ViewHolder) -> Unit,
                         val onUserClick: (RecyclerView.ViewHolder) -> Unit,
                         val onVotersClick: (RecyclerView.ViewHolder) -> Unit)

class StoriesRecyclerAdapter(private var onCardClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onCommentsClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onShareClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onUpvoteClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onTagClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onUserClick: (StoryWithComments) -> Unit = { print(it) },
                             private var onVotersClick: (StoryWithComments) -> Unit = { print(it) })
    : RecyclerView.Adapter<StripeViewHolder>() {

    companion object {
        @JvmStatic
        private val workingExecutor = Executors.newSingleThreadExecutor()
    }

    private var mStripes = ArrayList<StoryWithComments>()
    private val mItemsMap = HashMap<Long, Int>()
    private val handler = Handler()


    fun setStripesCustom(newItems: List<StoryWithComments>) {
        if (mStripes.isEmpty()) {
            handler.post {
                mStripes = ArrayList(newItems).clone() as ArrayList<StoryWithComments>
                notifyDataSetChanged()
                mStripes.forEach {
                    mItemsMap.put(it.rootStory()?.id ?: 0L, it.hashCode())
                }
            }

        } else {
            workingExecutor.execute {
                try {

                    val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                            if (mStripes == null
                                    || newItems == null
                                    || mStripes.isEmpty()
                                    || newItems.isEmpty()
                                    || mStripes.lastIndex < oldItemPosition
                                    || newItems.size < newItemPosition) return false
                            return mStripes[oldItemPosition].rootStory()?.id == newItems[newItemPosition].rootStory()?.id
                        }

                        override fun getOldListSize() = mStripes.size
                        override fun getNewListSize() = newItems.size
                        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                            if (mStripes == null
                                    || newItems == null
                                    || mStripes.isEmpty()
                                    || newItems.isEmpty()
                                    || mStripes.lastIndex < oldItemPosition
                                    || newItems.size < newItemPosition) return false
                            val oldHash = mItemsMap[mStripes[oldItemPosition].rootStory()?.id ?: 0L]
                            return oldHash == newItems[newItemPosition].rootStory()?.hashCode()
                        }
                    })
                    handler.post {
                        result.dispatchUpdatesTo(this)
                        mStripes = ArrayList(newItems)
                        mStripes.forEach {
                            mItemsMap.put(it.rootStory()?.id ?: 0L, it.hashCode())
                        }
                    }
                } catch (e: Exception) {
                    handler.post {
                        e.printStackTrace()
                        mStripes = ArrayList(newItems)
                        notifyDataSetChanged()
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
                onUserClick = { onUserClick.invoke(mStripes[it.adapterPosition]) },
                onVotersClick = { onVotersClick.invoke(mStripes[it.adapterPosition]) })
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
    private val mMainImageBig: ImageView = itemView.findViewById(R.id.image_main)
    private val mUpvoteBtn: TextView = itemView.findViewById(R.id.vote_btn)
    private val mVotingProgress: ProgressBar = itemView.findViewById(R.id.progress)
    private val mCommentsButton: TextView = itemView.findViewById(R.id.comments_btn)
    private val mVotersBtn: TextView = itemView.findViewById(R.id.voters_btn)
    private val mShareBtn: ImageButton = itemView.findViewById(R.id.share_btn)
    private val views = listOf<View>(mAvatar, mUserNameTv, mRebloggedByTv,
            mBlogNameTv, mTitleTv, mBodyTextMarkwon, mMainImageBig, mUpvoteBtn, mCommentsButton, mShareBtn, itemView)
    private val mGlide = Glide.with(parent.context)

    init {
        if (noAvatarDrawable == null) noAvatarDrawable = itemView.getVectorDrawable(R.drawable.ic_person_gray_24dp)
        if (userNotvotedDrarawble == null) userNotvotedDrarawble = itemView.getVectorDrawable(R.drawable.ic_triangle_in_cricle_gray_outline_20dp)
        if (userVotedvotedDrarawble == null) userVotedvotedDrarawble = itemView.getVectorDrawable(R.drawable.ic_triangle_in_circle_green_outline_20dp)
        if (errorDrawable == null) errorDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.error)!!

        mRebloggedByTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)
        mBlogNameTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_bullet_20dp), null, null, null)
        mCommentsButton.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_chat_gray_20dp), null, null, null)
        mVotersBtn.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_person_gray_20dp), null, null, null)
        mUpvoteBtn.setCompoundDrawablesWithIntrinsicBounds(userNotvotedDrarawble, null, null, null)
    }

    var state: StripeWrapper? = null
        set(value) {
            field = value
            if (field != null) {
                val wrapper = field?.stripe?.rootStory() ?: return

                mCommentsButton.setOnClickListener({ field!!.onCommentsClick(this) })
                mShareBtn.setOnClickListener({ field!!.onShareClick(this) })
                mUpvoteBtn.setOnClickListener({ field!!.onUpvoteClick(this) })
                mBlogNameTv.setOnClickListener({ field!!.onBlogClick(this) })
                mAvatar.setOnClickListener({ field!!.onUserClick(this) })
                mUserNameTv.setOnClickListener({ field!!.onUserClick(this) })
                mUpvoteBtn.setOnClickListener({ field!!.onUpvoteClick(this) })
                mVotersBtn.setOnClickListener({ field!!.onVotersClick(this) })

                mTitleTv.setOnClickListener({ field!!.onCardClick(this) })
                mBodyTextMarkwon.setOnClickListener({ field!!.onCardClick(this) })
                mMainImageBig.setOnClickListener({ field!!.onCardClick(this) })
                itemView.setOnClickListener({ field!!.onCardClick(this) })

                mUserNameTv.text = wrapper.author
                if (wrapper.firstRebloggedBy.isNotEmpty()) {
                    mRebloggedByTv.text = wrapper.firstRebloggedBy
                } else {
                    mRebloggedByTv.visibility = View.GONE
                }
                if (wrapper.categoryName.startsWith("ru--")) {
                    mBlogNameTv.text = Translit.lat2Ru(wrapper.categoryName.substring(4))
                } else {
                    mBlogNameTv.text = wrapper.categoryName
                }

                mTitleTv.text = wrapper.title.toLowerCase().capitalize()
                mUpvoteBtn.text = "$ ${String.format("%.3f", wrapper.payoutInDollars)}"

                mCommentsButton.text = wrapper.commentsCount.toString()

                if (wrapper.avatarPath != null) mGlide
                        .load(wrapper.avatarPath)
                        .error(mGlide.load(noAvatarDrawable))
                        .apply(RequestOptions
                                .placeholderOf(noAvatarDrawable)
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                        .into(mAvatar)
                else mAvatar.setImageDrawable(noAvatarDrawable)


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
                mVotersBtn.text = value?.stripe?.rootStory()?.votesNum?.toString() ?: ""

                if (wrapper.type == ItemType.PLAIN || wrapper.type == ItemType.PLAIN_WITH_IMAGE) {
                    mMainImageBig.visibility = View.GONE
                    mBodyTextMarkwon.visibility = View.VISIBLE
                    mBodyTextMarkwon.text = wrapper.cleanedFromImages.toHtml()
                    mBodyTextMarkwon.movementMethod = GolosMovementMethod.instance
                } else if (wrapper.type == ItemType.IMAGE_FIRST) {
                    val error = mGlide.load(errorDrawable)
                    mBodyTextMarkwon.setViewGone()
                    var nextImage: RequestBuilder<Drawable>? = null
                    if (wrapper.images.size > 1) {
                        nextImage = mGlide.load(wrapper.images[1]).error(error)
                    }
                    mMainImageBig.scaleType = ImageView.ScaleType.FIT_CENTER
                    mMainImageBig.visibility = View.VISIBLE

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