package io.golos.golos.screens.stories.adapters.viewholders

import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
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
import io.golos.golos.screens.stories.adapters.StripeWrapper
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.utils.*

class StripeFullViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(this.inflate(parent)) {
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
    private val mDelimeter: View = itemView.findViewById(R.id.delimeter)

    private val mGlide = Glide.with(parent.context)

    init {
        if (noAvatarDrawable == null) noAvatarDrawable = itemView.getVectorDrawable(R.drawable.ic_person_gray_32dp)

        if (userVotedvotedDrarawble == null) userVotedvotedDrarawble = itemView.getVectorDrawable(R.drawable.ic_triangle_in_circle_green_outline_20dp)
        if (errorDrawable == null) errorDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.error)!!

        mRebloggedByTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)
        mBlogNameTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_bullet_10dp), null, null, null)
        mVotersBtn.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_person_gray_20dp), null, null, null)

    }

    var state: StripeWrapper? = null
        set(value) {
            field = value
            if (field != null) {
                val wrapper = field?.stripe?.rootStory() ?: return
                setUpTheme()
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
                        .apply(RequestOptions.placeholderOf(noAvatarDrawable)
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                        .into(mAvatar)
                else mAvatar.setImageDrawable(noAvatarDrawable)


                if (wrapper.isUserUpvotedOnThis) {
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(userVotedvotedDrarawble, null, null, null)
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.upvote_green))
                } else {
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_triangle_in_cricle_gray_outline_20dp), null, null, null)
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.textColorP))
                }
                if (field?.stripe?.storyWithState()?.updatingState == UpdatingState.UPDATING) {
                    mVotingProgress.visibility = View.VISIBLE
                    mUpvoteBtn.visibility = View.GONE
                } else {
                    mVotingProgress.visibility = View.GONE
                    mUpvoteBtn.visibility = View.VISIBLE
                }
                mVotersBtn.text = value?.stripe?.rootStory()?.votesNum?.toString() ?: ""

                if (field?.isImagesShown == false){
                    mMainImageBig.setViewGone()
                    mBodyTextMarkwon.setViewVisible()
                    mBodyTextMarkwon.text = wrapper.cleanedFromImages.toHtml()
                    mBodyTextMarkwon.movementMethod = GolosMovementMethod.instance
                    return
                }

                if (wrapper.type == ItemType.PLAIN || wrapper.type == ItemType.PLAIN_WITH_IMAGE) {
                    mMainImageBig.visibility = View.GONE
                    mBodyTextMarkwon.visibility = View.VISIBLE
                    mBodyTextMarkwon.text = wrapper.cleanedFromImages.toHtml()
                    mBodyTextMarkwon.movementMethod = GolosMovementMethod.instance
                } else if (wrapper.type == ItemType.IMAGE_FIRST) {
                    val error = mGlide.load(R.drawable.error)
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

            }
        }

    private fun setUpTheme() {
        mCommentsButton.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_chat_gray_20dp), null, null, null)
        mVotersBtn.setTextColorCompat(R.color.textColorP)
        mCommentsButton.setTextColorCompat(R.color.textColorP)
        mTitleTv.setTextColorCompat(R.color.stripe_title)
        mBodyTextMarkwon.setTextColorCompat(R.color.stripe_text_color)
        mUserNameTv.setTextColorCompat(R.color.textColorP)
        mRebloggedByTv.setTextColorCompat(R.color.stripe_subtitle)
        mBlogNameTv.setTextColorCompat(R.color.stripe_subtitle)
        mDelimeter.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.delimeter_color_feed))
    }

    companion object {
        fun inflate(parent: ViewGroup): View = LayoutInflater.from(parent.context).inflate(R.layout.vh_stripe_full_size, parent, false)
        @JvmStatic
        var noAvatarDrawable: Drawable? = null

        @JvmStatic
        var userVotedvotedDrarawble: Drawable? = null
        @JvmStatic
        var errorDrawable: Drawable? = null
    }
}