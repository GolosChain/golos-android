package io.golos.golos.screens.stories

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.repository.StoryFilter
import io.golos.golos.repository.model.mapper
import io.golos.golos.screens.stories.adapters.StripeAdapter
import io.golos.golos.screens.stories.adapters.StoriesPagerAdpater
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.stories.viewmodel.*
import io.golos.golos.screens.widgets.OnVoteSubmit
import io.golos.golos.screens.widgets.VoteDialog
import io.golos.golos.utils.showSnackbar


/**
 * Created by yuri on 31.10.17.
 */
class StoriesFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, Observer<StoriesViewState> {
    private var mRecycler: RecyclerView? = null
    private var mSwipeRefresh: SwipeRefreshLayout? = null
    private var mViewModel: StoriesViewModel? = null
    private lateinit var mAdapter: StripeAdapter
    private lateinit var mFullscreenMessageLabel: TextView
    private var isVisibleBacking = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fr_stripe, container, false)
        bindViews(view)
        setUp()
        return view
    }

    private fun bindViews(view: View) {
        mRecycler = view.findViewById(R.id.recycler)
        mSwipeRefresh = view.findViewById(R.id.swipe_refresh)
        mSwipeRefresh?.setColorSchemeColors(ContextCompat.getColor(view.context, R.color.blue_dark))
        mSwipeRefresh?.setOnRefreshListener(this)
        mFullscreenMessageLabel = view.findViewById(R.id.fullscreen_label)
        val manager = LinearLayoutManager(view.context)
        mRecycler?.layoutManager = manager
        val provider = ViewModelProviders.of(activity!!)
        val type: FeedType = arguments!!.getSerializable(TYPE_TAG) as FeedType
        val filter = arguments!!.getString(FILTER_TAG, null)
        mViewModel = when (type) {
            FeedType.NEW -> provider.get(NewViewModel::class.java)
            FeedType.ACTUAL -> provider.get(ActualViewModle::class.java)
            FeedType.POPULAR -> provider.get(PopularViewModel::class.java)
            FeedType.PROMO -> provider.get(PromoViewModel::class.java)
            FeedType.PERSONAL_FEED -> provider.get(FeedViewModel::class.java)
        }
        mViewModel!!.filter = if (filter == null || filter == "null") null
        else mapper.readValue(filter, StoryFilter::class.java)
        mAdapter = StripeAdapter(
                onCardClick = { mViewModel?.onCardClick(it, activity) },
                onCommentsClick = { mViewModel?.onCommentsClick(it, activity) },
                onShareClick = { mViewModel?.onShareClick(it, activity) },
                onUpvoteClick = {
                    if (mViewModel?.canVote == true) {
                        if (it.rootStory()?.isUserUpvotedOnThis == true) {
                            mViewModel?.downVote(it)
                        } else {
                            val dialog = VoteDialog.getInstance()
                            dialog.selectPowerListener = object : OnVoteSubmit {
                                override fun submitVote(vote: Short) {
                                    mViewModel?.vote(it, vote)
                                }
                            }
                            dialog.show(activity!!.fragmentManager, null)
                        }
                    } else {
                        mViewModel?.onVoteRejected(it)
                    }
                },
                onTagClick = { mViewModel?.onBlogClick(context, it) }
        )
        mRecycler?.adapter = mAdapter
        (mRecycler?.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        mRecycler?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val position = manager.findLastCompletelyVisibleItemPosition()
                if (position + 10 > mAdapter.itemCount) {
                    mViewModel?.onScrollToTheEnd()
                }
            }
        })
        mRecycler?.recycledViewPool = StoriesPagerAdpater.sharedPool
    }

    private fun setUp() {
        mViewModel?.onChangeVisibilityToUser(isVisibleBacking)
        mViewModel?.storiesLiveData?.removeObservers(activity as LifecycleOwner)
        mViewModel?.storiesLiveData?.observe(activity as LifecycleOwner, this)
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
        if (isVisible) {
            t?.error?.let {
                if (it.localizedMessage != null) activity
                        ?.findViewById<View>(android.R.id.content)
                        ?.showSnackbar(it.localizedMessage)
                else if (it.nativeMessage != null) activity
                        ?.findViewById<View>(android.R.id.content)
                        ?.showSnackbar(it.nativeMessage)
                else {
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

    companion object {
        val TYPE_TAG = "TYPE_TAG"
        val FILTER_TAG = "FILTER_TAG"
        fun getInstance(type: FeedType,
                        filter: StoryFilter? = null): StoriesFragment {
            val fr = StoriesFragment()
            val bundle = Bundle()
            bundle.putSerializable(TYPE_TAG, type)
            if (filter != null)
                bundle.putString(FILTER_TAG, mapper.writeValueAsString(filter))
            else bundle.putString(FILTER_TAG, null)

            fr.arguments = bundle
            return fr
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
}