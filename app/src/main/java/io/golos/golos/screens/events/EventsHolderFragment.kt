package io.golos.golos.screens.events

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.ReselectionEmitter
import io.golos.golos.utils.StringProvider
import timber.log.Timber


class EventsHolderFragment : GolosFragment(), ReselectionEmitter, ParentVisibilityChangeEmitter {
    private val mReselectLiveData = MutableLiveData<Int>()
    private var mPager: ViewPager? = null
    private lateinit var mToolbar: androidx.appcompat.widget.Toolbar
    private val mStatusChange = MutableLiveData<VisibilityStatus>()
    private var lastSelectedFragment: Int = 0

    override val reselectLiveData: LiveData<Int>
        get() = mReselectLiveData

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fr_events_holder, container, false)
        mPager = v.findViewById<ViewPager>(R.id.holder_view_pager)
        mToolbar = v.findViewById<Toolbar>(R.id.events_toolabar)

        (activity as AppCompatActivity).setSupportActionBar(mToolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        setHasOptionsMenu(true)
        mPager?.adapter = EventsPagerAdapter(object : StringProvider {
            override fun get(resId: Int, args: String?): String {
                return getString(resId, args)
            }
        }, childFragmentManager)
        mToolbar.menu
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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.events_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.mark_all_read) {
            Timber.e("on all read click")
            Repository.get.setAllEventsRead()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override val status: LiveData<VisibilityStatus>
        get() = mStatusChange

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        mStatusChange.value = VisibilityStatus(isVisibleToUser, mPager?.currentItem
                ?: lastSelectedFragment)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("lastSelectedFragment", lastSelectedFragment)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        lastSelectedFragment = savedInstanceState?.getInt("lastSelectedFragment", 0) ?: 0
    }
}

interface ParentVisibilityChangeEmitter {
    val status: LiveData<VisibilityStatus>
}

data class VisibilityStatus(val isParentVisible: Boolean,
                            val currentSelectedFragment: Int)
