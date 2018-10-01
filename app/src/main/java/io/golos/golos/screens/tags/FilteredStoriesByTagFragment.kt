package io.golos.golos.screens.tags

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.repository.model.Tag
import io.golos.golos.screens.stories.adapters.StoriesPagerAdapter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.tags.adapters.StartMarginDecorator
import io.golos.golos.screens.tags.adapters.SubscribedTagsAdapter
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.screens.tags.viewmodel.FilteredStoriesByTagFragmentViewModel
import io.golos.golos.screens.tags.viewmodel.FilteredStoriesByTagViewModel
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.*

/**
 * Created by yuri on 06.01.18.
 */
class FilteredStoriesByTagFragment : GolosFragment(),
        Observer<FilteredStoriesByTagViewModel>,
        ReselectionEmitter {
    private lateinit var mTagsRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var mViewPager: ViewPager
    private lateinit var mNoTagsTv: TextView
    private lateinit var mPopularNowTv: TabLayout
    private var mLastTags = List(0) { LocalizedTag(Tag("", 0.0, 0L, 0L)) }
    private val mReselectionLiveData = MutableLiveData<Int>()
    private var mTagsSavedState: Parcelable? = null
    private var mViewModel: FilteredStoriesByTagFragmentViewModel? = null

    override val reselectLiveData: LiveData<Int>
        get() = mReselectionLiveData

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.f_filtered_stories, container, false)
        mTagsRecycler = v.findViewById(R.id.tags_recycler)
        mViewPager = v.findViewById(R.id.filtered_pager_lo)
        mNoTagsTv = v.findViewById(R.id.no_tags_chosen_tv)
        mPopularNowTv = v.findViewById<TabLayout>(R.id.tab_lo)
        mViewPager.offscreenPageLimit = 1
        mPopularNowTv.setupWithViewPager(mViewPager)

        mPopularNowTv.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                mReselectionLiveData.value = mPopularNowTv.selectedTabPosition
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
            }
        })

        (mTagsRecycler.itemAnimator as androidx.recyclerview.widget.SimpleItemAnimator).supportsChangeAnimations = false
        mTagsRecycler.layoutManager = MyLinearLayoutManager(activity!!, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        mViewModel = ViewModelProviders.of(activity!!).get(FilteredStoriesByTagFragmentViewModel::class.java)

        mViewModel?.getTagsLiveData()?.observe(this, this)
        mTagsRecycler.adapter = SubscribedTagsAdapter({
            mViewModel?.onAddTagButtonClick(activity)
        },
                { mViewModel?.onTagClick(activity, it) },
                { mViewModel?.onTagDeleteClick(activity, it) })
        mViewModel?.onCreate()
        mTagsRecycler.addItemDecoration(StartMarginDecorator())
        v.findViewById<View>(R.id.toolbar).setOnClickListener {
            mViewModel?.onTagSearchClick(activity)
        }
        return v
    }


    override fun onChanged(t: FilteredStoriesByTagViewModel?) {
        t?.let {
            val items = it.tags.toArrayList()
            if (it.tags.isEmpty()) {
                mViewPager.adapter = null
                mViewPager.visibility = View.GONE
                mPopularNowTv.visibility = View.GONE
                mNoTagsTv.visibility = View.VISIBLE
                items.add(LocalizedTag(Tag("", 0.0, 0L, 0L)))
                (mTagsRecycler.adapter as SubscribedTagsAdapter).tags = items
            } else {
                mViewPager.setViewVisible()
                mPopularNowTv.setViewVisible()
                mNoTagsTv.setViewGone()
                items.add(0, LocalizedTag(Tag("", 0.0, 0L, 0L)))
                (mTagsRecycler.adapter as SubscribedTagsAdapter).tags = items
                if (mTagsSavedState != null) {
                    mTagsRecycler.layoutManager?.onRestoreInstanceState(mTagsSavedState)
                    mTagsSavedState = null
                }

                if (!mLastTags.compareContents(it.tags)) {

                    mLastTags = it.tags
                    val storyFilter = StoryFilter(it.tags.map { it.tag.name })
                    mViewPager.adapter = StoriesPagerAdapter(activity ?: return,
                            childFragmentManager,
                            listOf(Pair(FeedType.POPULAR, storyFilter),
                                    Pair(FeedType.NEW, storyFilter),
                                    Pair(FeedType.ACTUAL, storyFilter)))

                    mViewPager.post {
                        mViewPager.currentItem = 1
                        mViewPager.currentItem = 0
                    }
                }
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        val parc = mTagsRecycler.layoutManager?.onSaveInstanceState()
        outState.putParcelable("FilteredStoriesByTagFragment", parc)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        mTagsSavedState = savedInstanceState?.getParcelable("FilteredStoriesByTagFragment")
    }
}