package io.golos.golos.screens

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import io.golos.golos.screens.MainActivity.Companion.PROFILE_FRAGMENT_POITION
import io.golos.golos.screens.MainActivity.Companion.STORIES_FRAGMENT_POITION
import io.golos.golos.screens.profile.ProfileRootFragment
import io.golos.golos.screens.stories.StoriesHolderFragment
import io.golos.golos.screens.widgets.GolosFragment

/**
 * Created by yuri on 05.12.17.
 */
class MainPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            STORIES_FRAGMENT_POITION -> StoriesHolderFragment()
            1 -> GolosFragment()
            2 -> GolosFragment()
            3 -> GolosFragment()
            PROFILE_FRAGMENT_POITION -> ProfileRootFragment()
            else -> GolosFragment()
        }
    }

    override fun getCount() = 5
}