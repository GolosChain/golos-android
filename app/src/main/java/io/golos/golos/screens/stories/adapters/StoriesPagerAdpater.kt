package io.golos.golos.screens.stories.adapters

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v7.widget.RecyclerView
import io.golos.golos.R
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.stories.StoriesFragment
import io.golos.golos.screens.stories.model.FeedType

class StoriesPagerAdpater(val context: Context, manager: FragmentManager) : FragmentPagerAdapter(manager) {
    private val typesToPosition = HashMap<Int, FeedType>()
    private val mFragments = HashMap<Int, StoriesFragment?>()

    init {
        typesToPosition.put(0, FeedType.PERSONAL_FEED)
        typesToPosition.put(1, FeedType.NEW)
        typesToPosition.put(2, FeedType.ACTUAL)
        typesToPosition.put(3, FeedType.POPULAR)
        typesToPosition.put(4, FeedType.PROMO)
    }

    var isFeedFragmentShown: Pair<Boolean, String?> = Pair(false, null)
        set(value) {
            var old = field
            field = value
            if (old != value) {
                mFragments
                        .forEach({ v ->
                            var actual = v.key
                            if (!isFeedFragmentShown.first) actual += 1
                            if (actual < typesToPosition.size) {
                                val type = typesToPosition[actual]
                                if (type == FeedType.PERSONAL_FEED) {
                                    v.value?.arguments = StoriesFragment.createArguments(typesToPosition[actual]!!,
                                            StoryFilter(userNameFilter = field.second))
                                } else v.value?.arguments = StoriesFragment.createArguments(typesToPosition[actual]!!)
                            }
                        })
                notifyDataSetChanged()
            }
        }

    override fun getItem(position: Int): Fragment {
        var actualPosition = position
        if (!isFeedFragmentShown.first) actualPosition += 1
        val type = typesToPosition[actualPosition]
        val fr = if (type == FeedType.PERSONAL_FEED) StoriesFragment.getInstance(FeedType.PERSONAL_FEED, StoryFilter(
                userNameFilter = isFeedFragmentShown.second ?: ""))
        else StoriesFragment.getInstance(typesToPosition[actualPosition]!!)
        mFragments.put(position, fr)
        return fr
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getPageTitle(position: Int): CharSequence? {
        var actualPosition = position
        if (!isFeedFragmentShown.first) actualPosition += 1
        val type = typesToPosition[actualPosition]
        var context = context
        return when (type) {
            FeedType.PERSONAL_FEED -> context.getString(R.string.feed)
            FeedType.POPULAR -> context.getString(R.string.popular)
            FeedType.PROMO -> context.getString(R.string.promo)
            FeedType.ACTUAL -> context.getString(R.string.actual)
            FeedType.NEW -> context.getString(R.string.stripes_new)
            else -> ""
        }
    }

    override fun getCount() = if (isFeedFragmentShown.first) 5 else 4

    companion object {
        var sharedPool = RecyclerView.RecycledViewPool()
    }
}