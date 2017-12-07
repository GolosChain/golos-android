package io.golos.golos.screens.stories

import android.content.Context
import android.support.v4.view.ViewPager
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import io.golos.golos.R
import io.golos.golos.screens.stories.adapters.StoriesPagerAdpater

/**
 * Created by yuri on 02.11.17.
 */
class PagerAndSpinnerConnection(private val spinner: Spinner,
                                private val pager: ViewPager,
                                private val adapter: StoriesPagerAdpater) : ViewPager.OnPageChangeListener {

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
        if (spinner.selectedItemPosition != position) {
            spinner.setSelection(position)
        }
    }

    fun onLoggedIn(context: Context) {
        spinner.adapter = ArrayAdapter<String>(context,
                android.R.layout.simple_list_item_1,
                context.resources.getStringArray(R.array.header_spinner_items_full))
    }

    fun onLoggedOut(context: Context) {
        spinner.adapter = ArrayAdapter<String>(context,
                android.R.layout.simple_list_item_1,
                context.resources.getStringArray(R.array.header_spinner_items))

    }
}