package io.golos.golos.screens.main_stripes

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.text.Html
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.drawer.AuthViewModel
import io.golos.golos.screens.drawer.NotLoggedInDrawerFragment
import io.golos.golos.screens.drawer.UserProfileDrawerFragment
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.main_stripes.adapters.StripesPagerAdpater
import io.golos.golos.screens.main_stripes.model.StripeType
import java.util.*


/**
 * Created by yuri on 30.10.17.
 */
class StripesActivity : GolosActivity() {
    private lateinit var mDrawer: DrawerLayout
    private lateinit var mDrawerPaneLo: ViewGroup
    private lateinit var mPager: ViewPager
    private lateinit var mFab: FloatingActionButton
    private var mDoubleBack = false
    private var lastTimeTapped: Long = Date().time
    lateinit private var mConnection: PagerAndSpinnerConnection
    private val mNotLoggedFTag = "mNotLoggedFTag"
    private val mUserProfileFTag = "mUserProfileFTag"
    private val listOfSupportedStripes = listOf(
            StripeType.POPULAR,
            StripeType.NEW,
            StripeType.ACTUAL,
            StripeType.PROMO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_stripes)
        setup()
        val authModel = ViewModelProviders.of(this).get(AuthViewModel::class.java)
        authModel.userAuthState.observe(this, android.arch.lifecycle.Observer {
            if (it?.isLoggedIn == true) {
                if (mFab.visibility != View.VISIBLE) mFab.show()
                if (supportFragmentManager.findFragmentByTag(mUserProfileFTag) == null) {
                    supportFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.fade_in, 0)
                            .replace(mDrawerPaneLo.id, UserProfileDrawerFragment.getInstance(), mUserProfileFTag)
                            .commit()
                }
            } else if (it?.isLoggedIn == false) {
                mFab.visibility = View.GONE
                if (supportFragmentManager.findFragmentByTag(mNotLoggedFTag) == null) {
                    supportFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.fade_in, 0)
                            .replace(mDrawerPaneLo.id, NotLoggedInDrawerFragment.getInstance(), mNotLoggedFTag)
                            .commit()
                }
            }
        })
        authModel.onCloseDrawerRequest.observe(this, android.arch.lifecycle.Observer { mDrawer.closeDrawer(Gravity.START) })
    }

    private fun setup() {
        mFab = findViewById(R.id.fab);
        mFab.setOnClickListener({ EditorActivity.startPostEditor(this, "") })
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        mDrawer = findViewById(R.id.drawer)
        val actionBarDrawerToggle = ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.app_name, R.string.app_name)
        actionBarDrawerToggle.syncState()
        mDrawerPaneLo = findViewById(R.id.drawer_pane_container)
        mPager = findViewById(R.id.view_pager)
        val adapter = StripesPagerAdpater(supportFragmentManager, listOfSupportedStripes)
        mPager.offscreenPageLimit = 4
        mPager.adapter = adapter
        mConnection = PagerAndSpinnerConnection(findViewById(R.id.spinner), mPager, adapter)
    }

    override fun onBackPressed() {
        if (mDrawer.isDrawerOpen(mDrawerPaneLo)) {
            mDrawer.closeDrawer(Gravity.START)
        } else {
            if ((Date().time - lastTimeTapped) > 3000) {
                mDoubleBack = false
                lastTimeTapped = Date().time
            }
            if (!mDoubleBack) {
                mDoubleBack = true
                Snackbar.make(findViewById(android.R.id.content),
                        Html.fromHtml("<font color=\"#ffffff\">${resources.getString(R.string.tab_back_double_to_enter)}</font>"),
                        Toast.LENGTH_SHORT).show()
            } else {
                super.onBackPressed()
            }
        }
    }
}