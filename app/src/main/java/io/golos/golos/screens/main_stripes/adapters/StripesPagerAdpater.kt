package io.golos.golos.screens.main_stripes.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import io.golos.golos.screens.main_stripes.StripeFragment
import io.golos.golos.screens.main_stripes.model.StripeType

/**
 * Created by yuri on 31.10.17.
 */
class StripesPagerAdpater(manager: FragmentManager, private val supportedTypes: List<StripeType>) : FragmentPagerAdapter(manager) {
    private val typesToPosition = HashMap<StripeType, Int>()
    private val positionToFragments = HashMap<Int, Fragment>()

    override fun getItem(position: Int): Fragment {
        typesToPosition.put(supportedTypes[position], position)
        val fr = StripeFragment.getInstance(supportedTypes[position])
        positionToFragments.put(position, fr)
        return fr
    }

    override fun getCount() = supportedTypes.size
    fun onPageSelected(position: Int) {
        positionToFragments.forEach({

        })
    }
}