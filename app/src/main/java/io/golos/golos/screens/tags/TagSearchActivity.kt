package io.golos.golos.screens.tags

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.View
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.tags.adapters.SearchTagAdapter
import io.golos.golos.screens.tags.viewmodel.TagSearchViewModel
import io.golos.golos.screens.tags.viewmodel.TagSearchViewModelScreenState
import io.golos.golos.utils.setTextColorHint


/**
 * Created by yuri on 10.01.18.
 */
class TagSearchActivity : GolosActivity(), Observer<TagSearchViewModelScreenState> {
    private lateinit var mSearchV: SearchView
    private lateinit var mViewModel: TagSearchViewModel
    private lateinit var mTabbar: View
    private lateinit var mRecycler: RecyclerView
    private lateinit var mProgress: View
    private lateinit var mScrollLO: View
    private lateinit var mRecomendedLabel: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_tags_search)
        mViewModel = ViewModelProviders.of(this).get(TagSearchViewModel::class.java)
        setup()
        mViewModel.getLiveData().observe(this, this)
        mViewModel.onCreate()
    }

    override fun onChanged(t: TagSearchViewModelScreenState?) {
        t?.let {
            if (it.isLoading) {
                mTabbar.visibility = View.GONE
                mScrollLO.visibility = View.GONE
                mProgress.visibility = View.VISIBLE
            } else {
                mTabbar.visibility = View.VISIBLE
                mScrollLO.visibility = View.VISIBLE
                mProgress.visibility = View.GONE
            }
            (mRecycler.adapter as SearchTagAdapter).tags = it.shownTags.subList(0,
                    if (it.shownTags.size > 100) 100 else it.shownTags.size)
            mRecomendedLabel.visibility = if (mSearchV.query.isEmpty() && it.shownTags.isNotEmpty()) View.VISIBLE
            else View.GONE
        }
    }

    private fun setup() {
        mSearchV = findViewById(R.id.search_view)
        mSearchV.setFocusable(false)
        mSearchV.setTextColorHint(R.color.textColorP)
        mSearchV.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                mViewModel.search(p0 ?: "")
                return true
            }
        })

        mSearchV.setOnCloseListener {
            mViewModel.onSearchEnd()
            false
        }
        mSearchV.setOnQueryTextFocusChangeListener({ v, isFocused ->
            if (v == mSearchV && isFocused && mSearchV.query.isEmpty()) mViewModel.onSearchStart()
            else if (v == mSearchV && !isFocused && mSearchV.query.isEmpty()) mViewModel.onSearchEnd()
        })
        mTabbar = findViewById(R.id.appbar)
        mProgress = findViewById(R.id.progress)
        mRecycler = findViewById(R.id.recycler)
        mScrollLO = findViewById(R.id.scroll_lo)
        mRecomendedLabel = findViewById(R.id.recommended_tv)
        mRecycler.layoutManager = LinearLayoutManager(this)
        mRecycler.adapter = SearchTagAdapter({ mViewModel.onTagClick(this, it, callingActivity != null) })
        findViewById<View>(R.id.back_btn).setOnClickListener { finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.onDestroy()
    }

    companion object {
        public val TAG_TAG = "TAG_TAG"
        fun startForResult(activity: Activity, requestId: Int) {
            val i = Intent(activity, TagSearchActivity::class.java)
            activity.startActivityForResult(i, requestId)
        }

        fun startt(activity: Activity) {
            val i = Intent(activity, TagSearchActivity::class.java)
            activity.startActivity(i)
        }
    }
}