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
import io.golos.golos.screens.stories.adapters.StripeWrapper
import io.golos.golos.screens.widgets.HolderClickListener
import io.golos.golos.utils.*

class StripeCompactViewHolder(parent: ViewGroup,
                              private val onUpvoteClick: HolderClickListener,
                              private val onDownVoteClick: HolderClickListener,
                              private val onRebloggerClick: HolderClickListener,
                              private val onCardClick: HolderClickListener,
                              private val onCommentsClick: HolderClickListener,
                              private val onBlogClick: HolderClickListener,
                              private val onUserClick: HolderClickListener,
                              private val onAvatarClick: HolderClickListener,
                              private val onVotersClick: HolderClickListener)
    : StoriesViewHolder(R.layout.vh_stripe_compact_size, parent) {
    private val mUserNameTv: TextView = itemView.findViewById(R.id.user_name)
    private val mRebloggedByTv: TextView = itemView.findViewById(R.id.reblogged_tv)
    private val mBlogNameTv: TextView = itemView.findViewById(R.id.blog_name_tv)
    private val mTitleTv: TextView = itemView.findViewById(R.id.title)
    private val mMainImageBig: ImageView = itemView.findViewById(R.id.image)
    private val mUpvoteBtn: TextView = itemView.findViewById(R.id.upvote_btn)
    private val mDownVoteBtn: TextView = itemView.findViewById(R.id.down_vote_btn)
    private val mMoneyTv: TextView = itemView.findViewById(R.id.money_tv)
    private val mTimeTv: TextView = itemView.findViewById(R.id.time_tv)
    private val mVotingProgress: ProgressBar = itemView.findViewById(R.id.progress_upvote)
    private val mDownVotingProgress: ProgressBar = itemView.findViewById(R.id.progress_downvote)

    private val mMainText: TextView = itemView.findViewById(R.id.main_tv)
    private val mUserNick: TextView = itemView.findViewById(R.id.user_nick_name_tv)

    private val mDelimeter: View = itemView.findViewById(R.id.delimeter)
    private val mAvatar: ImageView = itemView.findViewById(R.id.avatar_iv)


    init {

        if (errorDrawableS == null) errorDrawableS = ContextCompat.getDrawable(itemView.context, R.drawable.error_jpeg)!!
        mRebloggedByTv.setCompoundDrawablesWithIntrinsicBounds(itemView.getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)

        mUpvoteBtn.setOnClickListener { onUpvoteClick.onClick(this) }
        mDownVoteBtn.setOnClickListener { onDownVoteClick.onClick(this) }
        mBlogNameTv.setOnClickListener { onBlogClick.onClick(this) }
        mUserNameTv.setOnClickListener { onUserClick.onClick(this) }
        mUserNick.setOnClickListener { onUserClick.onClick(this) }
        mAvatar.setOnClickListener { onAvatarClick.onClick(this) }
        mMoneyTv.setOnClickListener { onVotersClick.onClick(this) }
        mMainImageBig.setOnClickListener { onCardClick.onClick(this) }
        itemView.setOnClickListener { onCardClick.onClick(this) }
        mRebloggedByTv.setOnClickListener { onUserClick.onClick(this) }
    }

    override fun handlerStateChange(newState: StripeWrapper?, oldState: StripeWrapper?) {
        super.handlerStateChange(newState, oldState)

        val wrapper = newState?.stripe

        if (newState != null && wrapper != null) {


            wrapper.voteUpdatingState?.let { votingState ->
                if (votingState.state == UpdatingState.DONE) {
                    mDownVotingProgress.setViewGone()
                    mVotingProgress.setViewGone()
                    mUpvoteBtn.setViewVisible()
                    mDownVoteBtn.setViewVisible()
                } else {
                    when {
                        votingState.votingStrength < 0 -> {//downvoting progress
                            mDownVotingProgress.setViewVisible()
                            mDownVoteBtn.setViewInvisible()

                            mVotingProgress.setViewGone()
                            mUpvoteBtn.setViewVisible()
                        }
                        votingState.votingStrength > 0 -> {//upvoting progress
                            mVotingProgress.setViewVisible()
                            mUpvoteBtn.setViewInvisible()

                            mDownVotingProgress.setViewGone()
                            mDownVoteBtn.setViewVisible()
                        }
                        else -> {//vote removing progress
                            val currentStatus = wrapper.voteStatus
                            if (currentStatus == GolosDiscussionItem.UserVoteType.VOTED) {
                                mVotingProgress.setViewVisible()
                                mDownVotingProgress.setViewGone()
                                mUpvoteBtn.setViewInvisible()
                                mDownVoteBtn.setViewVisible()
                            } else if (currentStatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED) {
                                mVotingProgress.setViewGone()
                                mDownVotingProgress.setViewVisible()
                                mUpvoteBtn.setViewVisible()
                                mDownVoteBtn.setViewInvisible()
                            }
                        }
                    }
                }
            }

            val reblogger = wrapper.story.rebloggedBy
            if (reblogger.isEmpty()){
                mUserNameTv.setOnClickListener { onUserClick.onClick(this) }
                mAvatar.setOnClickListener { onAvatarClick.onClick(this) }
            }else {
                mUserNameTv.setOnClickListener { onRebloggerClick.onClick(this) }
                mAvatar.setOnClickListener { onRebloggerClick.onClick(this) }
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
        return mDownVoteBtn
    }

    override fun getAuthorAvatar(): ImageView {
        return mAvatar
    }

    override fun getErrorDrawable(): Drawable {
        return errorDrawableS!!
    }

    override fun getMainText(): TextView {
        return mMainText
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
        return mUpvoteBtn
    }

    override fun showImageIfNotImageFirst(): Boolean {
        return true
    }


    companion object {

        @JvmStatic
        var errorDrawableS: Drawable? = null
        @JvmStatic
        var userVotedvotedDrarawble: Drawable? = null
    }
}