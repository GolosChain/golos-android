package io.golos.golos.screens.main_stripes.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v7.widget.RecyclerView
import io.golos.golos.screens.main_stripes.StoriesFragment
import io.golos.golos.screens.main_stripes.StoriesFragment.Companion.TYPE_TAG
import io.golos.golos.screens.main_stripes.model.FeedType

class StripesPagerAdpater(manager: FragmentManager) : FragmentPagerAdapter(manager) {
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
                    f.arguments.putSerializable(TYPE_TAG, typesToPosition[actual])
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

    override fun getItemPosition(`object`: Any?): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getCount() = if (isFeedFragmentShown) FeedType.values().size else FeedType.values().size - 1

    companion object {
        var sharedPool = RecyclerView.RecycledViewPool()
    }
}