package io.golos.golos.screens.stories

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.repository.UserSettingsRepository
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.editor.prepend
import io.golos.golos.screens.stories.adapters.FeedCellSettings
import io.golos.golos.screens.stories.adapters.StoriesRecyclerAdapter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.stories.model.NSFWStrategy
import io.golos.golos.screens.stories.model.StoryWithCommentsClickListener
import io.golos.golos.screens.stories.viewmodel.StoriesModelFactory
import io.golos.golos.screens.stories.viewmodel.StoriesViewModel
import io.golos.golos.screens.stories.viewmodel.StoriesViewState
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.widgets.dialogs.OnVoteSubmit
import io.golos.golos.screens.widgets.dialogs.VoteDialog
import io.golos.golos.utils.*
import timber.log.Timber

class StoriesFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, Observer<StoriesViewState> {
    private var mRecycler: RecyclerView? = null
    private var mSwipeRefresh: SwipeRefreshLayout? = null
    private var mViewModel: StoriesViewModel? = null
    private var mRefreshButton: TextView? = null
    private lateinit var mAdapter: StoriesRecyclerAdapter
    private lateinit var mFullscreenMessageLabel: TextView
    private var isVisibleBacking: Boolean? = null
    private var lastSentUpdateRequestTime = System.currentTimeMillis()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fr_stories, container, false)

        bindViews(view)
        setUp()
        return view
    }

    private fun bindViews(view: View) {
        mRecycler = view.findViewById(R.id.recycler)
        mSwipeRefresh = view.findViewById(R.id.swipe_refresh)
        mRefreshButton = view.findViewById(R.id.refresh_btn)
        mSwipeRefresh?.setColorSchemeColors(ContextCompat.getColor(view.context, R.color.blue_dark))
        mSwipeRefresh?.setOnRefreshListener(this)
        mFullscreenMessageLabel = view.findViewById(R.id.fullscreen_label)
        val manager = MyLinearLayoutManager(view.context)
        mRecycler?.layoutManager = manager
        mSwipeRefresh?.setProgressBackgroundColorSchemeColor(getColorCompat(R.color.splash_back))

        val ssb = SpannableStringBuilder.valueOf(getString(R.string.refresh_question))
        ssb.prepend("    ")
        ssb.setSpan(CenterVerticalDrawableSpan(mRefreshButton?.getVectorDrawableWithIntrinisticSize(R.drawable.ic_refresh_white_14dp)),
                0, 1, SPAN_INCLUSIVE_INCLUSIVE)

        mRefreshButton?.text = ssb

        mSwipeRefresh?.setColorSchemeResources(R.color.blue_light)
        if (arguments?.getSerializable(TYPE_TAG) == null) return

        val type: FeedType = arguments!!.getSerializable(TYPE_TAG) as FeedType
        val filterString = arguments!!.getString(FILTER_TAG, null)
        val filter = if (filterString == null || filterString == "null") null
        else mapper.readValue(filterString, StoryFilter::class.java)

        mViewModel = StoriesModelFactory.getStoriesViewModel(type, null, this, filter)

        mRecycler?.adapter = null
        mAdapter = StoriesRecyclerAdapter(
                onCardClick = object : StoryWithCommentsClickListener {
                    override fun onClick(story: StoryWithComments) {
                        mViewModel?.onCardClick(story, activity)
                    }
                },
                onCommentsClick = object : StoryWithCommentsClickListener {
                    override fun onClick(story: StoryWithComments) {
                        mViewModel?.onCommentsClick(story, activity)
                    }
                },
                onShareClick = object : StoryWithCommentsClickListener {
                    override fun onClick(story: StoryWithComments) {
                        mViewModel?.onShareClick(story, activity)
                    }
                },
                onUpvoteClick = object : StoryWithCommentsClickListener {
                    override fun onClick(story: StoryWithComments) {
                        if (mViewModel?.canVote() == true) {
                            if (story.rootStory()?.userVotestatus == GolosDiscussionItem.UserVoteType.VOTED) {
                                mViewModel?.cancelVote(story)
                            } else {
                                val dialog = VoteDialog.getInstance()
                                dialog.selectPowerListener = object : OnVoteSubmit {
                                    override fun submitVote(vote: Short) {
                                        mViewModel?.vote(story, vote)
                                    }
                                }
                                dialog.show(activity!!.supportFragmentManager, null)
                            }
                        } else {
                            mViewModel?.onVoteRejected(story)
                        }
                    }
                },
                onTagClick = object : StoryWithCommentsClickListener {
                    override fun onClick(story: StoryWithComments) {
                        mViewModel?.onBlogClick(story, activity)
                    }
                },
                onUserClick = object : StoryWithCommentsClickListener {
                    override fun onClick(story: StoryWithComments) {
                        mViewModel?.onUserClick(story, activity)
                    }
                },
                onVotersClick = object : StoryWithCommentsClickListener {
                    override fun onClick(story: StoryWithComments) {
                        mViewModel?.onVotersClick(story, activity)
                    }
                },
                feedCellSettings = mViewModel?.cellViewSettingLiveData?.value
                        ?: FeedCellSettings(true,
                                true,
                                NSFWStrategy(true, Pair(false, "")),
                                UserSettingsRepository.GolosCurrency.USD,
                                UserSettingsRepository.GolosBountyDisplay.THREE_PLACES))

        mRecycler?.adapter = mAdapter
        (mRecycler?.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        mRecycler?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val position = manager.findLastCompletelyVisibleItemPosition()
                if (position + 10 > mAdapter.itemCount
                        && ((System.currentTimeMillis() - lastSentUpdateRequestTime) > 1_000)) {
                    mViewModel?.onScrollToTheEnd()
                    lastSentUpdateRequestTime = System.currentTimeMillis()
                }
            }
        })
        mRefreshButton?.setOnClickListener { mViewModel?.onSwipeToRefresh() }
    }

    private fun setUp() {

        if (isVisibleBacking == true) {
            mViewModel?.onChangeVisibilityToUser(true)
        }
        mViewModel?.storiesLiveData?.removeObservers(activity as LifecycleOwner)
        mViewModel?.storiesLiveData?.observe(activity as LifecycleOwner, this)

        val observer = Observer<FeedCellSettings> { it ->
            mAdapter.feedCellSettings = it ?: FeedCellSettings(true,
                    true,
                    NSFWStrategy(true, Pair(false, "")),
                    UserSettingsRepository.GolosCurrency.USD,
                    UserSettingsRepository.GolosBountyDisplay.THREE_PLACES)
        }
        mViewModel?.cellViewSettingLiveData?.observe(activity as LifecycleOwner, observer)
    }

    override fun onChanged(t: StoriesViewState?) {
        if (t?.isLoading == true) {
            if (mSwipeRefresh?.isRefreshing == false) {
                mSwipeRefresh?.post({ mSwipeRefresh?.isRefreshing = false })
                mSwipeRefresh?.post({ mSwipeRefresh?.isRefreshing = true })
            }
        } else {
            if (mSwipeRefresh?.isRefreshing == true) {
                mSwipeRefresh?.post({ mSwipeRefresh?.isRefreshing = false })
            }
        }

        if (t?.items != null) {
            mRecycler?.post { mAdapter.setStripesCustom(t.items) }
        }

        if (t?.showRefreshButton == true) {
            mRefreshButton?.setViewVisible()
        } else {

            mRefreshButton?.setViewGone()
        }

        if (isVisible) {
            t?.error?.let {
                when {
                    it.localizedMessage != null -> activity
                            ?.findViewById<View>(android.R.id.content)
                            ?.showSnackbar(it.localizedMessage)
                    it.nativeMessage != null -> activity
                            ?.findViewById<View>(android.R.id.content)
                            ?.showSnackbar(it.nativeMessage)
                    else -> {
                    }
                }
            }
        }
        t?.popupMessage?.let {
            view?.showSnackbar(it)
        }
        t?.fullscreenMessage?.let {
            mRecycler?.visibility = View.GONE
            mFullscreenMessageLabel.visibility = View.VISIBLE
            mFullscreenMessageLabel.setText(it)
        }
        if (t?.fullscreenMessage == null) {
            mRecycler?.visibility = View.VISIBLE
            mFullscreenMessageLabel.visibility = View.GONE
        }
    }


    override fun onRefresh() {
        mViewModel?.onSwipeToRefresh()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        isVisibleBacking = isVisibleToUser
        mViewModel?.onChangeVisibilityToUser(isVisibleToUser)

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val parc = mRecycler?.layoutManager as? LinearLayoutManager
        parc?.onSaveInstanceState().let {
            outState.putParcelable("recyclerState", it)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel?.onDestroy()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val parc = savedInstanceState?.getParcelable<Parcelable>("recyclerState")
        parc?.let {
            (mRecycler?.layoutManager as? LinearLayoutManager)?.let {
                it.onRestoreInstanceState(parc)
            }
        }
    }

    fun getArgs(): Pair<FeedType, StoryFilter?> {
        val type: FeedType = arguments!!.getSerializable(TYPE_TAG) as FeedType
        val filterString = arguments!!.getString(FILTER_TAG, null)
        val filter = if (filterString == null || filterString == "null") null else mapper.readValue(filterString, StoryFilter::class.java)

        return Pair(type, filter)
    }

    companion object {
        private val TYPE_TAG = "TYPE_TAG"
        private val FILTER_TAG = "FILTER_TAG"

        fun getInstance(type: FeedType,
                        filter: StoryFilter? = null): StoriesFragment {
            val fr = StoriesFragment()
            val bundle = createArguments(type, filter)
            fr.arguments = bundle
            return fr
        }

        fun createArguments(type: FeedType,
                            filter: StoryFilter? = null): Bundle {
            val bundle = Bundle()
            bundle.putSerializable(TYPE_TAG, type)
            if (filter != null)
                bundle.putString(FILTER_TAG, mapper.writeValueAsString(filter))
            else bundle.putString(FILTER_TAG, null)
            return bundle
        }
    }
}