package io.golos.golos.screens

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.view.ViewPager
import android.view.View
import io.golos.golos.R
import io.golos.golos.utils.showSnackbar
import java.util.*

class MainActivity : GolosActivity() {
    private var lastTimeTapped: Long = Date().time
    private var mDoubleBack = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_tabs)

        val pager: ViewPager = findViewById(R.id.content_pager)
        pager.adapter = MainPagerAdapter(supportFragmentManager)
        pager.offscreenPageLimit = 4
        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_nav_view)
        bottomNavView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.stories -> {
                    pager.currentItem = STORIES_FRAGMENT_POITION
                    true
                }
                R.id.groups -> {
                    bottomNavView.showSnackbar(R.string.unaval_in_current_version)
                    false
                }
                R.id.groups_1 -> {
                    bottomNavView.showSnackbar(R.string.unaval_in_current_version)
                    false
                }
                R.id.profile -> {
                    pager.currentItem = PROFILE_FRAGMENT_POITION
                    true
                }
                else -> true
            }
        }
    }

    override fun onBackPressed() {
        if ((Date().time - lastTimeTapped) > 3000) {
            mDoubleBack = false
            lastTimeTapped = Date().time
        }
        if (!mDoubleBack) {
            mDoubleBack = true
            findViewById<View>(R.id.content_pager)?.let {
                it.showSnackbar(R.string.tab_back_double_to_enter)
            }
        } else {
            super.onBackPressed()
        }

    }

    companion object {
        val STORIES_FRAGMENT_POITION = 0
        val PROFILE_FRAGMENT_POITION = 3
    }
}
