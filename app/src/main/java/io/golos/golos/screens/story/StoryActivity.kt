package io.golos.golos.screens.story

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.wefika.flowlayout.FlowLayout
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.repository.model.mapper
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.adapters.CommentsAdapter
import io.golos.golos.screens.story.adapters.ImagesAdapter
import io.golos.golos.screens.story.adapters.StoryAdapter
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.screens.widgets.dialogs.OnVoteSubmit
import io.golos.golos.screens.widgets.dialogs.VoteDialog
import io.golos.golos.utils.*
import timber.log.Timber

/**
 * Created by yuri on 06.11.17.
 */
class StoryActivity : GolosActivity(), SwipeRefreshLayout.OnRefreshListener {
    private lateinit var mViewModel: StoryViewModel
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mFab: FloatingActionButton
    private lateinit var mToolbar: Toolbar
    private lateinit var mAvatar: ImageView
    private lateinit var mUserName: TextView
    private lateinit var mRebloggedBy: TextView
    private lateinit var mTagName: TextView
    private lateinit var mTagSubscribeBtn: Button
    private lateinit var mBlogNameTv: TextView
    private lateinit var mTitleTv: TextView
    private lateinit var mVotesIv: TextView
    private lateinit var mFlagTv: TextView
    private lateinit var mSwipeToRefresh: SwipeRefreshLayout
    private lateinit var mNoCommentsTv: TextView
    private lateinit var mStoryRecycler: RecyclerView
    private lateinit var mCommentsRecycler: RecyclerView
    private lateinit var mBottomImagesRecycler: RecyclerView
    private lateinit var mFlow: FlowLayout
    private lateinit var mCommentsCountBtn: TextView
    private lateinit var mVotingProgress: ProgressBar
    private lateinit var mAuthorSubscribeProgress: ProgressBar
    private lateinit var mMoneyBtn: TextView
    private lateinit var mCommentsTv: TextView
    private lateinit var mShareButton: ImageButton
    private lateinit var mAvatarOfAuthorInFollowLo: ImageView
    private lateinit var mNameOfAuthorInFollowLo: TextView
    private lateinit var mAuthorSubscribeButton: Button
    private lateinit var mTagAvatar: View
    private lateinit var mCommentsLoadingProgress: View

    private var isNeedToScrollToComments = false
    private var isScrollEventFired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_story)
        isNeedToScrollToComments = intent.getBooleanExtra(SCROLL_TO_COMMENTS, false)
        setUpViews()
        setUpViewModel()
    }

    override fun onRefresh() {
        mViewModel.requestRefresh()
    }

    private fun setUpViewModel() {
        val provider = ViewModelProviders.of(this)
        mViewModel = provider.get(StoryViewModel::class.java)

        if (!intent.hasExtra(PERMLINK_TAG) ||
                !intent.hasExtra(FEED_TYPE) ||
                !intent.hasExtra(PERMLINK_TAG)) {
            Timber.e(" no story set to activity")
            finish()
            return
        }
        mViewModel.onCreate(intent.getStringExtra(AUTHOR_TAG),
                intent.getStringExtra(PERMLINK_TAG),
                intent.getStringExtra(BLOG_TAG),
                intent.getSerializableExtra(FEED_TYPE) as FeedType,
                mapper.readValue(intent.getStringExtra(STORY_FILTER), StoryFilter::class.java),
                object : InternetStatusNotifier {
                    override fun isAppOnline() = App.isAppOnline()
                })

        mViewModel.liveData.observe(this, Observer {
            if (it?.storyTree?.rootStory() != null) {
                setFullscreenProgress(false)
                if (it.storyTree.rootStory()?.title?.isEmpty() != false) {
                    mTitleTv.visibility = View.GONE
                }
                if (it.isLoading) {
                    if (!mSwipeToRefresh.isRefreshing) mSwipeToRefresh.isRefreshing = true
                } else {
                    mSwipeToRefresh.isRefreshing = false
                }

                val story = it.storyTree.rootStory() ?: return@Observer

                var ets = if (story.parts.isEmpty()) StoryParserToRows().parse(story).toArrayList() else story.parts
                if (story.parts.isEmpty()) story.parts.addAll(ets)

                if (ets.find { it is ImageRow && it.src.matches(Regexps.linkToGolosBoard) } != null) {
                    val list = ArrayList<ImageRow>()
                    val a = ets
                            .filter { it is ImageRow && it.src.matches(Regexps.linkToGolosBoard) }
                            .map { it as ImageRow }
                    list.addAll(a)

                    if (list.size > 1) {
                        list.forEach {
                            ets.remove(it)
                        }
                        mBottomImagesRecycler.visibility = View.VISIBLE
                        if (mBottomImagesRecycler.adapter == null) {
                            mBottomImagesRecycler.adapter =
                                    ImagesAdapter({ },
                                            list)
                        }
                    }
                }
                mStoryRecycler.visibility = View.VISIBLE
                (mStoryRecycler.adapter as StoryAdapter).items = ArrayList(ets)

                if (mAvatar.drawable == null) {
                    mAvatar.visibility = View.VISIBLE
                    mUserName.text = story.author
                    mBlogNameTv.visibility = View.VISIBLE
                    mNameOfAuthorInFollowLo.text = story.author.capitalize()

                    story.avatarPath?.let {
                        val glide = Glide.with(this)

                        glide
                                .load(ImageUriResolver.resolveImageWithSize(story.avatarPath
                                        ?: "", wantedwidth = mAvatar.width))
                                .apply(RequestOptions().placeholder(R.drawable.ic_person_gray_24dp))
                                .error(Glide.with(this).load(R.drawable.ic_person_gray_24dp))
                                .into(mAvatar)
                        glide
                                .load(ImageUriResolver.resolveImageWithSize(story.avatarPath
                                        ?: "", wantedwidth = mAvatarOfAuthorInFollowLo.width))
                                .apply(RequestOptions().placeholder(R.drawable.ic_person_gray_52dp))
                                .error(Glide.with(this).load(R.drawable.ic_person_gray_52dp))
                                .into(mAvatarOfAuthorInFollowLo)
                    }
                    if (story.avatarPath == null) {
                        mAvatar.setImageResource(R.drawable.ic_person_gray_24dp)
                        mAvatarOfAuthorInFollowLo.setImageResource(R.drawable.ic_person_gray_52dp)
                    }
                    mUserName.setOnClickListener {
                        mViewModel.onUserClick(this, mUserName.text.toString())
                    }
                    mAvatarOfAuthorInFollowLo.setOnClickListener { mUserName.callOnClick() }
                    mNameOfAuthorInFollowLo.setOnClickListener { mUserName.callOnClick() }
                    mAvatar.setOnClickListener { mUserName.callOnClick() }
                }
                mAuthorSubscribeButton.text = if (it.subscribeOnStoryAuthorStatus.isCurrentUserSubscribed) getString(R.string.unfollow)
                else getString(R.string.follow)
                mAuthorSubscribeButton.visibility = if (it.subscribeOnStoryAuthorStatus.updatingState != UpdatingState.UPDATING) View.VISIBLE
                else View.GONE

                mAuthorSubscribeProgress.visibility = if (mAuthorSubscribeButton.visibility == View.GONE) View.VISIBLE else View.GONE

                mAuthorSubscribeButton.setOnClickListener { mViewModel.onSubscribeToBlogButtonClick() }


                if (it.subscribeOnTagStatus.isCurrentUserSubscribed) {
                    mTagSubscribeBtn.setText(R.string.unfollow)
                } else {
                    mTagSubscribeBtn.setText(R.string.follow)
                }

                mCommentsCountBtn.visibility = View.VISIBLE
                if (it.storyTree.rootStory()?.userVotestatus == GolosDiscussionItem.UserVoteType.VOTED) {
                    mMoneyBtn.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_triangle_in_circle_green_outline_20dp), null, null, null)
                    mMoneyBtn.setTextColor(ContextCompat.getColor(this, R.color.upvote_green))
                } else {
                    mMoneyBtn.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_triangle_in_cricle_gray_outline_20dp), null, null, null)
                    mMoneyBtn.setTextColor(ContextCompat.getColor(this, R.color.textColorP))
                }
                if (it.storyTree.rootStory()?.userVotestatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED) {
                    mFlagTv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_flag_20dp_red), null, null, null)
                } else {
                    mFlagTv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_flag_20dp_gray), null, null, null)
                }
                if (it.isStoryCommentButtonShown) mFab.show()
                else mFab.hide()

                val tagName = LocalizedTag.convertToLocalizedName(story.categoryName)
                mBlogNameTv.text = tagName
                mTagName.text = tagName.capitalize()
                mTagAvatar.setOnClickListener { mViewModel.onTagClick(this, story.categoryName) }
                mTagName.setOnClickListener { mTagAvatar.callOnClick() }

                mBlogNameTv.setOnClickListener { mViewModel.onTagClick(this, story.categoryName) }
                if (it.errorCode != null) {
                    if (it.errorCode.localizedMessage != null) it.errorCode.localizedMessage.let {
                        findViewById<View>(android.R.id.content).showSnackbar(it)
                    } else {
                        it.errorCode.nativeMessage?.let {
                            findViewById<View>(android.R.id.content).showSnackbar(it)
                        }
                    }
                }

                mCommentsCountBtn.text = it.storyTree.rootStory()?.commentsCount?.toString()
                mVotesIv.text = it.storyTree.rootStory()?.votesNum?.toString() ?: ""
                mVotesIv.setOnClickListener { mViewModel.onStoryVotesClick(this) }
                if (mFlow.childCount != story.tags.count()) {
                    mFlow.removeAllViews()
                    story.tags.forEach {
                        val view = layoutInflater.inflate(R.layout.v_story_tag_button, mFlow, false) as TextView
                        var text = it
                        view.setOnClickListener { mViewModel.onTagClick(this, text) }
                        if (text.contains("ru--")) text = Translit.lat2Ru(it.substring(4))
                        view.text = text.capitalize()
                        mFlow.addView(view)
                    }
                }
                if (it.storyTree.storyWithState()?.updatingState == UpdatingState.UPDATING) {
                    mVotingProgress.visibility = View.VISIBLE
                    mMoneyBtn.visibility = View.GONE

                } else {
                    mVotingProgress.visibility = View.GONE

                    mMoneyBtn.visibility = View.VISIBLE
                }
                findViewById<View>(R.id.vote_lo).visibility = View.VISIBLE

                if (story.commentsCount > 0 && it.storyTree.comments().isEmpty()) {//if comments not downloaded yet
                    mCommentsTv.setViewGone()
                    mNoCommentsTv.setViewGone()
                    mCommentsLoadingProgress.setViewVisible()
                    mCommentsRecycler.setViewGone()
                    if (isNeedToScrollToComments && !isScrollEventFired) {
                        mCommentsLoadingProgress.post { mCommentsLoadingProgress.requestFocus() }

                    }
                } else if (story.commentsCount > 0 && it.storyTree.comments().isNotEmpty()) {
                    mCommentsTv.setViewVisible()
                    mNoCommentsTv.setViewGone()
                    mCommentsLoadingProgress.setViewGone()
                    mCommentsRecycler.setViewVisible()
                    if (isNeedToScrollToComments && !isScrollEventFired) {
                        mCommentsRecycler.post { mCommentsRecycler.requestFocus() }
                        isScrollEventFired = true
                    }
                } else if (story.commentsCount == 0) {
                    mCommentsTv.setViewGone()
                    mNoCommentsTv.setViewVisible()
                    mCommentsLoadingProgress.setViewGone()
                    mCommentsRecycler.setViewGone()
                    if (isNeedToScrollToComments && !isScrollEventFired) {
                        mNoCommentsTv.post {
                            mNoCommentsTv.requestFocus()
                        }
                        isScrollEventFired = true
                    }
                }

                mMoneyBtn.text = String.format("$%.3f", story.payoutInDollars)
                (mCommentsRecycler.adapter as CommentsAdapter).items = ArrayList(it.storyTree.getFlataned())
            } else {
                setFullscreenProgress(true)
            }
            mTitleTv.text = it?.storyTitle
        })
    }

    private fun setUpViews() {
        mToolbar = findViewById<Toolbar>(R.id.toolbar)
        mToolbar.setNavigationOnClickListener({ finish() })
        mProgressBar = findViewById(R.id.progress)
        mFab = findViewById(R.id.fab)
        mAvatar = findViewById(R.id.avatar_iv)
        mUserName = findViewById(R.id.user_name)
        mTagName = findViewById(R.id.tag_name)
        mTagSubscribeBtn = findViewById(R.id.subscribe_tag_btn)
        mRebloggedBy = findViewById(R.id.reblogged_tv)
        mBlogNameTv = findViewById(R.id.blog_name_tv)
        mTitleTv = findViewById(R.id.title_tv)
        mNoCommentsTv = findViewById(R.id.no_comments_tv)
        mStoryRecycler = findViewById(R.id.recycler)
        mFlagTv = findViewById(R.id.flag_btn)
        mCommentsRecycler = findViewById(R.id.comments_recycler)
        mFlow = findViewById(R.id.tags_lo)
        mMoneyBtn = findViewById(R.id.money_btn)
        mVotesIv = findViewById(R.id.votes_btn)
        mSwipeToRefresh = findViewById(R.id.swipe_to_refresh)
        mCommentsCountBtn = findViewById(R.id.comments_btn)
        mVotingProgress = findViewById(R.id.voting_progress)
        mCommentsTv = findViewById(R.id.comments_tv)
        mShareButton = findViewById(R.id.share_btn)
        mCommentsLoadingProgress = findViewById(R.id.comments_progress)
        mAvatarOfAuthorInFollowLo = findViewById(R.id.avatar_in_follow_lo_iv)
        mTagAvatar = findViewById(R.id.tag_avatar)
        mNameOfAuthorInFollowLo = findViewById(R.id.author_name_in_follow_lo)
        mAuthorSubscribeButton = findViewById(R.id.follow_btn)
        mAuthorSubscribeProgress = findViewById(R.id.user_subscribe_progress)
        mBottomImagesRecycler = findViewById(R.id.additional_images_recycler)

        mAuthorSubscribeButton.visibility = View.GONE
        mStoryRecycler.isNestedScrollingEnabled = false
        mCommentsRecycler.isNestedScrollingEnabled = false
        mBottomImagesRecycler.isNestedScrollingEnabled = false

        mStoryRecycler.layoutManager = LinearLayoutManager(this)
        mCommentsRecycler.layoutManager = LinearLayoutManager(this)
        mBottomImagesRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mCommentsRecycler.adapter = CommentsAdapter(
                onUserClick = { mViewModel.onUserClick(this, it.story.author) },
                onCommentsClick = { mViewModel.onCommentClick(this, it.story) },
                onUserVotesClick = { mViewModel.onCommentVoteClick(this, it) })
        mStoryRecycler.adapter = StoryAdapter()
        mRebloggedBy.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)
        mBlogNameTv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_bullet_10dp), null, null, null)
        mCommentsCountBtn.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_chat_gray_20dp), null, null, null)
        mVotesIv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_person_gray_20dp), null, null, null)
        mCommentsCountBtn.visibility = View.INVISIBLE
        mCommentsTv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_sort_red_24dp), null, null, null)
        (mStoryRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        (mCommentsRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        (mStoryRecycler.adapter as StoryAdapter).onRowClick = { row, iv ->
            if (row is TextRow) mViewModel.onMainStoryTextClick(this, row.text)
            else if (row is ImageRow) mViewModel.onMainStoryImageClick(this, row.src, iv)
        }
        mSwipeToRefresh.setProgressBackgroundColorSchemeColor(getColorCompat(R.color.splash_back))
        mSwipeToRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent))
        mMoneyBtn.setOnClickListener({
            if (mViewModel.canUserVote) {
                val story = mViewModel.liveData.value?.storyTree?.storyWithState()
                        ?: return@setOnClickListener
                if (mViewModel.canUserUpVoteOnThis(story)) {
                    val dialog = VoteDialog.getInstance()
                    dialog.selectPowerListener = object : OnVoteSubmit {
                        override fun submitVote(vote: Short) {
                            mViewModel.onStoryVote(story, vote)
                        }
                    }
                    dialog.show(supportFragmentManager, null)
                } else mViewModel.onStoryVote(story, 0)
            } else {
                mViewModel.onVoteRejected()
            }
        })
        mFlagTv.setOnClickListener({
            if (mViewModel.canUserVote) {
                val story = mViewModel.liveData.value?.storyTree?.storyWithState()
                        ?: return@setOnClickListener
                mViewModel.onStoryVote(story, -100)
            } else {
                mViewModel.onVoteRejected()
            }
        })
        (mCommentsRecycler.adapter as CommentsAdapter).onUpvoteClick = {
            if (mViewModel.canUserVote) {
                if (mViewModel.canUserUpVoteOnThis(it)) {
                    val dialog = VoteDialog.getInstance()
                    dialog.selectPowerListener = object : OnVoteSubmit {
                        override fun submitVote(vote: Short) {
                            mViewModel.onStoryVote(it, vote)
                        }
                    }
                    dialog.show(supportFragmentManager, null)
                } else mViewModel.onStoryVote(it, 0)
            } else {
                mViewModel.onVoteRejected()
            }
        }
        (mCommentsRecycler.adapter as CommentsAdapter).onDownVoteClick = {
            if (mViewModel.canUserVote) {
                mViewModel.onStoryVote(it, -100)
            } else {
                mViewModel.onVoteRejected()
            }
        }
        (mCommentsRecycler.adapter as CommentsAdapter).onAnswerClick = {
            mViewModel.onAnswerToComment(this, it.story)
        }
        mFab.setOnClickListener({
            mViewModel.onWriteRootComment(this)
        })
        mSwipeToRefresh.isRefreshing = true
        mShareButton.setOnClickListener({ mViewModel.onShareClick(this) })
        mTagSubscribeBtn.setOnClickListener { mViewModel.onSubscribeToMainTagClick() }
        mSwipeToRefresh.setOnRefreshListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.onDestroy()
    }

    private fun setFullscreenProgress(isShown: Boolean) {
        if (isShown) {
            findViewById<View>(R.id.appbar).setViewGone()
            findViewById<View>(R.id.swipe_to_refresh).setViewGone()
            mFab.setViewGone()
            findViewById<View>(R.id.progress).setViewVisible()
        } else {
            findViewById<View>(R.id.appbar).setViewVisible()
            findViewById<View>(R.id.swipe_to_refresh).setViewVisible()
            if (mViewModel.canUserWriteComments()) mFab.setViewVisible()
            findViewById<View>(R.id.progress).setViewGone()
        }
    }


    companion object {
        private val AUTHOR_TAG = "AUTHOR_TAG"
        private val BLOG_TAG = "BLOG_TAG"
        private val PERMLINK_TAG = "PERMLINK_TAG"
        private val FEED_TYPE = "FEED_TYPE"
        private val STORY_FILTER = "STORY_FILTER"
        private val SCROLL_TO_COMMENTS = "SCROLL_TO_COMMENTS"
        fun start(ctx: Context,
                  author: String,
                  blog: String?,
                  permlink: String,
                  feedType: FeedType,
                  filter: StoryFilter?,
                  scrollToComments: Boolean = false) {

            val intent = Intent(ctx, StoryActivity::class.java)
            intent.putExtra(AUTHOR_TAG, author)
            intent.putExtra(BLOG_TAG, blog)
            intent.putExtra(PERMLINK_TAG, permlink)
            intent.putExtra(FEED_TYPE, feedType)
            intent.putExtra(STORY_FILTER, mapper.writeValueAsString(filter))
            intent.putExtra(SCROLL_TO_COMMENTS, scrollToComments)
            ctx.startActivity(intent)
        }
    }
}