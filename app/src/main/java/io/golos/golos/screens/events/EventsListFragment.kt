package io.golos.golos.screens.events

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
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
import io.golos.golos.repository.Repository
import io.golos.golos.repository.services.EventType
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.*

class EventsListFragment : GolosFragment(), SwipeRefreshLayout.OnRefreshListener, Observer<List<EventsListItemWrapper>> {
    private var parcelable: Parcelable? = null


    override fun onChanged(t: List<EventsListItemWrapper>?) {

        t?.let { eventsList ->

            if (mSwipeToRefresh?.isRefreshing == true) mSwipeToRefresh?.isRefreshing = false
            if (eventsList.isEmpty()) {
                mSwipeToRefresh?.setViewGone()
                mLabel?.setViewVisible()
            } else {
                mSwipeToRefresh?.setViewVisible()
                mLabel?.setViewGone()
                mRecycler?.post {
                    (mRecycler?.adapter as?  EventsListAdapter)?.items = eventsList

                    if (parcelable != null && eventsList.isNotEmpty()) {

                        (mRecycler?.layoutManager as? LinearLayoutManager)?.onRestoreInstanceState(parcelable)
                        parcelable = null
                    }
                }

            }
        }
    }


    override fun onRefresh() {
        mViewModel?.requestUpdate()
    }

    private var mRecycler: RecyclerView? = null
    private var mSwipeToRefresh: SwipeRefreshLayout? = null
    private var mLabel: TextView? = null
    private var mViewModel: EventsViewModel? = null
    private var mLoadingProgress: View? = null
    private var isVisibleToUser: Boolean = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fr_events_list, container, false)

        mRecycler = view.findViewById(R.id.recycler)
        mSwipeToRefresh = view.findViewById(R.id.swipe_refresh)
        mLabel = view.findViewById(R.id.fullscreen_label)
        mViewModel = ViewModelProviders.of(this).get(EventsViewModel::class.java)
        mLoadingProgress = view.findViewById(R.id.loading_progress)
        mSwipeToRefresh?.setOnRefreshListener(this)
        mSwipeToRefresh?.setProgressBackgroundColorSchemeColor(getColorCompat(R.color.splash_back))
        mSwipeToRefresh?.setColorSchemeColors(ContextCompat.getColor(view.context, R.color.blue_dark))

        mRecycler?.adapter = EventsListAdapter(emptyList(), {
            mViewModel?.onEventClick(activity ?: return@EventsListAdapter, it)
        },
                { mViewModel?.onScrollToTheEnd() },
                { mViewModel?.onFollowClick(it) },
                { mViewModel?.onAvatarClick(it, activity ?: return@EventsListAdapter) })
        mViewModel?.updateState?.observe(this, Observer<Boolean> {

        })
        (mRecycler?.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        return view
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        this.isVisibleToUser = isVisibleToUser
        mViewModel?.onChangeVisibilityToUser(isVisibleToUser)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel?.onCreate(getEventTypes(arguments
                ?: return), Repository.get,
                EventsSorterUseCase(object : StringProvider {
                    override fun get(resId: Int, args: String?) = getString(resId, args)
                }),
                Repository.get,
                Repository.get,
                Repository.get,
                Repository.get)
        mViewModel?.eventsList?.observe(this, this)
        (parentFragment as? ReselectionEmitter)?.reselectLiveData?.observe(this, Observer {
            if (it == arguments?.getInt(POSITION, Int.MIN_VALUE) && mRecycler?.childCount != 0) {
                mRecycler?.post { mRecycler?.scrollToPosition(0) }
            }
        })
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
        if (isVisibleToUser) mViewModel?.onChangeVisibilityToUser(isVisibleToUser)
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


