package io.golos.golos.screens.events

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.StringProvider

class EventsHolderFragment : GolosFragment() {
    private var mPager: ViewPager? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fr_events_holder, container, false)
        mPager = v.findViewById<ViewPager>(R.id.holder_view_pager)
        mPager?.adapter = EventsPagerAdapter(object : StringProvider {
            override fun get(resId: Int, args: String?): String {
                return getString(resId, args)
            }
        }, childFragmentManager)
        val tabLo = v.findViewById<TabLayout>(R.id.tab_lo)
        tabLo.setupWithViewPager(mPager)
        mPager?.offscreenPageLimit = 1
        return v
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        (mPager?.adapter as? EventsPagerAdapter)?.onUserVisibilityChanged(isVisibleToUser, mPager?.currentItem
                ?: return)
    }
}