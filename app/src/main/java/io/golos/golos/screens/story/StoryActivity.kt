package io.golos.golos.screens.story

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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
import io.golos.golos.screens.story.adapters.CommentsAdapter
import io.golos.golos.screens.story.adapters.MainStoryAdapter
import io.golos.golos.screens.story.model.*
import io.golos.golos.utils.Translit
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
    private lateinit var mPayoutIv: TextView
    private lateinit var mVotesCountTv: TextView
    private lateinit var mVoteBtn: ImageButton
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
        val extra = intent.getStringExtra(COMMENT_TAG)
        if (extra == null) {
            Timber.e(" no comment set to activity")
            finish()
            return
        }
        mViewModel.onCreate(mapper.readValue<Comment>(extra, Comment::class.java))
        mViewModel.liveData.observe(this, Observer {
            mProgressBar.visibility = if (it?.isLoading == true) View.VISIBLE else View.GONE
            if (it?.storyTree?.rootStory != null) {
                Timber.e("new story, titled ${it.storyTitle}")
                val story = it.storyTree.rootStory!!
                var ets = StoryParserToRows().parse(story)
                mStoryRecycler.visibility = View.VISIBLE
                if (ets.size == 1) {
                    Timber.e("parser fail!!!!! on ${it.storyTree.rootStory}")
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
                mVotesCountTv.text = it.storyTree.rootStory?.activeVotes?.toString()
                mCommentsTv.text = it.storyTree.rootStory?.commentsCount?.toString()
                if (mFlow.childCount != story.tags.count()) {
                    story.tags.forEach {
                        val view = layoutInflater.inflate(R.layout.v_story_tag_button, mFlow, false) as TextView
                        var text = it
                        if (text.contains("ru--")) text = Translit.lat2Ru(it.substring(4))
                        view.text = text
                        mFlow.addView(view)
                    }
                }
                findViewById<View>(R.id.vote_lo).visibility = View.VISIBLE
                findViewById<View>(R.id.comments_tv).visibility = View.VISIBLE
                mPayoutIv.text = String.format("$ %.2f", story.payoutInDollars)
                mVotesCountTv.text = "${story.votesNum}"
                (mCommentsRecycler.adapter as CommentsAdapter).items = ArrayList(it.storyTree.getFlataned())
                if (it.storyTree.comments.isEmpty()) {
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
        mPayoutIv = findViewById(R.id.money_label)
        mVoteBtn = findViewById(R.id.upvote_btn)
        mCommentsTv = findViewById(R.id.comments_btn)
        mStoryRecycler.isNestedScrollingEnabled = false
        mCommentsRecycler.isNestedScrollingEnabled = false
        mStoryRecycler.layoutManager = LinearLayoutManager(this)
        mCommentsRecycler.layoutManager = LinearLayoutManager(this)
        mCommentsRecycler.adapter = CommentsAdapter()
        mStoryRecycler.adapter = MainStoryAdapter()

        (mStoryRecycler.adapter as MainStoryAdapter).onRowClick = { row, iv ->
            if (row is TextRow) mViewModel.onMainStoryTextClick(this,row.text)
            else if (row is ImageRow) mViewModel.onMainStoryImageClick(this,row.src, iv)
        }
    }

    companion object {
        private val COMMENT_TAG = "COMMENT_TAG"
        fun start(ctx: Context,
                  comment: Comment) {

            val intent = Intent(ctx, StoryActivity::class.java)
            intent.putExtra(COMMENT_TAG, mapper.writeValueAsString(comment))
            ctx.startActivity(intent)
        }
    }
}