package io.golos.golos.screens.stories.adapters.viewholders

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.golos.golos.R
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.screens.editor.knife.KnifeParser
import io.golos.golos.screens.editor.knife.SpanFactory
import io.golos.golos.screens.stories.adapters.StripeWrapper
import io.golos.golos.screens.widgets.HolderClickListener
import io.golos.golos.utils.*

class StripeFullViewHolder(parent: ViewGroup,
                           private val onUpvoteClick: HolderClickListener,
                           private val onDownVoteClick: HolderClickListener,
                           private val onReblogClick: HolderClickListener,
                           private val onCardClick: HolderClickListener,
                           private val onCommentsClick: HolderClickListener,
                           private val onBlogClick: HolderClickListener,
                           private val onAvatar: HolderClickListener,
                           private val onRebloggerClick: HolderClickListener,
                           private val onUpVotersClick: HolderClickListener,
                           private val onDownVotersClick: HolderClickListener
) : StoriesViewHolder(R.layout.vh_stripe_full_size, parent), SpanFactory {
    override fun <T : Any?> produceOfType(type: Class<*>): T {
        return itemView.context.createGolosSpan(type)
    }

    private val mAvatar: ImageView = itemView.findViewById(R.id.avatar_iv)
    private val mUserNameTv: TextView = itemView.findViewById(R.id.user_name)
    private val mRebloggedByTv: TextView = itemView.findViewById(R.id.reblogged_tv)
    private val mBlogNameTv: TextView = itemView.findViewById(R.id.blog_name_tv)
    private val mTitleTv: TextView = itemView.findViewById(R.id.title)
    private val mBodyTextMarkwon: TextView = itemView.findViewById(R.id.text)
    private val mMainImageBig: ImageView = itemView.findViewById(R.id.image_main)
    private val mUpvoteBtn: TextView = itemView.findViewById(R.id.upvote_btn)
    private val mUpvoteIv: ImageView = itemView.findViewById(R.id.upvote_ibtn)
    private val mDownVoteBtn: TextView = itemView.findViewById(R.id.down_vote_btn)
    private val mDownVoteIv: ImageView = itemView.findViewById(R.id.dizlike_ibtn)
    private val mMoneyTv: TextView = itemView.findViewById(R.id.money_tv)
    private val mReblog: View = itemView.findViewById(R.id.reblog_ibtn)
    private val mVotingProgress: ProgressBar = itemView.findViewById(R.id.progress_upvote)
    private val mDownVotingProgress: ProgressBar = itemView.findViewById(R.id.progress_downvote)
    private val mCommentsButton: TextView = itemView.findViewById(R.id.comments_tv)
    private val mTimeTv: TextView = itemView.findViewById(R.id.time_tv)
    private val mNickNameTv: TextView = itemView.findViewById(R.id.user_nick_name_tv)

    private val mDelimeter: View = itemView.findViewById(R.id.delimeter)


    init {
        if (errorDrawableS == null) errorDrawableS = ContextCompat.getDrawable(itemView.context, R.drawable.error)!!
        mRebloggedByTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)

        mCommentsButton.setOnClickListener { onCommentsClick.onClick(this) }
        mDownVoteBtn.setOnClickListener { onDownVoteClick.onClick(this) }
        mReblog.setOnClickListener { onReblogClick.onClick(this) }

        mRebloggedByTv.setOnClickListener { onRebloggerClick.onClick(this) }
        mUpvoteBtn.setOnClickListener { onUpVotersClick.onClick(this) }
        mUpvoteIv.setOnClickListener { onUpvoteClick.onClick(this) }
        mBlogNameTv.setOnClickListener { onBlogClick.onClick(this) }
        mAvatar.setOnClickListener { onAvatar.onClick(this) }
        mUserNameTv.setOnClickListener { onAvatar.onClick(this) }
        mNickNameTv.setOnClickListener { onAvatar.onClick(this) }
        mDownVoteIv.setOnClickListener { onDownVoteClick.onClick(this) }
        mDownVoteBtn.setOnClickListener { onDownVotersClick.onClick(this) }
        mMainImageBig.setOnClickListener {
            onCardClick.onClick(this)
        }
        itemView.setOnClickListener { onCardClick.onClick(this) }
    }

    override fun handlerStateChange(newState: StripeWrapper?, oldState: StripeWrapper?) {
        super.handlerStateChange(newState, oldState)
        if (newState != null) {

            val wrapper = newState.stripe

            val reblogger = wrapper.story.rebloggedBy
            if (reblogger.isEmpty()) {
                mUserNameTv.setOnClickListener { onAvatar.onClick(this) }
                mNickNameTv.setOnClickListener { onAvatar.onClick(this) }
                mAvatar.setOnClickListener { onAvatar.onClick(this) }
            } else {
                mUserNameTv.setOnClickListener { onRebloggerClick.onClick(this) }
                mAvatar.setOnClickListener { onRebloggerClick.onClick(this) }
                mRebloggedByTv.setOnClickListener { onRebloggerClick.onClick(this) }
                mNickNameTv.setOnClickListener { onAvatar.onClick(this) }
            }
            if (newState.stripe.story.type != GolosDiscussionItem.ItemType.IMAGE_FIRST) {
                mMainImageBig.setViewGone()
                mMainImageBig.setImageDrawable(null)
                var htmlString = newState.stripe.asHtmlString
                if (htmlString != null) {
                    if (htmlString.length > 400) htmlString.substring(0..400)
                    mBodyTextMarkwon.text = htmlString

                } else {
                    htmlString = KnifeParser.fromHtml(wrapper.story.cleanedFromImages.substring(0,
                            if (wrapper.story.cleanedFromImages.length > 400) 400 else wrapper.story.cleanedFromImages.length), this)
                    newState.stripe.asHtmlString = htmlString
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
        mCommentsButton.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_comments_20dp), null, null, null)
        mBodyTextMarkwon.setTextColorCompat(R.color.text_color_white_black)
    }

    override fun getAuthorAvatar(): ImageView {
        return mAvatar
    }

    override fun getMainText(): TextView {
        return mBodyTextMarkwon
    }

    override fun getErrorDrawable(): Drawable {
        return errorDrawableS!!
    }

    override fun getMoneyText(): TextView {
        return mMoneyTv
    }

    override fun getDownVoteText(): TextView {
        return mDownVoteBtn
    }

    override fun getMainImage(): ImageView {
        return mMainImageBig
    }

    override fun getUpvoteIv(): ImageView {
        return mUpvoteIv
    }

    override fun getDownVotwProgress(): View {
        return mDownVotingProgress
    }

    override fun getUpvoteProgress(): View {
        return mVotingProgress
    }

    override fun getDownvoteIv(): ImageView {
        return mDownVoteIv
    }

    override fun getTitleTv(): TextView {
        return mTitleTv
    }

    override fun getCommentsTv(): TextView {
        return mCommentsButton
    }

    override fun getShownUserNameTv(): TextView {
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

    override fun getUserNickTv(): TextView {
        return mNickNameTv
    }

    override fun getDateStampText(): TextView {
        return mTimeTv
    }

    companion object {

        @JvmStatic
        var noAvatarDrawable: Drawable? = null

        @JvmStatic
        var errorDrawableS: Drawable? = null
    }
}