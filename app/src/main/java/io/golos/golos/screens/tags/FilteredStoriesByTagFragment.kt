package io.golos.golos.screens.tags

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
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
import io.golos.golos.utils.ReselectionEmitter
import io.golos.golos.utils.setViewGone
import io.golos.golos.utils.setViewVisible
import io.golos.golos.utils.toArrayList

/**
 * Created by yuri on 06.01.18.
 */
class FilteredStoriesByTagFragment : GolosFragment(),
        Observer<FilteredStoriesByTagViewModel>,
        ReselectionEmitter {
    private lateinit var mTagsReycler: RecyclerView
    private lateinit var mViewPager: ViewPager
    private lateinit var mNoTagsTv: TextView
    private lateinit var mPopularNowTv: TabLayout
    private var mLastTags = List(0) { LocalizedTag(Tag("", 0.0, 0L, 0L)) }
    private val mReselectionLiveData = MutableLiveData<Int>()

    override val reselectLiveData: LiveData<Int>
        get() = mReselectionLiveData

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.f_filtered_stories, container, false)
        mTagsReycler = v.findViewById(R.id.tags_recycler)
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

        (mTagsReycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        val viewModel = ViewModelProviders.of(this).get(FilteredStoriesByTagFragmentViewModel::class.java)
        viewModel.getTagsLiveData().observe(this, this)
        mTagsReycler.adapter = SubscribedTagsAdapter({ viewModel.onAddTagButtonClick(activity) },
                { viewModel.onTagClick(activity, it) },
                { viewModel.onTagDeleteClick(activity, it) })
        viewModel.onCreate()
        mTagsReycler.addItemDecoration(StartMarginDecorator())
        v.findViewById<View>(R.id.toolbar).setOnClickListener {
            viewModel.onTagSearchClick(activity)
        }
        return v
    }


    override fun onChanged(t: FilteredStoriesByTagViewModel?) {
        t?.let {
            val items = it.tags.toArrayList()
            if (it.tags.isEmpty()) {
                mViewPager.visibility = View.GONE
                mPopularNowTv.visibility = View.GONE
                mNoTagsTv.visibility = View.VISIBLE
                items.add(LocalizedTag(Tag("", 0.0, 0L, 0L)))
                (mTagsReycler.adapter as SubscribedTagsAdapter).tags = items
            } else {
                mViewPager.setViewVisible()
                mPopularNowTv.setViewVisible()
                mNoTagsTv.setViewGone()
                items.add(0, LocalizedTag(Tag("", 0.0, 0L, 0L)))
                (mTagsReycler.adapter as SubscribedTagsAdapter).tags = items
                if (it.tags != mLastTags) {
                    mLastTags = it.tags
                    val storyFilter = StoryFilter(it.tags.map { it.tag.name })
                    mViewPager.adapter = StoriesPagerAdapter(activity ?: return,
                            childFragmentManager,
                            listOf(Pair(FeedType.POPULAR, storyFilter),
                                    Pair(FeedType.NEW, storyFilter),
                                    Pair(FeedType.ACTUAL, storyFilter)))
                }
            }
        }
    }
}