package io.golos.golos.screens.story

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.wefika.flowlayout.FlowLayout
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.main_stripes.model.FeedType
import io.golos.golos.screens.story.adapters.CommentsAdapter
import io.golos.golos.screens.story.adapters.MainStoryAdapter
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.screens.widgets.OnVoteSubmit
import io.golos.golos.screens.widgets.VoteDialog
import io.golos.golos.utils.Translit
import io.golos.golos.utils.UpdatingState
import io.golos.golos.utils.getVectorDrawable
import io.golos.golos.utils.showSnackbar
import timber.log.Timber

/**
 * Created by yuri on 06.11.17.
 */
class StoryActivity : GolosActivity() {
    private lateinit var mViewModel: StoryViewModel
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mFab: FloatingActionButton
    private lateinit var mToolbar: Toolbar
    private lateinit var mAvatar: ImageView
    private lateinit var mUserName: TextView
    private lateinit var mRebloggedBy: TextView
    private lateinit var mForTV: TextView
    private lateinit var mBlogNameTv: TextView
    private lateinit var mtitileTv: TextView
    private lateinit var mNoCommentsTv: TextView
    private lateinit var mStoryRecycler: RecyclerView
    private lateinit var mCommentsRecycler: RecyclerView
    private lateinit var mFlow: FlowLayout
    private lateinit var mPayoutTv: TextView
    private lateinit var mVotesCountTv: TextView
    private lateinit var mVoteBtn: ImageButton
    private lateinit var mCommentsCountTv: TextView
    private lateinit var mVotingProgress: ProgressBar
    private lateinit var mCommentsTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_story)
        setUpViews()
        setUpViewModel()
    }

    private fun setUpViewModel() {
        val provider = ViewModelProviders.of(this)
        mViewModel = provider.get(StoryViewModel::class.java)
        val extra = intent.getLongExtra(COMMENT_TAG, 0L)
        if (extra == 0L) {
            Timber.e(" no comment set to activity")
            finish()
            return
        }
        mViewModel.onCreate(extra, intent.getSerializableExtra(FEED_TYPE) as FeedType)
        mViewModel.liveData.observe(this, Observer {
            mProgressBar.visibility = if (it?.isLoading == true) View.VISIBLE else View.GONE
            if (it?.storyTree?.rootStory() != null) {
                Timber.e("on new story")
                val story = it.storyTree.rootStory()!!
                var ets = StoryParserToRows().parse(story)
                mStoryRecycler.visibility = View.VISIBLE
                if (ets.size == 1) {
                    Timber.e("parser fail!!!!! on ${it.storyTree.rootStory()}")
                }
                (mStoryRecycler.adapter as MainStoryAdapter).items = ArrayList(ets)
                if (mAvatar.drawable == null) {
                    mAvatar.visibility = View.VISIBLE
                    if (story.avatarPath != null) Glide.with(this)
                            .load(story.avatarPath)
                            .error(Glide.with(this).load(R.drawable.ic_person_gray_24dp))
                            .apply(RequestOptions.placeholderOf(R.drawable.ic_person_gray_24dp))
                            .into(mAvatar)
                    else mAvatar.setImageResource(R.drawable.ic_person_gray_24dp)
                    mUserName.text = story.author
                    mForTV.visibility = View.VISIBLE
                    mBlogNameTv.visibility = View.VISIBLE
                }
                if (it.storyTree.rootStory()?.isUserUpvotedOnThis == true) {
                    mVoteBtn.setImageResource(R.drawable.ic_upvote_active_18dp_green)
                    mVoteBtn.background = ResourcesCompat.getDrawable(resources, R.drawable.ripple_green_solid, null)
                    mPayoutTv.setTextColor(ContextCompat.getColor(this, R.color.upvote_green))
                } else {
                    mVoteBtn.setImageResource(R.drawable.ic_upvote_18_gray)
                    mVoteBtn.background = ResourcesCompat.getDrawable(resources, R.drawable.ripple_gray_circle, null)
                    mPayoutTv.setTextColor(ContextCompat.getColor(this, R.color.textColorP))
                }
                if (it.isStoryCommentButtonShown) mFab.show()
                else mFab.hide()
                if (story.categoryName.contains("ru--")) {
                    mBlogNameTv.text = Translit.lat2Ru(story.categoryName.substring(4))
                } else {
                    mBlogNameTv.text = story.categoryName
                }
                if (it.errorCode != null) {
                    if (it.errorCode.localizedMessage != null) it.errorCode.localizedMessage.let {
                        findViewById<View>(android.R.id.content).showSnackbar(it)
                    } else {
                        it.errorCode.nativeMessage?.let {
                            findViewById<View>(android.R.id.content).showSnackbar(it)
                        }
                    }
                }
                mVotesCountTv.text = it.storyTree.rootStory()?.activeVotes?.toString()
                mCommentsCountTv.text = it.storyTree.rootStory()?.commentsCount?.toString()
                if (mFlow.childCount != story.tags.count()) {
                    story.tags.forEach {
                        val view = layoutInflater.inflate(R.layout.v_story_tag_button, mFlow, false) as TextView
                        var text = it
                        if (text.contains("ru--")) text = Translit.lat2Ru(it.substring(4))
                        view.text = text
                        mFlow.addView(view)
                    }
                }
                if (it.storyTree.storyWithState()?.updatingState == UpdatingState.UPDATING) {
                    mVotingProgress.visibility = View.VISIBLE
                    mPayoutTv.visibility = View.GONE
                    mVoteBtn.visibility = View.GONE
                } else {
                    mVotingProgress.visibility = View.GONE
                    mVoteBtn.visibility = View.VISIBLE
                    mPayoutTv.visibility = View.VISIBLE
                }
                findViewById<View>(R.id.vote_lo).visibility = View.VISIBLE
                findViewById<View>(R.id.comments_tv).visibility = View.VISIBLE
                mPayoutTv.text = String.format("$ %.2f", story.payoutInDollars)
                mVotesCountTv.text = "${story.votesNum}"

                (mCommentsRecycler.adapter as CommentsAdapter).items = ArrayList(it.storyTree.getFlataned())

                if (it.storyTree.comments().isEmpty()) {
                    mCommentsRecycler.visibility = View.GONE
                    mNoCommentsTv.visibility = View.VISIBLE
                } else {
                    mCommentsRecycler.visibility = View.VISIBLE
                    mNoCommentsTv.visibility = View.GONE
                }
            }
            mtitileTv.text = it?.storyTitle
        })
    }

    private fun setUpViews() {
        mToolbar = findViewById<Toolbar>(R.id.toolbar)
        mToolbar.setNavigationOnClickListener({ finish() })
        mProgressBar = findViewById(R.id.progress)
        mFab = findViewById(R.id.fab)
        mAvatar = findViewById(R.id.avatar_iv)
        mUserName = findViewById(R.id.user_name)
        mRebloggedBy = findViewById(R.id.reblogged_tv)
        mForTV = findViewById(R.id.for_user_tv)
        mBlogNameTv = findViewById(R.id.blog_name_tv)
        mtitileTv = findViewById(R.id.title_tv)
        mNoCommentsTv = findViewById(R.id.no_comments_tv)
        mStoryRecycler = findViewById(R.id.recycler)
        mCommentsRecycler = findViewById(R.id.comments_recycler)
        mVotesCountTv = findViewById(R.id.votes_btn)
        mFlow = findViewById(R.id.tags_lo)
        mPayoutTv = findViewById(R.id.money_label)
        mCommentsTv = findViewById(R.id.comments_tv)
        mVoteBtn = findViewById(R.id.upvote_btn)
        mCommentsCountTv = findViewById(R.id.comments_btn)
        mVotingProgress = findViewById(R.id.voting_progress)
        mStoryRecycler.isNestedScrollingEnabled = false
        mCommentsRecycler.isNestedScrollingEnabled = false
        mStoryRecycler.layoutManager = LinearLayoutManager(this)
        mCommentsRecycler.layoutManager = LinearLayoutManager(this)
        mCommentsRecycler.adapter = CommentsAdapter()
        mStoryRecycler.adapter = MainStoryAdapter()
        mRebloggedBy.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)
        mBlogNameTv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_bullet_accent_20dp), null, null, null)
        mVotesCountTv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_voices_20dp_gray), null, null, null)
        mCommentsCountTv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_comments_wth_dots_24dp_gray), null, null, null)
        mCommentsTv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_sort_red_24dp), null, null, null)
        (mStoryRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        (mCommentsRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        (mStoryRecycler.adapter as MainStoryAdapter).onRowClick = { row, iv ->
            if (row is TextRow) mViewModel.onMainStoryTextClick(this, row.text)
            else if (row is ImageRow) mViewModel.onMainStoryImageClick(this, row.src, iv)
        }
        mVoteBtn.setOnClickListener({
            if (mViewModel.showVoteDialog) {
                val story = mViewModel.liveData.value?.storyTree?.storyWithState() ?: return@setOnClickListener
                if (mViewModel.canUserVoteOnThis(story)) {
                    val dialog = VoteDialog.getInstance()
                    dialog.selectPowerListener = object : OnVoteSubmit {
                        override fun submitVote(vote: Short) {
                            mViewModel.onStoryVote(story, vote)
                        }
                    }
                    dialog.show(fragmentManager, null)
                } else mViewModel.onStoryVote(story, -1)
            }
        })
        (mCommentsRecycler.adapter as CommentsAdapter).onUpvoteClick = {
            if (mViewModel.showVoteDialog) {
                if (mViewModel.canUserVoteOnThis(it)) {
                    val dialog = VoteDialog.getInstance()
                    dialog.selectPowerListener = object : OnVoteSubmit {
                        override fun submitVote(vote: Short) {
                            mViewModel.onStoryVote(it, vote)
                        }
                    }
                    dialog.show(fragmentManager, null)
                } else mViewModel.onStoryVote(it, -1)
            }
        }
        (mCommentsRecycler.adapter as CommentsAdapter).onAnswerClick = {
            mViewModel.onAnswerToComment(this, it.story)
        }
        mFab.setOnClickListener({
            mViewModel.onWriteRootComment(this)
        })
    }

    companion object {
        private val COMMENT_TAG = "COMMENT_TAG"
        private val FEED_TYPE = "FEED_TYPE"
        fun start(ctx: Context,
                  storyId: Long,
                  feedType: FeedType) {

            val intent = Intent(ctx, StoryActivity::class.java)
            intent.putExtra(COMMENT_TAG, storyId)
            intent.putExtra(FEED_TYPE, feedType)
            ctx.startActivity(intent)
        }
    }
}