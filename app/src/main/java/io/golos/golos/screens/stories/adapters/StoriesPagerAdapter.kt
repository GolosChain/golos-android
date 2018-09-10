package io.golos.golos.screens.stories.adapters

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import io.golos.golos.R
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.stories.DiscussionsListFragment
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.widgets.GolosFragment
import timber.log.Timber

class StoriesPagerAdapter(val context: Context,
                          manager: FragmentManager,
                          private val supportedFeedTypes: List<Pair<FeedType, StoryFilter?>>) : FragmentStatePagerAdapter(manager) {

    fun enumerateSupportedFeedTypes() = supportedFeedTypes.map { it.first }


    override fun getItem(position: Int): Fragment {
        return if (position > supportedFeedTypes.lastIndex) GolosFragment()
        else {
            DiscussionsListFragment.getInstance(supportedFeedTypes[position].first, position, supportedFeedTypes[position].second)
        }
    }


    override fun getPageTitle(position: Int): CharSequence? {
        if (position > supportedFeedTypes.lastIndex) return null
        var context = context
        return when (supportedFeedTypes[position].first) {
            FeedType.PERSONAL_FEED -> context.getString(R.string.feed)
            FeedType.POPULAR -> context.getString(R.string.popular)
            FeedType.PROMO -> context.getString(R.string.promo)
            FeedType.ACTUAL -> context.getString(R.string.actual)
            FeedType.NEW -> context.getString(R.string.stripes_new)
            else -> ""
        }
    }

    override fun getItemPosition(`object`: Any): Int {

        return PagerAdapter.POSITION_NONE
    }

    override fun getCount() = supportedFeedTypes.size
}