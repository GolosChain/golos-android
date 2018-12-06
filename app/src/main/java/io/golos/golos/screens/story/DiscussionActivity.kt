package io.golos.golos.screens.story

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.wefika.flowlayout.FlowLayout
import io.golos.golos.App
import io.golos.golos.R
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.settings.SpinnerAdapterWithDownChevron
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.adapters.CommentsAdapter
import io.golos.golos.screens.story.adapters.ImagesAdapter
import io.golos.golos.screens.story.adapters.StoryAdapter
import io.golos.golos.screens.story.model.*
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.screens.widgets.FooterView
import io.golos.golos.screens.widgets.dialogs.ChangeVoteDialog
import io.golos.golos.screens.widgets.dialogs.PhotosDialog
import io.golos.golos.screens.widgets.dialogs.VoteDialog
import io.golos.golos.utils.*
import timber.log.Timber

/**
 * Created by yuri on 06.11.17.
 */
class DiscussionActivity : GolosActivity(), SwipeRefreshLayout.OnRefreshListener,
        VoteDialog.OnVoteSubmit, ReblogConfirmalDialog.OnReblogConfirmed, ChangeVoteDialog.OnChangeConfirmal {
    private lateinit var mViewModel: DiscussionViewModel
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mToolbar: Toolbar
    private lateinit var mAvatar: ImageView
    private lateinit var mUserName: TextView
    private lateinit var mRebloggedBy: TextView
    private lateinit var mTagName: TextView
    private lateinit var mUserNick: TextView
    private lateinit var mTagSubscribeBtn: Button
    private lateinit var mBlogNameTv: TextView
    private lateinit var mTitleTv: TextView
    private lateinit var mSwipeToRefresh: SwipeRefreshLayout
    private lateinit var mStoryRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var mCommentsRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var mBottomImagesRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var mFlow: FlowLayout
    private lateinit var mAuthorSubscribeProgress: ProgressBar
    private lateinit var mShareButton: ImageButton
    private lateinit var mCommentsTv: TextView
    private lateinit var mAvatarOfAuthorInFollowLo: ImageView
    private lateinit var mNameOfAuthorInFollowLo: TextView
    private lateinit var mAuthorSubscribeButton: Button
    private lateinit var mTagAvatar: View
    private lateinit var mCommentsLoadingProgress: View
    private lateinit var mSortLo: View
    private lateinit var mSortSpinner: Spinner
    private lateinit var mAppBar: View
    private lateinit var mFooter: FooterView
    private lateinit var mFollowBlock: View
    private lateinit var mWriteCommentLo: View
    private lateinit var mWriteCommentTv: TextView
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
            Timber.e(" no rootWrapper set to activity")
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
            if (it?.storyTree?.rootWrapper != null) {
                setFullscreenProgress(false)
                if (it.storyTree.rootWrapper.story.title.isEmpty()) {
                    mTitleTv.setViewGone()
                }
                mSwipeToRefresh.setRefreshingS(it.isLoading)

                val story = it.storyTree.rootWrapper.story
                val rootWrapper = it.storyTree.rootWrapper
                val account = it.storyTree.rootWrapper.authorAccountInfo

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

                    mBlogNameTv.visibility = View.VISIBLE
                    mNameOfAuthorInFollowLo.text = rootWrapper.authorAccountInfo?.shownName ?: rootWrapper.authorAccountInfo?.userName ?: story.author.capitalize()

                    it.storyTree.rootWrapper.authorAccountInfo?.avatarPath?.let { avatarPath ->
                        val glide = Glide.with(this)

                        glide
                                .load(ImageUriResolver.resolveImageWithSize(avatarPath, wantedwidth = mAvatar.width))
                                .error(Glide.with(this).load(R.drawable.ic_person_gray_24dp))
                                .into(mAvatar)
                        glide
                                .load(ImageUriResolver.resolveImageWithSize(avatarPath, wantedwidth = mAvatarOfAuthorInFollowLo.width))
                                .apply(RequestOptions().placeholder(R.drawable.ic_person_gray_52dp))
                                .error(Glide.with(this).load(R.drawable.ic_person_gray_52dp))
                                .into(mAvatarOfAuthorInFollowLo)
                    }
                    if (it.storyTree.rootWrapper.authorAccountInfo?.avatarPath == null) {
                        mAvatar.setImageResource(R.drawable.ic_person_gray_24dp)
                        mAvatarOfAuthorInFollowLo.setImageResource(R.drawable.ic_person_gray_52dp)
                    }

                    mAvatarOfAuthorInFollowLo.setOnClickListener { mUserName.callOnClick() }
                    mNameOfAuthorInFollowLo.setOnClickListener { mUserName.callOnClick() }
                    mAvatar.setOnClickListener { mUserName.callOnClick() }
                }


                if (it.storyTree.rootWrapper.voteStatus == GolosDiscussionItem.UserVoteType.VOTED) {
                    if (mFooter.upvoteImageButton.tag ?: "" != "blue") {
                        mFooter.upvoteImageButton.setImageResource(R.drawable.ic_liked_20dp)
                        mFooter.upvoteImageButton.tag = "blue"
                    }

                } else {
                    if (mFooter.upvoteImageButton.tag ?: "" != "gray") {
                        mFooter.upvoteImageButton.setImageResource(R.drawable.ic_like_20dp)
                        mFooter.upvoteImageButton.tag = "gray"
                    }
                }


                if (it.storyTree.rootWrapper.voteStatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED) {
                    if (mFooter.dizLikeImageView.tag ?: "" != "red") {
                        mFooter.dizLikeImageView.setImageResource(R.drawable.ic_dizliked_20dp)
                        mFooter.dizLikeImageView.tag = "red"
                    }
                } else {
                    if (mFooter.dizLikeImageView.tag ?: "" != "gray") {
                        mFooter.dizLikeImageView.setImageResource(R.drawable.ic_dizlike_20dp)
                        mFooter.dizLikeImageView.tag = "gray"
                    }
                }

                if (it.storyTree.rootWrapper.isPostReposted) {
                    if (mFooter.reblogImageView.tag != "blue") {
                        mFooter.reblogImageView.setImageResource(R.drawable.ic_reposted_20dp)
                        mFooter.reblogImageView.tag = "blue"
                    }
                } else {
                    if (mFooter.reblogImageView.tag != "gray") {
                        mFooter.reblogImageView.setImageResource(R.drawable.ic_repost_20dp)
                        mFooter.reblogImageView.tag = "gray"
                    }
                }

                if (it.showRepostButton) {
                    mFooter.setReblogProgress(it.storyTree.rootWrapper.repostStatus == UpdatingState.UPDATING)
                } else {
                    mFooter.reblogImageView.setViewGone()
                }

                mFooter.upvoteText.setOnClickListener {
                    mViewModel.onUpvotersClick(rootWrapper, this)
                }
                mFooter.dizLikeCountTextView.setOnClickListener {
                    mViewModel.onDownVotersClick(rootWrapper, this)
                }

                if (mFooter.upvoteText.text.toString() != story.upvotesNum.toString()) {
                    mFooter.upvoteText.text = story.upvotesNum.toString()
                }
                if (mFooter.dizLikeCountTextView.text.toString() != story.downvotesNum.toString()) {
                    mFooter.dizLikeCountTextView.text = story.downvotesNum.toString()
                }

                if (mUserName.text.toString() != account?.shownName) {
                    if (account?.shownName == null) mUserName.setViewGone()
                    else mUserName.setViewVisible()

                    mUserName.text = account?.shownName.orEmpty().capitalize()
                }


                if (account != null && account.userName != story.author) {

                    mRebloggedBy.setViewVisible()
                    mRebloggedBy.text = story.author
                    mUserNick.text = account.userName
                    mRebloggedBy.setOnClickListener {
                        mViewModel.onUserClick(this, story.author)
                    }
                    mUserNick.setOnClickListener { mViewModel.onUserClick(this, account.userName) }
                    mUserName.setOnClickListener {
                        mViewModel.onUserClick(this, account.userName)

                    }
                } else {
                    mUserName.setOnClickListener {
                        mViewModel.onUserClick(this, story.author)
                    }
                    mUserNick.setOnClickListener {
                        mUserName.callOnClick()
                    }
                    mRebloggedBy.setViewInvisible()
                    mUserNick.text = story.author
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
                    findViewById<View>(R.id.bullet).setOnClickListener { mViewModel.onTagClick(this, story.categoryName) }
                }
                if (it.error != null) {
                    if (it.error.localizedMessage != null) it.error.localizedMessage.let {
                        findViewById<View>(android.R.id.content).showSnackbar(it)
                    } else {
                        it.error.nativeMessage?.let {
                            findViewById<View>(android.R.id.content).showSnackbar(it)
                        }
                    }
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
                if (story.commentsCount.toString() != mFooter.commentsTextView.text.toString()) {
                    mFooter.commentsTextView.text = story.commentsCount.toString()
                }
                it.storyTree.rootWrapper.voteUpdatingState?.let { votingState ->
                    if (votingState.state == UpdatingState.DONE) {
                        mFooter.setDizLikwProgress(false)
                        mFooter.setUpovteProgress(false)
                    } else {
                        when {
                            votingState.votingStrength < 0 -> {//downvoting progress
                                mFooter.setDizLikwProgress(true)
                                mFooter.setUpovteProgress(false)
                            }
                            votingState.votingStrength > 0 -> {//upvoting progress
                                mFooter.setDizLikwProgress(false)
                                mFooter.setUpovteProgress(true)
                            }
                            else -> {//vote removing progress
                                val currentStatus = it.storyTree.rootWrapper.voteStatus
                                if (currentStatus == GolosDiscussionItem.UserVoteType.VOTED) {//removing upvote
                                    mFooter.setDizLikwProgress(false)
                                    mFooter.setUpovteProgress(true)
                                } else if (currentStatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED) {//removing downvote
                                    mFooter.setDizLikwProgress(true)
                                    mFooter.setUpovteProgress(false)
                                }
                            }
                        }
                    }
                }

                mFooter.setViewVisible()
                if (story.commentsCount > 0 && it.storyTree.comments.isEmpty()) {//if comments not downloaded yet
                    mCommentsTv.setViewGone()
                    mSortLo.setViewGone()
                    mCommentsLoadingProgress.setViewVisible()
                    mCommentsRecycler.setViewGone()
                    if (isNeedToScrollToComments && !isScrollEventFired) {
                        mCommentsLoadingProgress.post { mCommentsLoadingProgress.requestFocus() }

                    }
                } else if (story.commentsCount > 0 && it.storyTree.comments.isNotEmpty()) {
                    mCommentsTv.setViewVisible()
                    mSortLo.setViewVisible()
                    val text = SpannableStringBuilder.valueOf(resources.getString(R.string.comments_with_count, story.commentsCount.toString()))
                    text.setSpan(ForegroundColorSpan(getColorCompat(R.color.gray_a6)),
                            text.length - story.commentsCount.length(), text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    mCommentsTv.text = text

                    mCommentsLoadingProgress.setViewGone()
                    mCommentsRecycler.setViewVisible()
                    if (isNeedToScrollToComments && !isScrollEventFired) {
                        mCommentsRecycler.post { mCommentsRecycler.requestFocus() }
                        isScrollEventFired = true
                    }
                } else if (story.commentsCount == 0) {
                    mCommentsTv.setViewGone()
                    mSortLo.setViewGone()
                    mCommentsLoadingProgress.setViewGone()
                    mCommentsRecycler.setViewGone()
                    if (isNeedToScrollToComments && !isScrollEventFired) {
                        mWriteCommentTv.post {
                            mWriteCommentTv.requestFocus()
                        }
                        isScrollEventFired = true
                    }
                }
                mSortSpinner.setSelection(it.commentsSortType.toCommentSortAdapterPosition())

                (mCommentsRecycler.adapter as CommentsAdapter).items = ArrayList(it.storyTree.comments)

                mWriteCommentTv.text = getString(if (it.storyTree.rootWrapper.story.commentsCount > 0) R.string.write_a_comment else R.string.no_comments)

                mFooter.moneyCountTextView.text = calculateShownReward(it.storyTree.rootWrapper,
                        ctx = this)

                if (it.storyTree.rootWrapper.isStoryEditable && mToolbar.menu?.findItem(R.id.of) == null) {
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
        mAvatar = findViewById(R.id.avatar_iv)
        mUserName = findViewById(R.id.user_name)
        mTagName = findViewById(R.id.tag_name)
        mFollowBlock = findViewById(R.id.follow_lo)
        mTagSubscribeBtn = findViewById(R.id.subscribe_tag_btn)
        mRebloggedBy = findViewById(R.id.reblogged_tv)
        mBlogNameTv = findViewById(R.id.blog_name_tv)
        mTitleTv = findViewById(R.id.title_tv)
        mStoryRecycler = findViewById(R.id.recycler)
        mFooter = findViewById(R.id.footer_lo)
        mCommentsRecycler = findViewById(R.id.comments_recycler)
        mFlow = findViewById(R.id.tags_lo)
        mSwipeToRefresh = findViewById(R.id.swipe_to_refresh)
        mWriteCommentLo = findViewById(R.id.write_a_comment_lo)
        mWriteCommentTv = findViewById(R.id.write_a_comment_tv)
        mShareButton = findViewById(R.id.share_btn)
        mCommentsLoadingProgress = findViewById(R.id.comments_progress)
        mAvatarOfAuthorInFollowLo = findViewById(R.id.avatar_in_follow_lo_iv)
        mTagAvatar = findViewById(R.id.tag_avatar)
        mNameOfAuthorInFollowLo = findViewById(R.id.author_name_in_follow_lo)
        mAuthorSubscribeButton = findViewById(R.id.follow_btn)
        mAuthorSubscribeProgress = findViewById(R.id.user_subscribe_progress)
        mUserNick = findViewById(R.id.user_nick_tv)
        mBottomImagesRecycler = findViewById(R.id.additional_images_recycler)
        mAppBar = findViewById(R.id.appbar)
        mCommentsTv = findViewById(R.id.comments_tv)
        mSortLo = findViewById(R.id.sort_lo)
        mSortSpinner = findViewById(R.id.sort_spinner)

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
                onUserVotesClick = {
                    mViewModel.onCommentVoteClick(this, it)
                },
                onEditClick = { mViewModel.onEditClick(this, it.story) })
        mStoryRecycler.adapter = StoryAdapter()
        mRebloggedBy.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_reblogged_black_20dp), null, null, null)
        // mCommentsTv.setCompoundDrawablesWithIntrinsicBounds(getVectorDrawable(R.drawable.ic_sort_red_24dp), null, null, null)
        (mStoryRecycler.itemAnimator as androidx.recyclerview.widget.SimpleItemAnimator).supportsChangeAnimations = false
        (mCommentsRecycler.itemAnimator as androidx.recyclerview.widget.SimpleItemAnimator).supportsChangeAnimations = false
        mSortSpinner.adapter = SpinnerAdapterWithDownChevron(this, R.array.comments_sort)
        mSortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < 4) mViewModel.changeCommentsSortType(position.toSortType())
            }
        }
        mWriteCommentLo.setOnClickListener {
            if (mViewModel.canUserWriteComments()) mViewModel.onWriteRootComment(this)
            else Snackbar.make(mWriteCommentLo, R.string.login_to_write_comment, Snackbar.LENGTH_SHORT).show()
        }

        (mStoryRecycler.adapter as StoryAdapter).onRowClick = { row, iv ->
            if (row is TextRow) mViewModel.onMainStoryTextClick(this, row.text)
            else if (row is ImageRow) mViewModel.onMainStoryImageClick(row.src)
        }
        mFooter.reblogImageView.setOnClickListener {
            if (!mViewModel.canUserVote) {
                mViewModel.onVoteRejected()
                return@setOnClickListener
            }
            if (mViewModel.storyLiveData.value?.storyTree?.rootWrapper?.isPostReposted == true) {
                findViewById<View>(android.R.id.content).showSnackbar(R.string.duplicate_reblog)
                return@setOnClickListener
            }
            ReblogConfirmalDialog.getInstance(0L).show(supportFragmentManager, null)
        }

        mSwipeToRefresh.setProgressBackgroundColorSchemeColor(getColorCompat(R.color.splash_back))
        mSwipeToRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent))
        mFooter.upvoteImageButton.setOnClickListener {
            onStoryVote(mViewModel.storyLiveData.value?.storyTree?.rootWrapper
                    ?: return@setOnClickListener, VoteDialog.DialogType.UPVOTE)
        }
        mFooter.dizLikeImageView.setOnClickListener {
            onStoryVote(mViewModel.storyLiveData.value?.storyTree?.rootWrapper
                    ?: return@setOnClickListener, VoteDialog.DialogType.DOWN_VOTE)
        }
        (mCommentsRecycler.adapter as CommentsAdapter).onUpvoteClick = {
            onStoryVote(it, VoteDialog.DialogType.UPVOTE)
        }
        (mCommentsRecycler.adapter as CommentsAdapter).onDownVoteClick = {

            onStoryVote(it, VoteDialog.DialogType.DOWN_VOTE)
        }
        (mCommentsRecycler.adapter as CommentsAdapter).onAnswerClick = {
            mViewModel.onAnswerToComment(this, it.story)
        }
        mSwipeToRefresh.isRefreshing = true
        mShareButton.setOnClickListener({ mViewModel.onShareClick(this) })
        mTagSubscribeBtn.setOnClickListener {
            if (mViewModel.canUserVote)
                mViewModel.onSubscribeToMainTagClick()
            else mViewModel.onVoteRejected()
        }

        mSwipeToRefresh.setOnRefreshListener(this)
        setSupportActionBar(mToolbar)
        mToolbar.setNavigationOnClickListener({ finish() })


    }

    private fun onStoryVote(item: StoryWrapper, type: VoteDialog.DialogType) {
        if (mViewModel.canUserVote) {
            if (item.voteStatus == GolosDiscussionItem.UserVoteType.FLAGED_DOWNVOTED && type == VoteDialog.DialogType.DOWN_VOTE) {
                ChangeVoteDialog.getInstance(item.story.id).show(supportFragmentManager, null)
            } else if (item.voteStatus == GolosDiscussionItem.UserVoteType.VOTED && type == VoteDialog.DialogType.UPVOTE) {
                ChangeVoteDialog.getInstance(item.story.id).show(supportFragmentManager, null)
            } else {
                val dialog = VoteDialog.getInstance(item.story.id, type)
                dialog.show(supportFragmentManager, null)
            }
        } else {
            mViewModel.onVoteRejected()
        }
    }

    override fun submitVote(id: Long, vote: Short, type: VoteDialog.DialogType) {
        val actualVote = if (type == VoteDialog.DialogType.UPVOTE) vote else (-vote).toShort()
        mViewModel.onStoryVote(id, actualVote)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (mViewModel.isPostEditable()) menuInflater.inflate(R.menu.story_menu_editable, menu)
        else menuInflater.inflate(R.menu.story_menu_not_editable, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.share -> mViewModel.onShareClick(this)
            R.id.edit -> mViewModel.onEditClick(this, mViewModel.storyLiveData.value?.storyTree?.rootWrapper?.story
                    ?: return true)
        }
        return true
    }

    private fun Int.toSortType() = when (this) {
        0 -> CommentsSortType.POPULARITY
        1 -> CommentsSortType.NEW_FIRST
        2 -> CommentsSortType.OLD_FIRST
        3 -> CommentsSortType.VOTES
        else -> throw IllegalStateException(" there is only 4 options, current is $this")
    }

    private fun CommentsSortType.toCommentSortAdapterPosition() = when (this) {
        CommentsSortType.POPULARITY -> 0
        CommentsSortType.NEW_FIRST -> 1
        CommentsSortType.OLD_FIRST -> 2
        CommentsSortType.VOTES -> 3
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.onStop()
    }

    override fun oReblogConfirmed(id: Long) {
        mViewModel.onRootStoryReblog()
    }

    override fun onChangeConfirm(id: Long) {
        mViewModel.onStoryVote(id, 0)
    }

    private fun setFullscreenProgress(isShown: Boolean) {
        if (isShown) {
            mAppBar.setViewGone()
            mSwipeToRefresh.setViewGone()
            mWriteCommentLo.setViewGone()
            mProgressBar.setViewVisible()
        } else {
            mAppBar.setViewVisible()
            mSwipeToRefresh.setViewVisible()
            mWriteCommentLo.setViewVisible()
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