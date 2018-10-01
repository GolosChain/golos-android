package io.golos.golos.screens.story

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.wefika.flowlayout.FlowLayout
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.adapters.CommentsAdapter
import io.golos.golos.screens.story.adapters.ImagesAdapter
import io.golos.golos.screens.story.adapters.StoryAdapter
import io.golos.golos.screens.story.model.DiscussionType
import io.golos.golos.screens.story.model.ImageRow
import io.golos.golos.screens.story.model.StoryParserToRows
import io.golos.golos.screens.story.model.TextRow
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.screens.widgets.dialogs.OnVoteSubmit
import io.golos.golos.screens.widgets.dialogs.PhotosDialog
import io.golos.golos.screens.widgets.dialogs.VoteDialog
import io.golos.golos.utils.*
import timber.log.Timber

/**
 * Created by yuri on 06.11.17.
 */
class DiscussionActivity : GolosActivity(), SwipeRefreshLayout.OnRefreshListener {
    private lateinit var mViewModel: DiscussionViewModel
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
    private lateinit var mStoryRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var mCommentsRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var mBottomImagesRecycler: androidx.recyclerview.widget.RecyclerView
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
    private lateinit var mAppBar: View
    private lateinit var mVoteLo: View
    private lateinit var mFollowBlock: View
    private lateinit var mWriteCommentLo: View
    private lateinit var mWriteCommentTv: View
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
        mViewModel = provider.get(DiscussionViewModel::class.java)

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

        mViewModel.imageDialogShowEvent.observe(this, Observer {
            val dialogData = it ?: return@Observer
            PhotosDialog.getInstance(dialogData.images, if (dialogData.position < 0) 0 else dialogData.position)
                    .show(supportFragmentManager, "images")
        })
        mViewModel.storyLiveData.observe(this, Observer {
            if (it?.storyTree?.rootStory() != null) {
                setFullscreenProgress(false)
                if (it.storyTree.rootStory()?.title?.isEmpty() != false) {
                    mTitleTv.setViewGone()
                }
                mSwipeToRefresh.setRefreshingS(it.isLoading)

                val story = it.storyTree.rootStory() ?: return@Observer

                val ets = if (story.parts.isEmpty())
                    StoryParserToRows.parse(story, true).toArrayList() else story.parts
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
                        if (mBottomImagesRecycler.adapter == null) {
                            mBottomImagesRecycler.visibility = View.VISIBLE
                            mBottomImagesRecycler.adapter =
                                    ImagesAdapter({ },
                                            list)
                        }
                    }
                }

                mStoryRecycler.setViewVisible()
                val adapter = mStoryRecycler.adapter as StoryAdapter
                adapter.items = ArrayList(ets)
                if (mAvatar.drawable == null) {
                    mAvatar.setImageResource(R.drawable.ic_person_gray_24dp)

                    mAvatar.visibility = View.VISIBLE
                    mUserName.text = story.author
                    mBlogNameTv.visibility = View.VISIBLE
                    mNameOfAuthorInFollowLo.text = story.author.capitalize()

                    story.avatarPath?.let {
                        val glide = Glide.with(this)

                        glide
                                .load(ImageUriResolver.resolveImageWithSize(story.avatarPath
                                        ?: "", wantedwidth = mAvatar.width))
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


                mCommentsCountBtn.setViewVisible()
                if (it.storyTree.rootStory()?.userVotestatus == GolosDiscussionItem.UserVoteType.VOTED) {
                    if (mMoneyBtn.tag ?: "" != "green") {
                        mMoneyBtn.setVectorDrawableStart(R.drawable.ic_triangle_in_circle_green_outline_20dp)
                        mMoneyBtn.setTextColor(ContextCompat.getColor(this, R.color.upvote_green))
                        mMoneyBtn.tag = "green"
                    }

                } else {
                    if (mMoneyBtn.tag ?: "" != "gray") {
                        mMoneyBtn.setVectorDrawableStart(R.drawable.ic_triangle_in_cricle_gray_outline_20dp)
                        mMoneyBtn.setTextColor(ContextCompat.getColor(this, R.color.textColorP))
                        mMoneyBtn.tag = "gray"
                    }
                }
                if (it.storyTree.rootStory()?.userVotestatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED
                        && mFlagTv.tag ?: "" != "red") {

                    mFlagTv.setVectorDrawableStart(R.drawable.ic_flag_20dp_red)
                    mFlagTv.tag = "red"
                } else {
                    if (mFlagTv.tag ?: "" != "gray") {
                        mFlagTv.setVectorDrawableStart(R.drawable.ic_flag_20dp_gray)
                        mFlagTv.tag = "gray"
                    }
                }


                val tagName = LocalizedTag.convertToLocalizedName(story.categoryName)
                if (mBlogNameTv.text.isEmpty()) {
                    mBlogNameTv.text = tagName
                }
                if (mTagName.text.isEmpty()) {
                    mTagName.text = tagName.capitalize()
                }

                if (!mTagAvatar.hasOnClickListeners()) {
                    mTagAvatar.setOnClickListener { mViewModel.onTagClick(this, story.categoryName) }
                }
                if (!mTagName.hasOnClickListeners()) {
                    mTagName.setOnClickListener { mTagAvatar.callOnClick() }
                }
                if (!mBlogNameTv.hasOnClickListeners()) {
                    mBlogNameTv.setOnClickListener { mViewModel.onTagClick(this, story.categoryName) }
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

                val commentsCountString = it.storyTree.rootStory()?.commentsCount?.toString()
                if (commentsCountString != mCommentsCountBtn.text.toString()) {
                    mCommentsCountBtn.text = commentsCountString
                }
                val votesCountString = it.storyTree.rootStory()?.votesNum?.toString() ?: ""
                if (mVotesIv.text != votesCountString) {
                    mVotesIv.text = votesCountString
                }
                if (!mVotesIv.hasOnClickListeners()) {
                    mVotesIv.setOnClickListener { mViewModel.onStoryVotesClick(this) }
                }
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
                    mVotingProgress.setViewVisible()
                    mMoneyBtn.setViewGone()
                } else {
                    mVotingProgress.setViewGone()
                    mMoneyBtn.setViewVisible()
                }

                if (it.discussionType == DiscussionType.STORY) {
                    mFollowBlock.setViewVisible()
                    if (it.canUserCommentThis) mFab.show()
                    else mFab.hide()
                    mWriteCommentLo.setViewGone()
                } else if (it.discussionType == DiscussionType.COMMENT) {
                    mFollowBlock.setViewGone()
                    mFab.hide()
                    mWriteCommentLo.setViewVisible()
                    if (!mWriteCommentTv.hasOnClickListeners()) {
                        mWriteCommentLo.setOnClickListener {
                            if (mViewModel.canUserWriteComments()) mViewModel.onWriteRootComment(this)
                            else Snackbar.make(mWriteCommentLo, R.string.login_to_write_comment, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                mVoteLo.setViewVisible()
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
                (mCommentsRecycler.adapter as CommentsAdapter).items = ArrayList(it.storyTree.getFlataned())

                mMoneyBtn.text = calculateShownReward(it.storyTree.storyWithState()
                        ?: return@Observer,
                        ctx = this)

                if (it.storyTree.storyWithState()?.isStoryEditable == true
                        && mToolbar.menu?.findItem(R.id.of) == null) {
                    mToolbar.menu?.clear()
                    mToolbar.inflateMenu(R.menu.story_menu_editable)
                }

            } else {
                setFullscreenProgress(true)
            }
            if (it?.storyTitle != mTitleTv.text) {
                mTitleTv.text = it?.storyTitle
            }
        })
        mViewModel.subscriptionLiveData.observe(this, Observer {
            it ?: return@Observer

            val subscribeText = if (it.subscribeOnStoryAuthorStatus.isCurrentUserSubscribed) getString(R.string.unfollow)
            else getString(R.string.follow)

            if (subscribeText != mAuthorSubscribeButton.text) {
                mAuthorSubscribeButton.text = subscribeText
            }

            if (it.subscribeOnStoryAuthorStatus.updatingState == UpdatingState.UPDATING) {
                mAuthorSubscribeButton.setViewGone()
                mAuthorSubscribeProgress.setViewVisible()
            } else {
                mAuthorSubscribeButton.setViewVisible()
                mAuthorSubscribeProgress.setViewGone()
            }

            if (!mAuthorSubscribeButton.hasOnClickListeners()) {
                mAuthorSubscribeButton.setOnClickListener { mViewModel.onSubscribeToBlogButtonClick() }
            }

            if (it.subscribeOnTagStatus.isCurrentUserSubscribed) {
                mTagSubscribeBtn.setText(R.string.unfollow)
            } else {
                mTagSubscribeBtn.setText(R.string.follow)
            }
        })
    }


    override fun onStart() {
        super.onStart()
        mViewModel.onStart()
    }

    override fun onStop() {
        super.onStop()
        mViewModel.onStop()
    }

    private fun setUpViews() {
        mToolbar = findViewById<Toolbar>(R.id.toolbar)

        mProgressBar = findViewById(R.id.progress)
        mFab = findViewById(R.id.fab)
        mAvatar = findViewById(R.id.avatar_iv)
        mUserName = findViewById(R.id.user_name)
        mTagName = findViewById(R.id.tag_name)
        mFollowBlock = findViewById(R.id.follow_lo)
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
        mWriteCommentLo = findViewById(R.id.write_a_comment_lo)
        mWriteCommentTv = findViewById(R.id.write_a_comment_tv)
        mCommentsTv = findViewById(R.id.comments_tv)
        mShareButton = findViewById(R.id.share_btn)
        mCommentsLoadingProgress = findViewById(R.id.comments_progress)
        mAvatarOfAuthorInFollowLo = findViewById(R.id.avatar_in_follow_lo_iv)
        mTagAvatar = findViewById(R.id.tag_avatar)
        mNameOfAuthorInFollowLo = findViewById(R.id.author_name_in_follow_lo)
        mAuthorSubscribeButton = findViewById(R.id.follow_btn)
        mAuthorSubscribeProgress = findViewById(R.id.user_subscribe_progress)
        mBottomImagesRecycler = findViewById(R.id.additional_images_recycler)
        mVoteLo = findViewById(R.id.vote_lo)
        mAppBar = findViewById(R.id.appbar)

        mAuthorSubscribeButton.visibility = View.GONE
        mStoryRecycler.isNestedScrollingEnabled = false
        mCommentsRecycler.isNestedScrollingEnabled = false
        mBottomImagesRecycler.isNestedScrollingEnabled = false

        mStoryRecycler.layoutManager = MyLinearLayoutManager(this)
        mCommentsRecycler.layoutManager = MyLinearLayoutManager(this)
        mBottomImagesRecycler.layoutManager = MyLinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        mCommentsRecycler.adapter = CommentsAdapter(
                onUserClick = { mViewModel.onUserClick(this, it.story.author) },
                onCommentsClick = { mViewModel.onCommentClick(this, it.story) },
                onUserVotesClick = { mViewModel.onCommentVoteClick(this, it) },
                onEditClick = { mViewModel.onEditClick(this, it.story) })
        mStoryRecycler.adapter = StoryAdapter()
        mRebloggedBy.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)
        mBlogNameTv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_bullet_10dp), null, null, null)
        mCommentsCountBtn.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_chat_gray_20dp), null, null, null)
        mVotesIv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_person_gray_20dp), null, null, null)
        mCommentsCountBtn.visibility = View.INVISIBLE
        mCommentsTv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_sort_red_24dp), null, null, null)
        (mStoryRecycler.itemAnimator as androidx.recyclerview.widget.SimpleItemAnimator).supportsChangeAnimations = false
        (mCommentsRecycler.itemAnimator as androidx.recyclerview.widget.SimpleItemAnimator).supportsChangeAnimations = false

        (mStoryRecycler.adapter as StoryAdapter).onRowClick = { row, iv ->
            if (row is TextRow) mViewModel.onMainStoryTextClick(this, row.text)
            else if (row is ImageRow) mViewModel.onMainStoryImageClick(row.src)
        }


        mSwipeToRefresh.setProgressBackgroundColorSchemeColor(getColorCompat(R.color.splash_back))
        mSwipeToRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent))
        mMoneyBtn.setOnClickListener {
            if (mViewModel.canUserVote) {
                val story = mViewModel.storyLiveData.value?.storyTree?.storyWithState()
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
        }
        mFlagTv.setOnClickListener {
            if (mViewModel.canUserVote) {
                val story = mViewModel.storyLiveData.value?.storyTree?.storyWithState()
                        ?: return@setOnClickListener
                mViewModel.onStoryVote(story, -100)
            } else {
                mViewModel.onVoteRejected()
            }
        }
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
        mFab.setOnClickListener {
            mViewModel.onWriteRootComment(this)
        }
        mSwipeToRefresh.isRefreshing = true
        mShareButton.setOnClickListener({ mViewModel.onShareClick(this) })
        mTagSubscribeBtn.setOnClickListener { mViewModel.onSubscribeToMainTagClick() }
        mSwipeToRefresh.setOnRefreshListener(this)
        setSupportActionBar(mToolbar)
        mToolbar.setNavigationOnClickListener({ finish() })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (mViewModel.isPostEditable()) menuInflater.inflate(R.menu.story_menu_editable, menu)
        else menuInflater.inflate(R.menu.story_menu_not_editable, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.share -> mViewModel.onShareClick(this)
            R.id.edit -> mViewModel.onEditClick(this, mViewModel.storyLiveData.value?.storyTree?.rootStory()
                    ?: return true)
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.onStop()
    }

    private fun setFullscreenProgress(isShown: Boolean) {
        if (isShown) {
            mAppBar.setViewGone()
            mSwipeToRefresh.setViewGone()
            mFab.setViewGone()
            mWriteCommentLo.setViewGone()
            mProgressBar.setViewVisible()
        } else {
            mAppBar.setViewVisible()
            mSwipeToRefresh.setViewVisible()
            mWriteCommentLo.setViewVisible()
            if (mViewModel.canUserWriteComments()) mFab.setViewVisible()
            mProgressBar.setViewGone()
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
            ctx.startActivity(getStartIntent(ctx, author, blog, permlink, feedType, filter, scrollToComments))
        }

        fun getStartIntent(ctx: Context,
                           author: String,
                           blog: String?,
                           permlink: String,
                           feedType: FeedType,
                           filter: StoryFilter?,
                           scrollToComments: Boolean = false): Intent {
            val intent = Intent(ctx, DiscussionActivity::class.java)
            intent.putExtra(AUTHOR_TAG, author)
            intent.putExtra(BLOG_TAG, blog)
            intent.putExtra(PERMLINK_TAG, permlink)
            intent.putExtra(FEED_TYPE, feedType)
            intent.putExtra(STORY_FILTER, mapper.writeValueAsString(filter))
            intent.putExtra(SCROLL_TO_COMMENTS, scrollToComments)
            return intent
        }
    }
}