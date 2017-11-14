package io.golos.golos.screens.main_stripes.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v7.widget.RecyclerView
import io.golos.golos.screens.main_stripes.StripeFragment
import io.golos.golos.screens.main_stripes.StripeFragment.Companion.TYPE_TAG
import io.golos.golos.screens.main_stripes.model.StripeFragmentType

class StripesPagerAdpater(manager: FragmentManager) : FragmentPagerAdapter(manager) {
    private val typesToPosition = HashMap<Int, StripeFragmentType>()
    private val mFragments = ArrayList<StripeFragment>()

    init {
        typesToPosition.put(0, StripeFragmentType.FEED)
        typesToPosition.put(1, StripeFragmentType.POPULAR)
        typesToPosition.put(2, StripeFragmentType.ACTUAL)
        typesToPosition.put(3, StripeFragmentType.PROMO)
        typesToPosition.put(4, StripeFragmentType.NEW)
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
        val fr = StripeFragment.getInstance(typesToPosition[actualPosition]!!)
        mFragments.add(position, fr)
        return fr
    }

    override fun getItemPosition(`object`: Any?): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getCount() = if (isFeedFragmentShown) StripeFragmentType.values().size else StripeFragmentType.values().size - 1

    companion object {
        var sharedPool = RecyclerView.RecycledViewPool()
    }
}