package io.golos.golos.screens.stories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.main_activity.FeedTypePreselect
import io.golos.golos.screens.profile.viewmodel.AuthViewModel
import io.golos.golos.screens.stories.adapters.StoriesPagerAdapter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.ReselectionEmitter


/**
 * Created by yuri on 30.10.17.
 */


class DiscussionsListHolderFragment : GolosFragment(), ReselectionEmitter {
    private lateinit var mPager: ViewPager
    private lateinit var mFab: FloatingActionButton
    private val mFeedPositions: List<FeedType>
    private lateinit var mAuthModel: AuthViewModel
    private lateinit var mTabLo: TabLayout
    private val mReselectEmitter = MutableLiveData<Int>()

    init {
        mFeedPositions = (ArrayList<FeedType>().apply {
            add(FeedType.PERSONAL_FEED)
            add(FeedType.NEW)
            add(FeedType.ACTUAL)
            add(FeedType.POPULAR)
            add(FeedType.PROMO)
        })
    }

    override val reselectLiveData: LiveData<Int>
        get() = mReselectEmitter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.f_stripes, container, false)
        setup(view)
        mAuthModel = ViewModelProviders.of(activity!!).get(AuthViewModel::class.java)
        mAuthModel.userAuthState.observe(this, androidx.lifecycle.Observer {
            if (it?.isLoggedIn == true) {
                if (mFab.visibility != View.VISIBLE) mFab.show()
                if (mPager.adapter == null
                        || (mPager.adapter as? StoriesPagerAdapter)?.enumerateSupportedFeedTypes()?.contains(FeedType.PERSONAL_FEED) == false) {

                    mPager.adapter = createPagerAdapter(true)
                    mPager.post {
                        mPager.adapter?.notifyDataSetChanged()
                        mTabLo.setupWithViewPager(mPager)
                    }

                }
            } else {
                mFab.visibility = View.GONE
                if (mPager.adapter == null
                        || (mPager.adapter as? StoriesPagerAdapter)?.enumerateSupportedFeedTypes()?.contains(FeedType.PERSONAL_FEED) == true) {
                    mPager.adapter = createPagerAdapter(false)
                    mPager.post {
                        mPager.adapter?.notifyDataSetChanged()
                        mTabLo.setupWithViewPager(mPager)
                    }
                }
            }
        })

        (activity as? FeedTypePreselect)?.getSelection()?.observe(this, Observer {
            val feedType = it ?: return@Observer
            if (feedType == FeedType.PERSONAL_FEED && mAuthModel.userAuthState.value?.isLoggedIn != true) return@Observer
            val position = mFeedPositions.map {
                if (it == FeedType.PERSONAL_FEED
                        && mAuthModel.userAuthState.value?.isLoggedIn != true) null else it
            }
                    .filter { it != null }
                    .indexOf(feedType)
            if (position == -1) return@Observer
            mPager.post {
                val adapter = mPager.adapter ?: return@post
                if (position < adapter.count) mPager.setCurrentItem(position, true)
            }
        })
        return view
    }


    private fun setup(root: View) {
        mFab = root.findViewById(R.id.fab)
        mFab.setOnClickListener {
            context?.let {
                EditorActivity.startPostCreator(it, "")
            }
        }
        mPager = root.findViewById(R.id.holder_view_pager)
        mPager.offscreenPageLimit = 1
        mTabLo = root.findViewById(R.id.tab_lo)
        mTabLo.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                mReselectEmitter.value = mTabLo.selectedTabPosition
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {

            }
        })
        mTabLo.setupWithViewPager(mPager)
    }


    private fun createPagerAdapter(isUserLoggedIn: Boolean): StoriesPagerAdapter? {
        val supportedTypes: List<Pair<FeedType, StoryFilter?>> = mFeedPositions.map {
            if (it == FeedType.PERSONAL_FEED) {
                if (isUserLoggedIn)
                    Pair(FeedType.PERSONAL_FEED, StoryFilter(null,
                            Repository.get.appUserData.value?.name
                                    ?: ""))
                else null
            } else Pair(it, null)
        }.filter { it != null }.map { it!! }
        return StoriesPagerAdapter(activity ?: return null, childFragmentManager, supportedTypes)
    }

    companion object {
        fun getInstance(): DiscussionsListHolderFragment {
            return DiscussionsListHolderFragment()
        }
    }
}

