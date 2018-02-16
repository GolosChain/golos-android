package io.golos.golos.screens.stories.adapters.viewholders

import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import io.golos.golos.R
import io.golos.golos.screens.stories.adapters.StripeWrapper
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.utils.*

class StripeCompactViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(this.inflate(parent)) {
    private val mUserNameTv: TextView = itemView.findViewById(R.id.user_name)
    private val mRebloggedByTv: TextView = itemView.findViewById(R.id.reblogged_tv)
    private val mBlogNameTv: TextView = itemView.findViewById(R.id.blog_name_tv)
    private val mTitleTv: TextView = itemView.findViewById(R.id.title)
    private val mMainImageBig: ImageView = itemView.findViewById(R.id.image)
    private val mUpvoteValue: TextView = itemView.findViewById(R.id.vote_value)
    private val mUpvoteIv: ImageView = itemView.findViewById(R.id.vote_iv)
    private val mVotingProgress: ProgressBar = itemView.findViewById(R.id.progress)
    private val mCommentsButton: TextView = itemView.findViewById(R.id.comments_count_tv)
    private val mCommentsIv: ImageView = itemView.findViewById(R.id.comments_iv)
    private val mDelimeter: View = itemView.findViewById(R.id.delimeter)


    private val mGlide = Glide.with(parent.context)

    init {
        if (userVotedvotedDrarawble == null) userVotedvotedDrarawble = itemView.getVectorDrawable(R.drawable.ic_triangle_in_circle_green_outline_20dp)
        if (errorDrawable == null) errorDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.error_jpeg)!!
        mRebloggedByTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)
        mBlogNameTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_bullet_10dp), null, null, null)
    }

    var state: StripeWrapper? = null
        set(value) {
            field = value
            if (field != null) {
                val wrapper = field?.stripe?.rootStory() ?: return

                setUpTheme()

                mCommentsButton.setOnClickListener({ field!!.onCommentsClick(this) })
                mCommentsIv.setOnClickListener({ field!!.onCommentsClick(this) })
                mUpvoteValue.setOnClickListener({ field!!.onUpvoteClick(this) })
                mUpvoteIv.setOnClickListener({ field!!.onUpvoteClick(this) })
                mBlogNameTv.setOnClickListener({ field!!.onBlogClick(this) })
                mUserNameTv.setOnClickListener({ field!!.onUserClick(this) })

                mTitleTv.setOnClickListener({ field!!.onCardClick(this) })
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
                mUpvoteValue.text = "$${String.format("%.3f", wrapper.payoutInDollars)}"

                mCommentsButton.text = wrapper.commentsCount.toString()

                if (wrapper.isUserUpvotedOnThis) {
                    mUpvoteIv.setImageDrawable(userVotedvotedDrarawble)
                    mUpvoteValue.setTextColor(ContextCompat.getColor(itemView.context, R.color.upvote_green))
                } else {
                    mUpvoteIv.setImageDrawable(itemView.getVectorDrawable(R.drawable.ic_triangle_in_cricle_gray_outline_20dp))
                    mUpvoteValue.setTextColor(ContextCompat.getColor(itemView.context, R.color.textColorP))
                }
                if (field?.stripe?.storyWithState()?.updatingState == UpdatingState.UPDATING) {
                    mVotingProgress.visibility = View.VISIBLE
                    mUpvoteValue.visibility = View.GONE
                    mUpvoteIv.visibility = View.GONE
                } else {
                    mVotingProgress.visibility = View.GONE
                    mUpvoteValue.visibility = View.VISIBLE
                    mUpvoteIv.visibility = View.VISIBLE
                }

                if (field?.isImagesShown == false) {
                    mMainImageBig.setViewGone()
                    return
                }

                if (field?.stripe?.rootStory()?.images?.isEmpty() != false) {
                    mMainImageBig.setViewGone()
                } else {
                    mMainImageBig.setViewVisible()

                    val image = (if (wrapper.images.size > 0) wrapper.images[0] else (wrapper
                            .parts
                            .find { it is ImageRow })
                            ?.let { (it as ImageRow).src }) ?: return

                    val error = mGlide.load(R.drawable.error_jpeg)
                    var nextImage: RequestBuilder<Drawable>? = null
                    if (wrapper.images.size > 1) {
                        nextImage = mGlide.load(wrapper.images[1]).error(error)
                    }

                    mGlide.load(image)
                            .error(nextImage ?: error)
                            .apply(RequestOptions()
                                    .placeholder(errorDrawable)
                                    .centerCrop()
                            )
                            .into(mMainImageBig)
                }
            }
        }

    private fun setUpTheme() {
        mCommentsIv.setImageDrawable(itemView.getVectorDrawable(R.drawable.ic_chat_gray_20dp))
        mUpvoteValue.setTextColorCompat(R.color.textColorP)
        mCommentsButton.setTextColorCompat(R.color.textColorP)
        mTitleTv.setTextColorCompat(R.color.stripe_title)
        mUserNameTv.setTextColorCompat(R.color.textColorP)
        mRebloggedByTv.setTextColorCompat(R.color.stripe_subtitle)
        mBlogNameTv.setTextColorCompat(R.color.stripe_subtitle)
        mDelimeter.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.delimeter_color_feed))
    }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_stripe_compact_size, parent, false)
        @JvmStatic
        var errorDrawable: Drawable? = null

        @JvmStatic
        var userVotedvotedDrarawble: Drawable? = null
    }
}