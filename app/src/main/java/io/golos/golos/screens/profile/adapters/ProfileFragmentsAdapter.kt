package io.golos.golos.screens.profile.adapters

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import io.golos.golos.R
import io.golos.golos.screens.profile.PouchFragment
import io.golos.golos.screens.stories.StoriesFragment
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.widgets.GolosFragment

/**
 * Created by yuri on 08.12.17.
 */
class ProfileFragmentsAdapter(manager: FragmentManager, private val context: Context) : FragmentPagerAdapter(manager) {
    public val size = 3
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> StoriesFragment.getInstance(FeedType.BLOG)
            1 -> StoriesFragment.getInstance(FeedType.COMMENTS)
            2 -> PouchFragment()
            else -> GolosFragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> context.getString(R.string.blog)
            1 -> context.getString(R.string.answers)
            2 -> context.getString(R.string.pouch)
            else -> ""
        }
    }

    override fun getCount(): Int {
        return size
    }
}