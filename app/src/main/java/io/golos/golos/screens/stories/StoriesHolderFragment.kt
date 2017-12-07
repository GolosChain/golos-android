package io.golos.golos.screens.stories

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.profile.AuthViewModel
import io.golos.golos.screens.stories.adapters.StoriesPagerAdpater
import io.golos.golos.screens.widgets.GolosFragment


/**
 * Created by yuri on 30.10.17.
 */
class StoriesHolderFragment : GolosFragment() {
    private lateinit var mPager: ViewPager
    private lateinit var mFab: FloatingActionButton


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.f_stripes, container, false)
        setup(view)
        val authModel = ViewModelProviders.of(activity!!).get(AuthViewModel::class.java)
        authModel.userAuthState.observe(activity as LifecycleOwner, android.arch.lifecycle.Observer {
            if (it?.isLoggedIn == true) {
                if (mFab.visibility != View.VISIBLE) mFab.show()
                (mPager.adapter as? StoriesPagerAdpater)?.isFeedFragmentShown = true
            } else if (it?.isLoggedIn == false) {
                mFab.visibility = View.GONE
                (mPager.adapter as? StoriesPagerAdpater)?.isFeedFragmentShown = false
            }
        })
        return view
    }

    private fun setup(root: View) {
        mFab = root.findViewById(R.id.fab)
        mFab.setOnClickListener({
            context?.let {
                EditorActivity.startPostEditor(it, "")
            }
        })
        mPager = root.findViewById(R.id.view_pager)
        val adapter = StoriesPagerAdpater(root.context, childFragmentManager)
        mPager.offscreenPageLimit = 5
        mPager.adapter = adapter
        val tabLo: TabLayout = root.findViewById(R.id.tab_lo)
        tabLo.setupWithViewPager(mPager)
    }
}