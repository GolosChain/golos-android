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
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.profile.viewmodel.AuthViewModel
import io.golos.golos.screens.stories.adapters.StoriesPagerAdapter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.widgets.GolosFragment
import timber.log.Timber


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
                if (mPager.adapter == null
                        || (mPager.adapter as? StoriesPagerAdapter)?.enumerateSupportedFeedTypes()?.contains(FeedType.PERSONAL_FEED) == false) {
                    mPager.adapter = createPagerAdapter(true)
                    mPager.adapter?.notifyDataSetChanged()
                }
            } else {
                mFab.visibility = View.GONE
                if (mPager.adapter == null
                        || (mPager.adapter as? StoriesPagerAdapter)?.enumerateSupportedFeedTypes()?.contains(FeedType.PERSONAL_FEED) == true) {
                    mPager.adapter = createPagerAdapter(false)
                    mPager.adapter?.notifyDataSetChanged()
                }
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
        mPager.offscreenPageLimit = 5
        val tabLo: TabLayout = root.findViewById(R.id.tab_lo)
        tabLo.setupWithViewPager(mPager)
    }


    private fun createPagerAdapter(isUserLoggedIn: Boolean): StoriesPagerAdapter? {
        val supportedTypes = ArrayList<Pair<FeedType, StoryFilter?>>(5)
        supportedTypes.apply {
            if (isUserLoggedIn) add(Pair(FeedType.PERSONAL_FEED, StoryFilter(null, Repository.get.getCurrentUserDataAsLiveData().value?.userName ?: "")))
            add(Pair(FeedType.NEW, null))
            add(Pair(FeedType.ACTUAL, null))
            add(Pair(FeedType.POPULAR, null))
            add(Pair(FeedType.PROMO, null))
        }
        return StoriesPagerAdapter(activity ?: return null, childFragmentManager, supportedTypes)
    }



}