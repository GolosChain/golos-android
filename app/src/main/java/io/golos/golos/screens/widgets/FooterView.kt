package io.golos.golos.screens.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.utils.*

/**
 * Created by yuri yurivladdurain@gmail.com on 23/10/2018.
 */
class FooterView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        addView(inflate<View>(R.layout.v_footer))
    }

    val upvoteImageButton = findViewById<ImageView>(R.id.footer_upvote_ibtn)
    val upvoteText = findViewById<TextView>(R.id.footer_upvote_btn)
    val upvoteProgresss = findViewById<View>(R.id.footer_progress_upvote)


    val moneyCountTextView = findViewById<TextView>(R.id.footer_money_tv)

    val dizLikeImageView = findViewById<ImageView>(R.id.footer_dizlike_ibtn)
    val dizLikeCountTextView = findViewById<TextView>(R.id.footer_down_vote_btn)
    val dizLikeProgress = findViewById<View>(R.id.footer_progress_downvote)

    val commentsCountTextView = findViewById<TextView>(R.id.footer_comments_tv)

    val reblogImageView = findViewById<ImageView>(R.id.footer_reblog_ibtn)
    val reblogProgress = findViewById<View>(R.id.footer_reblog_progress)

    val voteCountTextView = findViewById<TextView>(R.id.footer_votes_tv)

    fun setUpovteProgress(isLoading: Boolean) {
        if (isLoading) {
            upvoteImageButton.setViewInvisible()
            upvoteText.setViewInvisible()
            upvoteProgresss.setViewVisible()
        } else {
            upvoteImageButton.setViewVisible()
            upvoteText.setViewVisible()
            upvoteProgresss.setViewGone()
        }
    }

    fun setDizLikwProgress(isLoading: Boolean) {
        if (isLoading) {
            dizLikeImageView.setViewInvisible()
            dizLikeCountTextView.setViewInvisible()
            dizLikeProgress.setViewVisible()
        } else {
            dizLikeImageView.setViewVisible()
            dizLikeCountTextView.setViewVisible()
            dizLikeProgress.setViewGone()
        }
    }

    fun setReblogProgress(isLoading: Boolean) {
        if (isLoading) {
            reblogImageView.setViewInvisible()
            reblogProgress.setViewVisible()
        } else {
            reblogImageView.setViewVisible()
            reblogProgress.setViewGone()
        }
    }

    init {
        commentsCountTextView.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_comments_20dp), null, null, null)
        voteCountTextView.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_person_outline), null, null, null)
    }
}