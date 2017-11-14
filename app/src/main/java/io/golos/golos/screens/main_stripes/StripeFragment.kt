package io.golos.golos.screens.main_stripes

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.screens.OnVoteSubmit
import io.golos.golos.screens.VoteDialog
import io.golos.golos.screens.main_stripes.adapters.StripeAdapter
import io.golos.golos.screens.main_stripes.model.StripeType
import io.golos.golos.screens.main_stripes.viewmodel.*
import io.golos.golos.utils.ErrorCodes
import io.golos.golos.utils.showSnackbar
import timber.log.Timber


/**
 * Created by yuri on 31.10.17.
 */
class StripeFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private var mRecycler: RecyclerView? = null
    private var mSwipeRefresh: SwipeRefreshLayout? = null
    private var mViewModel: StripeFragmentViewModel? = null
    private lateinit var mAdapter: StripeAdapter
    private var isVisibleBacking = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fr_stripe, container, false)
        mRecycler = view.findViewById(R.id.recycler)
        mSwipeRefresh = view.findViewById(R.id.swipe_refresh)
        mSwipeRefresh?.setColorSchemeColors(ContextCompat.getColor(container!!.context, R.color.colorPrimaryDark))
        mSwipeRefresh?.setOnRefreshListener(this)
        val manager = LinearLayoutManager(container!!.context)
        mRecycler?.layoutManager = manager
        val provider = ViewModelProviders.of(this)
        val type: StripeType = arguments.getSerializable(TYPE_TAG) as StripeType
        mViewModel = when (type) {
            StripeType.NEW -> provider.get(NewViewModel::class.java)
            StripeType.ACTUAL -> provider.get(ActualViewModle::class.java)
            StripeType.POPULAR -> provider.get(PopularViewModel::class.java)
            StripeType.PROMO -> provider.get(PromoViewModel::class.java)
        }
        mAdapter = StripeAdapter(
                onCardClick = { mViewModel?.onCardClick(it, activity) },
                onCommentsClick = { mViewModel?.onCommentsClick(it, activity) },
                onShareClick = { mViewModel?.onShareClick(it, activity) },
                onUpvoteClick = {
                    if (it.isUserUpvotedOnThis) {
                        mViewModel?.downVote(it)
                    } else {
                        val dialog = VoteDialog.getInstance()
                        dialog.selectPowerListener = object : OnVoteSubmit {
                            override fun submitVote(vote: Int) {
                                Timber.e(it.title)
                                mViewModel?.vote(it, vote)
                            }
                        }
                        dialog.show(activity.fragmentManager, null)
                    }
                }
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
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel?.onChangeVisibilityToUser(isVisibleBacking)
        mViewModel?.stripeLiveData?.observe(this, Observer {
            if (it?.isLoading == true) {
                mSwipeRefresh?.post({ mSwipeRefresh?.isRefreshing = false })
                mSwipeRefresh?.post({ mSwipeRefresh?.isRefreshing = true })
            } else {
                if (mSwipeRefresh?.isRefreshing == true) {
                    mSwipeRefresh?.post({ mSwipeRefresh?.isRefreshing = false })
                }
            }

            if (it?.items != null) {
                mRecycler?.post { mAdapter.setStripesCustom(it.items) }

            }
            it?.error?.let {
                if (getView() == null) {
                    return@let
                }
                var message = R.string.unknown_error
                if (it == ErrorCodes.ERROR_SLOW_CONNECTION) {
                    message = R.string.slow_internet_connection

                } else if (it == ErrorCodes.ERROR_NO_CONNECTION) {
                    message = R.string.no_internet_connection
                }
                getView()?.showSnackbar(message)
            }
            it?.messageLocalized?.let {
                getView()?.showSnackbar(it)
            }
            it?.message?.let { getView()?.showSnackbar(it) }
        })

    }

    companion object {
        private val TYPE_TAG = "TYPE_TAG"
        fun getInstance(type: StripeType): StripeFragment {
            val fr = StripeFragment()
            val bundle = Bundle()
            bundle.putSerializable(TYPE_TAG, type)
            fr.arguments = bundle
            return fr
        }
    }

    override fun onRefresh() {
        mViewModel?.onSwipeToRefresh()
        Handler().postDelayed({ mSwipeRefresh?.isRefreshing = false }, 2000)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        isVisibleBacking = isVisibleToUser
        mViewModel?.onChangeVisibilityToUser(isVisibleToUser)
    }
}