package io.golos.golos.screens.events

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.services.EventType
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.*
import java.util.concurrent.TimeUnit

class EventsListFragment : GolosFragment(), SwipeRefreshLayout.OnRefreshListener, Observer<EventsListState> {
    private var parcelable: Parcelable? = null
    private var mLastSentScrollEvent = Long.MIN_VALUE
    private val mScrollEventTimeOut = 500


    override fun onChanged(t: EventsListState?) {

        t?.let { eventsList ->

            val list = eventsList.events
            val updatingState = eventsList.updatingState

            if (mSwipeToRefresh?.isRefreshing == true) mSwipeToRefresh?.isRefreshing = false
            when {
                updatingState == UpdatingState.UPDATING && list.isEmpty() -> {
                    mProgressView?.setViewVisible()
                    mSwipeToRefresh?.setViewGone()
                    mLabel?.setViewGone()
                }
                list.isEmpty() -> {
                    mSwipeToRefresh?.setViewGone()
                    mProgressView?.setViewGone()
                    mLabel?.setViewVisible()
                }
                else -> {
                    mSwipeToRefresh?.setViewVisible()
                    mLabel?.setViewGone()
                    mProgressView?.setViewGone()
                    mRecycler?.post {

                        (mRecycler?.adapter as?  EventsListAdapter)?.items = list

                        if (parcelable != null && list.isNotEmpty()) {

                            (mRecycler?.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)?.onRestoreInstanceState(parcelable)
                            parcelable = null
                        }
                    }

                }
            }
        }
    }


    override fun onRefresh() {
        mViewModel?.requestUpdate()
    }

    private var mRecycler: androidx.recyclerview.widget.RecyclerView? = null
    private var mSwipeToRefresh: SwipeRefreshLayout? = null
    private var mLabel: TextView? = null
    private var mViewModel: EventsViewModel? = null
    private var mLoadingProgress: View? = null
    private var isVisibleToUserHint: Boolean = false
    private var isParentVisible: Boolean = false
    private var mProgressView: View? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fr_events_list, container, false)

        mRecycler = view.findViewById(R.id.recycler)
        mSwipeToRefresh = view.findViewById(R.id.swipe_refresh)
        mLabel = view.findViewById(R.id.fullscreen_label)
        mViewModel = ViewModelProviders.of(this).get(EventsViewModel::class.java)
        mLoadingProgress = view.findViewById(R.id.loading_progress)
        mProgressView = view.findViewById(R.id.progress)
        mSwipeToRefresh?.setOnRefreshListener(this)
        mSwipeToRefresh?.setProgressBackgroundColorSchemeColor(getColorCompat(R.color.splash_back))
        mSwipeToRefresh?.setColorSchemeColors(ContextCompat.getColor(view.context, R.color.blue_dark))

        mRecycler?.adapter = EventsListAdapter(emptyList(), {
            mViewModel?.onEventClick(activity ?: return@EventsListAdapter, it)
        },
                { mViewModel?.onScrollToTheEnd() },
                { mViewModel?.onFollowClick(it) },
                { mViewModel?.onAvatarClick(it, activity ?: return@EventsListAdapter) },
                object : OnItemShowListener {
                    override fun onItemShow(item: EventListItem) {
                        // mViewModel?.onItemShow(item)
                    }
                })
        mViewModel?.updateState?.observe(this, Observer<Boolean> {

        })
        mRecycler?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                onScrolled()

            }
        })
        (mRecycler?.itemAnimator as androidx.recyclerview.widget.SimpleItemAnimator).supportsChangeAnimations = false
        mLabel?.text = getTextForNoEvents()

        setUp()
        return view
    }

    private fun onScrolled() {
        if ((mLastSentScrollEvent + mScrollEventTimeOut) > System.currentTimeMillis()) return
        val first = (mRecycler?.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
                ?: 0
        val last = (mRecycler?.layoutManager as? LinearLayoutManager)?.findLastVisibleItemPosition()
                ?: 0
        val events = mViewModel?.eventsList?.value?.events ?: return

        if (first > -1 && last > -1 && events.isNotEmpty() && first < events.size) {
            mViewModel?.onItemShow(events.subList(first, last).asSequence().filter { it is EventContainingItem }.map { (it as EventContainingItem).event }.toList())
        }
        mLastSentScrollEvent = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()

    }

    private fun setUp() {
        mViewModel?.onCreate(getEventTypes(arguments
                ?: return), Repository.get,
                EventsSorterUseCase(object : StringProvider {
                    override fun get(resId: Int, args: String?) = getString(resId, args)
                }),
                Repository.get,
                Repository.get,
                Repository.get,
                Repository.get,
                Repository.get.notificationsRepository)
        mViewModel?.eventsList?.observe(this, this)
        (parentFragment as? ReselectionEmitter)?.reselectLiveData?.observe(this, Observer {
            it ?: return@Observer

            if (it == arguments?.getInt(POSITION, Int.MIN_VALUE)
                    && mRecycler?.childCount != 0) {
                mRecycler?.post { mRecycler?.scrollToPosition(0) }
            }
        })
        (parentFragment as ParentVisibilityChangeEmitter?)?.status?.observe(this, Observer {
            it ?: return@Observer
            isParentVisible = it.isParentVisible
            val myPosition = arguments?.getInt(POSITION, Int.MIN_VALUE) ?: return@Observer
            if (myPosition != it.currentSelectedFragment) return@Observer
            mViewModel?.onChangeVisibilityToUser(it.isParentVisible)
            if (it.isParentVisible) mRecycler?.postDelayed({ onScrolled() }, 100)
        })
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        this.isVisibleToUserHint = isVisibleToUser
        mViewModel?.onChangeVisibilityToUser(isVisibleToUser && isParentVisible)
        if (isVisibleToUser) mRecycler?.postDelayed({ onScrolled() }, 100)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("EventsListFragment123", mRecycler?.layoutManager?.onSaveInstanceState())
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        parcelable = savedInstanceState?.getParcelable("EventsListFragment123")
    }


    override fun onStop() {
        super.onStop()
        mViewModel?.onStop()

    }


    override fun onStart() {
        super.onStart()
        mViewModel?.onStart()
        if (isVisibleToUserHint) mViewModel?.onChangeVisibilityToUser(isVisibleToUserHint && isParentVisible)
    }

    fun getTextForNoEvents(): CharSequence {
        val start = getString(R.string.empty)
        val types = getEventTypes(arguments ?: return start)
        return when {
            types == null -> start.plus("\n").plus(getString(R.string.no_events_all))
            types.compareContents(listOf(EventType.REWARD, EventType.CURATOR_AWARD)) ->
                start
            types.compareContents(listOf(EventType.REPLY)) ->
                start.plus("\n").plus(getString(R.string.no_events_replies))
            types.compareContents(listOf(EventType.VOTE, EventType.FLAG, EventType.SUBSCRIBE, EventType.UNSUBSCRIBE, EventType.REPOST)) ->
                start.plus("\n").plus(getString(R.string.no_events_social))
            types.compareContents(listOf(EventType.MENTION)) ->
                start.plus("\n").plus(getString(R.string.no_events_mentions))
            else -> start
        }
    }


    companion object {

        private val POSITION = "position"

        fun getInstance(types: List<EventType>?, position: Int): EventsListFragment {
            val f = EventsListFragment()
                    .apply {
                        arguments = Bundle()
                                .apply {
                                    setEventTypes(types, this)
                                    putInt(POSITION, position)
                                }
                    }
            return f
        }

        fun getEventTypes(b: Bundle): List<EventType>? {
            return if (b.containsKey("event_types"))
                b.getStringArrayList("event_types").map { EventType.valueOf(it) }
            else null
        }


        private fun setEventTypes(types: List<EventType>?, b: Bundle) {
            b.putStringArrayList("event_types", types?.map { it.name }?.toArrayList() ?: return)
        }
    }
}


