package io.golos.golos.screens.events

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.services.EventType
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.getColorCompat
import io.golos.golos.utils.setViewGone
import io.golos.golos.utils.setViewVisible
import io.golos.golos.utils.toArrayList

class EventsListFragment : GolosFragment(), SwipeRefreshLayout.OnRefreshListener, Observer<EventsList> {


    override fun onChanged(t: EventsList?) {
        t?.let { eventsList ->
            val events = eventsList.events
            if (mSwipeToRefresh?.isRefreshing == true) mSwipeToRefresh?.isRefreshing = false
            if (events.isEmpty()) {
                mSwipeToRefresh?.setViewGone()
                mLabel?.setViewVisible()
            } else {
                mSwipeToRefresh?.setViewVisible()
                mLabel?.setViewGone()
                (mRecycler?.adapter as?  EventsListAdapter)?.notification = events
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fr_events_list, container, false)

        mRecycler = view.findViewById(R.id.recycler)
        mSwipeToRefresh = view.findViewById(R.id.swipe_refresh)
        mLabel = view.findViewById(R.id.fullscreen_label)
        mViewModel = ViewModelProviders.of(this).get(EventsViewModel::class.java)
        mSwipeToRefresh?.setOnRefreshListener(this)
        mSwipeToRefresh?.setProgressBackgroundColorSchemeColor(getColorCompat(R.color.splash_back))
        mSwipeToRefresh?.setColorSchemeColors(ContextCompat.getColor(view.context, R.color.blue_dark))

        mRecycler?.adapter = EventsListAdapter(emptyList(), {
            mViewModel?.onEventClick(activity ?: return@EventsListAdapter, it)
        })
        return view
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        mViewModel?.onChangeVisibilityToUser(isVisibleToUser)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel?.onCreate(getEventTypes(arguments ?: return), Repository.get)
        mViewModel?.eventsList?.observe(this, this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("EventsListFragmentIS", mRecycler?.layoutManager?.onSaveInstanceState())
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            mRecycler?.layoutManager?.onRestoreInstanceState(it.getParcelable("EventsListFragmentIS"))
        }
    }


    override fun onStop() {
        super.onStop()
        mViewModel?.onStop()
    }

    override fun onStart() {
        super.onStart()
        mViewModel?.onStart()
    }


    companion object {

        fun getInstance(types: List<EventType>?): EventsListFragment {
            val f = EventsListFragment()
                    .apply {
                        arguments = Bundle()
                                .apply {
                                    setEventTypes(types, this)
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


