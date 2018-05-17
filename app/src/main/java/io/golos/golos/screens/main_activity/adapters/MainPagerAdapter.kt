package io.golos.golos.screens.main_activity.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import io.golos.golos.screens.main_activity.MainActivity.Companion.FILTERED_BY_TAG_STORIES
import io.golos.golos.screens.main_activity.MainActivity.Companion.PROFILE_FRAGMENT_POSITION
import io.golos.golos.screens.main_activity.MainActivity.Companion.STORIES_FRAGMENT_POSITION
import io.golos.golos.screens.profile.ProfileRootFragment
import io.golos.golos.screens.stories.StoriesHolderFragment
import io.golos.golos.screens.tags.FilteredStoriesByTagFragment
import io.golos.golos.screens.widgets.GolosFragment

/**
 * Created by yuri on 05.12.17.
 */
class MainPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            STORIES_FRAGMENT_POSITION -> StoriesHolderFragment()
            FILTERED_BY_TAG_STORIES -> FilteredStoriesByTagFragment()
            2 -> GolosFragment()
            PROFILE_FRAGMENT_POSITION -> ProfileRootFragment()
            else -> GolosFragment()
        }
    }

    override fun getCount() = 4
}