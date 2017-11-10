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
import android.widget.Button
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
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.utils.Translit
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
    private lateinit var mPayoutBtn: Button
    private lateinit var mVotesCountBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_story)
        setUpViews()
        setUpViewModel()
    }

    private fun setUpViewModel() {
        val provider = ViewModelProviders.of(this)
        mViewModel = provider.get(StoryViewModel::class.java)
        mViewModel.onCreate(blogName = intent.getStringExtra(BLOG_TAG) ?: "",
                author = intent.getStringExtra(AUTHOR_TAG) ?: "",
                permlink = intent.getStringExtra(PERMLINK_TAG) ?: "")
        mtitileTv.text = intent.getStringExtra(TITLE_TAG)

        mViewModel.liveData.observe(this, Observer {
            mProgressBar.visibility = if (it?.isLoading == true) View.VISIBLE else View.GONE
            if (it?.storyTree?.rootStory != null) {
                Timber.e("new story, titled ${it.storyTitle}")
                val story = it.storyTree.rootStory!!
                var ets = StoryParserToRows().parse(story)
                mStoryRecycler.visibility = View.VISIBLE

                mStoryRecycler.adapter = MainStoryAdapter(ets)
                if (ets.size == 1) {
                    Timber.e("parser fail!!!!! on ${it.storyTree.rootStory}")
                }

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

                if (story.categoryName.contains("ru--")) {
                    mBlogNameTv.text = Translit.lat2Ru(story.categoryName.substring(4))
                } else {
                    mBlogNameTv.text = story.categoryName
                }
                if (it.errorCode != null) showErrorMessage(it.errorCode)
                story.tags.forEach {
                    val view = layoutInflater.inflate(R.layout.v_story_tag_button, mFlow, false) as TextView
                    var text = it
                    if (text.contains("ru--")) text = Translit.lat2Ru(it.substring(4))
                    view.text = text
                    mFlow.addView(view)
                }
                findViewById<View>(R.id.vote_lo).visibility = View.VISIBLE
                findViewById<View>(R.id.comments_tv).visibility = View.VISIBLE
                mPayoutBtn.text = String.format("$ %.2f", story.payoutValueInDollars)
                mVotesCountBtn.text = "${story.votesNum} ${resources.getQuantityString(R.plurals.votes, story.votesNum)}"
                mCommentsRecycler.adapter = CommentsAdapter()
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
        mFlow = findViewById(R.id.tags_lo)
        mPayoutBtn = findViewById(R.id.upvote_btn)
        mVotesCountBtn = findViewById(R.id.votes_btn)
        mStoryRecycler.isNestedScrollingEnabled = false
        mCommentsRecycler.isNestedScrollingEnabled = false
        mStoryRecycler.layoutManager = LinearLayoutManager(this)
        mCommentsRecycler.layoutManager = LinearLayoutManager(this)
    }

    companion object {
        private val AUTHOR_TAG = "AUTHOR_TAG"
        private val PERMLINK_TAG = "PERMLINK_TAG"
        private val BLOG_TAG = "BLOG_TAG"
        private val TITLE_TAG = "TITLE_TAG"
        fun start(ctx: Context,
                  author: String,
                  permlink: String,
                  blog: String,
                  title: String = "") {

            val intent = Intent(ctx, StoryActivity::class.java)
            intent.putExtra(AUTHOR_TAG, author)
            intent.putExtra(PERMLINK_TAG, permlink)
            intent.putExtra(BLOG_TAG, blog)
            intent.putExtra(TITLE_TAG, title)
            ctx.startActivity(intent)
        }
    }
}