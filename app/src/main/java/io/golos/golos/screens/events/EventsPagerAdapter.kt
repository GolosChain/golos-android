package io.golos.golos.screens.events

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import io.golos.golos.R
import io.golos.golos.repository.services.EventType
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.StringProvider

class EventsPagerAdapter(private val textProvider: StringProvider,
                         fm: FragmentManager) : FragmentPagerAdapter(fm) {
    private val fragments: HashMap<Int, GolosFragment> = hashMapOf()
    override fun getItem(position: Int): Fragment {
        fragments[position] = when (position) {
            0 -> EventsListFragment.getInstance(null)
            1 -> EventsListFragment.getInstance(listOf(EventType.AWARD, EventType.CURATOR_AWARD))
            2 -> EventsListFragment.getInstance(listOf(EventType.REPLY))
            3 -> EventsListFragment.getInstance(listOf(EventType.VOTE, EventType.FLAG, EventType.SUBSCRIBE, EventType.UNSUBSCRIBE, EventType.REPOST))
            4 -> EventsListFragment.getInstance(listOf(EventType.MENTION))
            else -> EventsListFragment.getInstance(null)
        }
        return fragments[position]!!
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> textProvider.get(R.string.all).capitalize()
            1 -> textProvider.get(R.string.awards).capitalize()
            2 -> textProvider.get(R.string.replys).capitalize()
            3 -> textProvider.get(R.string.social).capitalize()
            4 -> textProvider.get(R.string.mentions).capitalize()
            else -> ""
        }
    }

    override fun getCount(): Int = 5
    fun onUserVisibilityChanged(visibleToUser: Boolean, position: Int) {
        fragments[position]?.userVisibleHint = visibleToUser

    }
}