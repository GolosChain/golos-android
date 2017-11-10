package io.golos.golos.screens.main_stripes

import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewPager
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import io.golos.golos.screens.main_stripes.adapters.StripesPagerAdpater
import timber.log.Timber

/**
 * Created by yuri on 02.11.17.
 */
class PagerAndSpinnerConnection(private val spinner: Spinner,
                                private val pager: ViewPager,
                                private val adapter: StripesPagerAdpater) : ViewPager.OnPageChangeListener {

    init {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                pager.setCurrentItem(position, true)
            }
        }
        pager.addOnPageChangeListener(this)
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        adapter.onPageSelected(position)
        if (spinner.selectedItemPosition != position) {
            spinner.setSelection(position)
        }
    }
}