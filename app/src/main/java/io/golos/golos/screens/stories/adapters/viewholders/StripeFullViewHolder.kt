package io.golos.golos.screens.stories.adapters.viewholders

import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import io.golos.golos.R
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.screens.stories.adapters.StripeWrapper
import io.golos.golos.screens.widgets.HolderClickListener
import io.golos.golos.utils.*

class StripeFullViewHolder(parent: ViewGroup,
                           private val onUpvoteClick: HolderClickListener,
                           private val onCardClick: HolderClickListener,
                           private val onCommentsClick: HolderClickListener,
                           private val onShareClick: HolderClickListener,
                           private val onBlogClick: HolderClickListener,
                           private val onUserClick: HolderClickListener,
                           private val onVotersClick: HolderClickListener
) : StoriesViewHolder(R.layout.vh_stripe_full_size, parent) {
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
        if (errorDrawableS == null) errorDrawableS = ContextCompat.getDrawable(itemView.context, R.drawable.error)!!

        mRebloggedByTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)
        mBlogNameTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_bullet_10dp), null, null, null)
        mVotersBtn.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_person_gray_20dp), null, null, null)

        mCommentsButton.setOnClickListener({ onCommentsClick.onClick(this) })
        mShareBtn.setOnClickListener({ onShareClick.onClick(this) })
        mUpvoteBtn.setOnClickListener({ onUpvoteClick.onClick(this) })
        mBlogNameTv.setOnClickListener({ onBlogClick.onClick(this) })
        mAvatar.setOnClickListener({ onUserClick.onClick(this) })
        mUserNameTv.setOnClickListener({ onUserClick.onClick(this) })
        mUpvoteBtn.setOnClickListener({ onUpvoteClick.onClick(this) })
        mVotersBtn.setOnClickListener({ onVotersClick.onClick(this) })

        mTitleTv.setOnClickListener({ onCardClick.onClick(this) })
        mBodyTextMarkwon.setOnClickListener({ onCardClick.onClick(this) })
        mMainImageBig.setOnClickListener({ onCardClick.onClick(this) })
        itemView.setOnClickListener({ onCardClick.onClick(this) })
        mBodyTextMarkwon.movementMethod = GolosMovementMethod.instance
    }

    override fun handlerStateChange(newState: StripeWrapper?, oldState: StripeWrapper?) {
        super.handlerStateChange(newState, oldState)
        if (newState != null) {

            val wrapper = newState.stripe.rootStory() ?: return

            if (wrapper.avatarPath != null) mGlide
                    .load(ImageUriResolver.resolveImageWithSize(
                            wrapper.avatarPath ?: "",
                            wantedwidth = mAvatar.width))
                    .error(mGlide.load(noAvatarDrawable))
                    .into(mAvatar)
            else mAvatar.setImageDrawable(noAvatarDrawable)
            if (wrapper.userVotestatus == GolosDiscussionItem.UserVoteType.VOTED) {
                if (mUpvoteBtn.tag == null || mUpvoteBtn.tag != "green") {
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(userVotedvotedDrarawble, null, null, null)
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.upvote_green))
                    mUpvoteBtn.tag = "green"
                }

            } else {
                if (mUpvoteBtn.tag == null || mUpvoteBtn.tag != "gray") {
                    mUpvoteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_triangle_in_cricle_gray_outline_20dp), null, null, null)
                    mUpvoteBtn.setTextColor(ContextCompat.getColor(itemView.context, R.color.textColorP))
                    mUpvoteBtn.tag = "gray"
                }
            }

            if (newState.stripe.storyWithState()?.updatingState == UpdatingState.UPDATING) {
                mVotingProgress.setViewVisible()
                mUpvoteBtn.setViewGone()
            } else {
                mVotingProgress.setViewGone()
                mUpvoteBtn.setViewVisible()
            }
            mVotersBtn.text = wrapper.votesNum.toString()
            if (newState.stripe.rootStory()?.type != GolosDiscussionItem.ItemType.IMAGE_FIRST) {
                mMainImageBig.setViewGone()
                mMainImageBig.setImageDrawable(null)
                var htmlString = newState.stripe.storyWithState()?.asHtmlString
                if (htmlString != null) {
                    if (htmlString.length > 400) htmlString.substring(0..400)
                    mBodyTextMarkwon.text = htmlString

                } else {
                    htmlString = wrapper.cleanedFromImages.substring(0,
                            if (wrapper.cleanedFromImages.length > 400) 400 else wrapper.cleanedFromImages.length).toHtml()
                    newState.stripe.storyWithState()?.asHtmlString = htmlString
                    mBodyTextMarkwon.text = htmlString
                }
                mBodyTextMarkwon.setViewVisible()
            } else {
                mBodyTextMarkwon.setViewGone()
            }
        }
    }

    override fun setUpTheme() {
        super.setUpTheme()
        mCommentsButton.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_chat_gray_20dp), null, null, null)
        mBodyTextMarkwon.setTextColorCompat(R.color.text_color_white_black)
    }

    override fun getMainText(): TextView? {
        return mBodyTextMarkwon
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
        return mUpvoteBtn
    }

    override fun showImageIfNotImageFirst(): Boolean {
        return false
    }

    companion object {

        @JvmStatic
        var noAvatarDrawable: Drawable? = null
        @JvmStatic
        var userVotedvotedDrarawble: Drawable? = null
        @JvmStatic
        var errorDrawableS: Drawable? = null
    }
}