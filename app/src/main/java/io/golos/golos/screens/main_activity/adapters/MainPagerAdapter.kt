package io.golos.golos.screens.main_activity.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import io.golos.golos.screens.events.EventsHolderFragment
import io.golos.golos.screens.main_activity.MainActivity.Companion.FILTERED_BY_TAG_STORIES
import io.golos.golos.screens.main_activity.MainActivity.Companion.NOTIFICATIONS_HISTORY
import io.golos.golos.screens.main_activity.MainActivity.Companion.PROFILE_FRAGMENT_POSITION
import io.golos.golos.screens.main_activity.MainActivity.Companion.STORIES_FRAGMENT_POSITION
import io.golos.golos.screens.profile.ProfileRootFragment
import io.golos.golos.screens.stories.DiscussionsListHolderFragment
import io.golos.golos.screens.tags.FilteredStoriesByTagFragment
import io.golos.golos.screens.widgets.GolosFragment

/**
 * Created by yuri on 05.12.17.
 */
class MainPagerAdapter(manager: FragmentManager) : FragmentStatePagerAdapter(manager) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            STORIES_FRAGMENT_POSITION -> DiscussionsListHolderFragment.getInstance()
            FILTERED_BY_TAG_STORIES -> FilteredStoriesByTagFragment()
            NOTIFICATIONS_HISTORY -> EventsHolderFragment()
            PROFILE_FRAGMENT_POSITION -> ProfileRootFragment()
            else -> GolosFragment()
        }
    }

    override fun getCount() = 4


}