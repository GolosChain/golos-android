package io.golos.golos.screens.tags

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.repository.model.Tag
import io.golos.golos.screens.tags.adapters.StartMarginDecorator
import io.golos.golos.screens.tags.adapters.SubscribedTagsAdapter
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.screens.tags.viewmodel.FilteredStoriesByTagFragmentViewModel
import io.golos.golos.screens.tags.viewmodel.FilteredStoriesByTagViewModel
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.toArrayList

/**
 * Created by yuri on 06.01.18.
 */
class FilteredStoriesByTagFragment : GolosFragment(), Observer<FilteredStoriesByTagViewModel> {
    private lateinit var mTagsReycler: RecyclerView
    private lateinit var mFragmentsFrame: FrameLayout
    private lateinit var mNoTagsTv: TextView
    private lateinit var mPopularNowTv: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.f_filtered_stories, container, false)
        mTagsReycler = v.findViewById(R.id.tags_recycler)
        mFragmentsFrame = v.findViewById(R.id.tags_fragment_frame)
        mNoTagsTv = v.findViewById(R.id.no_tags_chosen_tv)
        mPopularNowTv = v.findViewById(R.id.popular_now_tv)
        mTagsReycler.layoutManager = LinearLayoutManager(inflater.context, LinearLayoutManager.HORIZONTAL, false)
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
                mFragmentsFrame.visibility = View.GONE
                mPopularNowTv.visibility = View.GONE
                mNoTagsTv.visibility = View.VISIBLE
                items.add(LocalizedTag(Tag("", 0.0, 0L, 0L)))
                (mTagsReycler.adapter as SubscribedTagsAdapter).tags = items
            } else {
                mFragmentsFrame.visibility = View.VISIBLE
                mPopularNowTv.visibility = View.VISIBLE
                mNoTagsTv.visibility = View.GONE
                items.add(0, LocalizedTag(Tag("", 0.0, 0L, 0L)))
                (mTagsReycler.adapter as SubscribedTagsAdapter).tags = items
            }
        }
    }
}