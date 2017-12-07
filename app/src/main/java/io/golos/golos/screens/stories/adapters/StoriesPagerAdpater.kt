package io.golos.golos.screens.stories.adapters

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v7.widget.RecyclerView
import io.golos.golos.R
import io.golos.golos.screens.stories.StoriesFragment
import io.golos.golos.screens.stories.StoriesFragment.Companion.TYPE_TAG
import io.golos.golos.screens.stories.model.FeedType

class StoriesPagerAdpater(val context: Context, manager: FragmentManager) : FragmentPagerAdapter(manager) {
    private val typesToPosition = HashMap<Int, FeedType>()
    private val mFragments = ArrayList<StoriesFragment>()

    init {
        typesToPosition.put(0, FeedType.PERSONAL_FEED)
        typesToPosition.put(1, FeedType.POPULAR)
        typesToPosition.put(2, FeedType.ACTUAL)
        typesToPosition.put(3, FeedType.PROMO)
        typesToPosition.put(4, FeedType.NEW)
    }

    var isFeedFragmentShown: Boolean = false
        set(value) {
            var old = field
            field = value
            if (old != value) {
                mFragments.forEachIndexed({ i, f ->
                    var actual = i
                    if (!isFeedFragmentShown) actual += 1
                    f.arguments!!.putSerializable(TYPE_TAG, typesToPosition[actual])
                })
                notifyDataSetChanged()
            }
        }

    override fun getItem(position: Int): Fragment {
        var actualPosition = position
        if (!isFeedFragmentShown) actualPosition += 1
        val fr = StoriesFragment.getInstance(typesToPosition[actualPosition]!!)
        mFragments.add(position, fr)
        return fr
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getPageTitle(position: Int): CharSequence? {
        var actualPosition = position
        if (!isFeedFragmentShown) actualPosition += 1
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

    override fun getCount() = if (isFeedFragmentShown) FeedType.values().size else FeedType.values().size - 1

    companion object {
        var sharedPool = RecyclerView.RecycledViewPool()
    }
}