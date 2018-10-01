package io.golos.golos.screens.profile.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import io.golos.golos.R
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.profile.UserInformationFragment
import io.golos.golos.screens.stories.DiscussionsListFragment
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.widgets.GolosFragment

/**
 * Created by yuri on 08.12.17.
 */
class ProfileFragmentsAdapter(manager: FragmentManager,
                              private val context: Context,
                              private val profileUserName: String) : FragmentStatePagerAdapter(manager) {
    val size = 4
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> DiscussionsListFragment.getInstance(FeedType.BLOG, position, StoryFilter(userNameFilter = profileUserName))
            1 -> DiscussionsListFragment.getInstance(FeedType.COMMENTS, position, StoryFilter(userNameFilter = profileUserName))
            2 -> DiscussionsListFragment.getInstance(FeedType.ANSWERS, position, StoryFilter(userNameFilter = profileUserName))
            3 -> UserInformationFragment()
            else -> GolosFragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> context.getString(R.string.blog)
            1 -> context.getString(R.string.comments)
            2 -> context.getString(R.string.answers)
            3 -> context.getString(R.string.information)
            else -> ""
        }
    }

    fun getPositionOf(feedType: FeedType?) = when (feedType) {
        FeedType.BLOG -> 0
        FeedType.COMMENTS -> 1
        FeedType.ANSWERS -> 2
        else -> null
    }

    override fun getCount(): Int {
        return size
    }
}