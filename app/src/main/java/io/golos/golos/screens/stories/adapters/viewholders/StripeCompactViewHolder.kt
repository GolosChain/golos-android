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
import io.golos.golos.R
import io.golos.golos.screens.stories.adapters.StripeWrapper
import io.golos.golos.utils.UpdatingState
import io.golos.golos.utils.getVectorDrawable
import io.golos.golos.utils.setTextColorCompat
import io.golos.golos.utils.setViewGone

class StripeCompactViewHolder(parent: ViewGroup,
                              private val onUpvoteClick: (RecyclerView.ViewHolder) -> Unit,
                              private val onCardClick: (RecyclerView.ViewHolder) -> Unit,
                              private val onCommentsClick: (RecyclerView.ViewHolder) -> Unit,
                              private val onShareClick: (RecyclerView.ViewHolder) -> Unit,
                              private val onBlogClick: (RecyclerView.ViewHolder) -> Unit,
                              private val onUserClick: (RecyclerView.ViewHolder) -> Unit,
                              private val onVotersClick: (RecyclerView.ViewHolder) -> Unit)
    : StoriesViewHolder(R.layout.vh_stripe_compact_size, parent) {
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


    init {
        if (userVotedvotedDrarawble == null) userVotedvotedDrarawble = itemView.getVectorDrawable(R.drawable.ic_triangle_in_circle_green_outline_20dp)
        if (errorDrawableS == null) errorDrawableS = ContextCompat.getDrawable(itemView.context, R.drawable.error_jpeg)!!
        mRebloggedByTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)
        mBlogNameTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_bullet_10dp), null, null, null)

        mCommentsButton.setOnClickListener({ onCommentsClick(this) })
        mCommentsIv.setOnClickListener({ onCommentsClick(this) })
        mUpvoteValue.setOnClickListener({ onUpvoteClick(this) })
        mUpvoteIv.setOnClickListener({ onUpvoteClick(this) })
        mBlogNameTv.setOnClickListener({ onBlogClick(this) })
        mUserNameTv.setOnClickListener({ onUserClick(this) })

        mTitleTv.setOnClickListener({ onCardClick(this) })
        mMainImageBig.setOnClickListener({ onCardClick(this) })
        itemView.setOnClickListener({ onCardClick(this) })
    }

    override fun handlerStateChange(newState: StripeWrapper?, oldState: StripeWrapper?) {
        super.handlerStateChange(newState, oldState)

        val wrapper = newState?.stripe?.rootStory()

        if (newState != null && wrapper != null) {

            if (wrapper.isUserUpvotedOnThis) {
                mUpvoteIv.setImageDrawable(userVotedvotedDrarawble)
                mUpvoteValue.setTextColorCompat(R.color.upvote_green)
            } else {
                mUpvoteIv.setImageDrawable(itemView.getVectorDrawable(R.drawable.ic_triangle_in_cricle_gray_outline_20dp))
                mUpvoteValue.setTextColorCompat(R.color.textColorP)
            }
            if (newState.stripe.storyWithState()?.updatingState == UpdatingState.UPDATING) {
                mVotingProgress.visibility = View.VISIBLE
                mUpvoteValue.visibility = View.GONE
                mUpvoteIv.visibility = View.GONE
            } else {
                mVotingProgress.visibility = View.GONE
                mUpvoteValue.visibility = View.VISIBLE
                mUpvoteIv.visibility = View.VISIBLE
            }
        }
    }

    override fun handleImagePlacing(newState: StripeWrapper?, imageView: ImageView) {
        super.handleImagePlacing(newState, imageView)
        if (newState?.stripe?.rootStory()?.images?.isEmpty() == true){
            mMainImageBig.setViewGone()
        }
    }

    override fun getErrorDrawable(): Drawable {
        return errorDrawableS!!
    }

    override fun getMainImage(): ImageView {
        return mMainImageBig
    }

    override fun getTitleTv(): TextView {
        return mTitleTv
    }

    override fun getCommentsTv(): TextView {
        return mCommentsButton
    }

    override fun getUserNameTv(): TextView {
        return mUserNameTv
    }

    override fun getReblogedByTv(): TextView {
        return mRebloggedByTv
    }

    override fun getBlogNameTv(): TextView {
        return mBlogNameTv
    }

    override fun getDelimeterV(): View {
        return mDelimeter
    }

    override fun getUpvoteText(): TextView {
        return mUpvoteValue
    }

    override fun showImageIfNotImageFirst(): Boolean {
        return true
    }

    override fun setUpTheme() {
        super.setUpTheme()
        mCommentsIv.setImageDrawable(itemView.getVectorDrawable(R.drawable.ic_chat_gray_20dp))
    }

    companion object {

        @JvmStatic
        var errorDrawableS: Drawable? = null
        @JvmStatic
        var userVotedvotedDrarawble: Drawable? = null
    }
}