package io.golos.golos.screens.events

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import io.golos.golos.R
import io.golos.golos.repository.services.EventType
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.StringProvider

class EventsPagerAdapter(private val textProvider: StringProvider,
                         fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    private val fragments: HashMap<Int, GolosFragment> = hashMapOf()
    override fun getItem(position: Int): Fragment {
        fragments[position] = when (position) {
            0 -> EventsListFragment.getInstance(null, position)
            1 -> EventsListFragment.getInstance(listOf(EventType.REWARD, EventType.CURATOR_AWARD), position)
            2 -> EventsListFragment.getInstance(listOf(EventType.REPLY), position)
            3 -> EventsListFragment.getInstance(listOf(EventType.VOTE, EventType.FLAG, EventType.SUBSCRIBE, EventType.UNSUBSCRIBE, EventType.REPOST), position)
            4 -> EventsListFragment.getInstance(listOf(EventType.MENTION), position)
            else -> EventsListFragment.getInstance(null, position)
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