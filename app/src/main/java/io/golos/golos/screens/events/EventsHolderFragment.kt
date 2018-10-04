package io.golos.golos.screens.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.ReselectionEmitter
import io.golos.golos.utils.StringProvider
import timber.log.Timber

class EventsHolderFragment : GolosFragment(), ReselectionEmitter, ParentVisibilityChangeEmitter {
    private val mReselectLiveData = MutableLiveData<Int>()
    private var mPager: ViewPager? = null
    private val mStatusChange = MutableLiveData<VisibilityStatus>()

    override val reselectLiveData: LiveData<Int>
        get() = mReselectLiveData

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fr_events_holder, container, false)
        mPager = v.findViewById<ViewPager>(R.id.holder_view_pager)
        mPager?.adapter = EventsPagerAdapter(object : StringProvider {
            override fun get(resId: Int, args: String?): String {
                return getString(resId, args)
            }
        }, childFragmentManager)
        val tabLo = v.findViewById<TabLayout>(R.id.tab_lo)
        tabLo.setupWithViewPager(mPager)
        tabLo.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

                mReselectLiveData.value = tabLo.selectedTabPosition
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {

            }
        })
        mPager?.offscreenPageLimit = 1
        return v
    }

    override val status: LiveData<VisibilityStatus>
        get() = mStatusChange

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        mStatusChange.value = VisibilityStatus(isVisibleToUser, mPager?.currentItem ?: return)
    }
}

interface ParentVisibilityChangeEmitter {
    val status: LiveData<VisibilityStatus>
}

data class VisibilityStatus(val isParentVisible: Boolean,
                            val currentSelectedFragment: Int)
