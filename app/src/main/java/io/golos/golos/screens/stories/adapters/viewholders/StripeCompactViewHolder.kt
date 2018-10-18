package io.golos.golos.screens.stories.adapters.viewholders

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.golos.golos.R
import io.golos.golos.screens.editor.knife.KnifeParser
import io.golos.golos.screens.editor.knife.SpanFactory
import io.golos.golos.screens.stories.adapters.StripeWrapper
import io.golos.golos.screens.widgets.HolderClickListener
import io.golos.golos.utils.createGolosSpan
import io.golos.golos.utils.getVectorDrawable
import io.golos.golos.utils.setViewGone

class StripeCompactViewHolder(parent: ViewGroup,
                              private val onUpvoteClick: HolderClickListener,
                              private val onDownVoteClick: HolderClickListener,
                              private val onRebloggerClick: HolderClickListener,
                              private val onCardClick: HolderClickListener,
                              private val onBlogClick: HolderClickListener,
                              private val onUserClick: HolderClickListener,
                              private val onAvatarClick: HolderClickListener,
                              private val onUpVotersClick: HolderClickListener,
                              private val onDowVotersClick: HolderClickListener)
    : StoriesViewHolder(R.layout.vh_stripe_compact_size, parent), SpanFactory {
    override fun <T : Any?> produceOfType(type: Class<*>): T {
        return itemView.context.createGolosSpan(type)
    }

    private val mUserNameTv: TextView = itemView.findViewById(R.id.user_name)
    private val mRebloggedByTv: TextView = itemView.findViewById(R.id.reblogged_tv)
    private val mBlogNameTv: TextView = itemView.findViewById(R.id.blog_name_tv)
    private val mTitleTv: TextView = itemView.findViewById(R.id.title)
    private val mTextView: TextView = itemView.findViewById(R.id.tv)
    private val mMainImageBig: ImageView = itemView.findViewById(R.id.image)
    private val mUpVotesCountTv: TextView = itemView.findViewById(R.id.upvote_btn)
    private val mUpvoteIv: ImageView = itemView.findViewById(R.id.upvote_ibtn)
    private val mDownVotersCountTv: TextView = itemView.findViewById(R.id.down_vote_btn)
    private val mDownVoteIv: ImageView = itemView.findViewById(R.id.down_vote_ibtn)
    private val mMoneyTv: TextView = itemView.findViewById(R.id.money_tv)
    private val mTimeTv: TextView = itemView.findViewById(R.id.time_tv)
    private val mVotingProgress: ProgressBar = itemView.findViewById(R.id.progress_upvote)
    private val mDownVotingProgress: ProgressBar = itemView.findViewById(R.id.progress_downvote)

    private val mUserNick: TextView = itemView.findViewById(R.id.user_nick_name_tv)

    private val mDelimeter: View = itemView.findViewById(R.id.delimeter)
    private val mAvatar: ImageView = itemView.findViewById(R.id.avatar_iv)


    init {

        if (errorDrawableS == null) errorDrawableS = ContextCompat.getDrawable(itemView.context, R.drawable.error_jpeg)!!
        mRebloggedByTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)

        mUpVotesCountTv.setOnClickListener { onUpVotersClick.onClick(this) }
        mDownVotersCountTv.setOnClickListener { onDowVotersClick.onClick(this) }
        mUpvoteIv.setOnClickListener { onUpvoteClick.onClick(this) }
        mDownVoteIv.setOnClickListener { onDownVoteClick.onClick(this) }
        mBlogNameTv.setOnClickListener { onBlogClick.onClick(this) }
        mUserNameTv.setOnClickListener { onUserClick.onClick(this) }
        mUserNick.setOnClickListener { onUserClick.onClick(this) }
        mAvatar.setOnClickListener { onAvatarClick.onClick(this) }
        mMoneyTv.setOnClickListener { onUpVotersClick.onClick(this) }
        mMainImageBig.setOnClickListener { onCardClick.onClick(this) }
        itemView.setOnClickListener { onCardClick.onClick(this) }
        mRebloggedByTv.setOnClickListener { onUserClick.onClick(this) }
    }

    override fun handlerStateChange(newState: StripeWrapper?, oldState: StripeWrapper?) {
        super.handlerStateChange(newState, oldState)

        val wrapper = newState?.stripe

        if (newState != null && wrapper != null) {

            val reblogger = wrapper.story.rebloggedBy
            if (reblogger.isEmpty()) {
                mUserNameTv.setOnClickListener { onUserClick.onClick(this) }
                mAvatar.setOnClickListener { onAvatarClick.onClick(this) }
            } else {
                mUserNameTv.setOnClickListener { onRebloggerClick.onClick(this) }
                mAvatar.setOnClickListener { onRebloggerClick.onClick(this) }
            }


            var htmlString = newState.stripe.asHtmlString
            if (htmlString != null) {
                if (htmlString.length > 200) htmlString.substring(0..200)
                mTextView.text = htmlString

            } else {
                htmlString = KnifeParser.fromHtml(wrapper.story.cleanedFromImages.substring(0,
                        if (wrapper.story.cleanedFromImages.length > 400) 400 else wrapper.story.cleanedFromImages.length), this)
                newState.stripe.asHtmlString = htmlString
                mTextView.text = htmlString
            }
        }
    }

    override fun handleImagePlacing(newState: StripeWrapper?, imageView: ImageView) {
//        super.handleImagePlacing(newState, imageView)
//        if (newState?.stripe?.story?.images?.isEmpty() == true) {
//            mMainImageBig.setViewGone()
//        }
    }

    override fun getMoneyText(): TextView {
        return mMoneyTv
    }

    override fun getDownVoteText(): TextView {
        return mDownVotersCountTv
    }

    override fun getAuthorAvatar(): ImageView {
        return mAvatar
    }

    override fun getErrorDrawable(): Drawable {
        return errorDrawableS!!
    }

    override fun getMainText(): TextView {
        return mTextView
    }

    override fun getUserNickTv(): TextView {
        return mUserNick
    }

    override fun getDateStampText(): TextView {
        return mTimeTv
    }

    override fun getMainImage(): ImageView {
        return mMainImageBig
    }

    override fun getTitleTv(): TextView {
        return mTitleTv

    }

    override fun getDownVotwProgress(): View {
        return mDownVotingProgress
    }

    override fun getUpvoteProgress(): View {
        return mVotingProgress
    }

    override fun getCommentsTv(): TextView? {
        return null
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
        return mUpVotesCountTv
    }

    override fun showImageIfNotImageFirst(): Boolean {
        return true
    }

    override fun getUpvoteIv(): ImageView {
        return mUpvoteIv
    }

    override fun getDownvoteIv(): ImageView {
        return mDownVoteIv
    }

    companion object {

        @JvmStatic
        var errorDrawableS: Drawable? = null
        @JvmStatic
        var userVotedvotedDrarawble: Drawable? = null
    }
}