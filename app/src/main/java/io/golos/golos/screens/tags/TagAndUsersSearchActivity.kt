package io.golos.golos.screens.tags

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.widget.SearchView
import android.view.View
import io.golos.golos.R
import io.golos.golos.repository.persistence.model.GolosUserWithAvatar
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.screens.tags.viewmodel.TagSearchViewModel
import io.golos.golos.screens.tags.viewmodel.TagSearchViewModelScreenState
import io.golos.golos.screens.tags.views.TagsAndUsersPager
import io.golos.golos.utils.setTextColorHint
import io.golos.golos.utils.setViewGone
import io.golos.golos.utils.setViewVisible


/**
 * Created by yuri on 10.01.18.
 */
class TagAndUsersSearchActivity : GolosActivity(), Observer<TagSearchViewModelScreenState> {
    private lateinit var mTagsSearchV: SearchView
    private lateinit var mUsersSearchV: SearchView
    private lateinit var mViewModel: TagSearchViewModel
    private lateinit var mTabbar: View
    private lateinit var mTagsAndUsersPager: TagsAndUsersPager
    private lateinit var mProgress: View
    private val mHandler = Handler()

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
                mTagsAndUsersPager.visibility = View.GONE
                mProgress.visibility = View.VISIBLE
            } else {
                mTabbar.visibility = View.VISIBLE
                mTagsAndUsersPager.visibility = View.VISIBLE
                mProgress.visibility = View.GONE
            }
            mTagsAndUsersPager.tags = it.shownTags.subList(0,
                    if (it.shownTags.size > 100) 100 else it.shownTags.size)
            mTagsAndUsersPager.users = it.shownUsers
        }
    }

    private fun setup() {
        mTagsSearchV = findViewById(R.id.search_view)
        mUsersSearchV = findViewById(R.id.users_search_view)
        mTagsSearchV.setFocusable(false)
        mUsersSearchV.setFocusable(false)
        mTagsSearchV.setTextColorHint(R.color.textColorP)
        mUsersSearchV.setTextColorHint(R.color.textColorP)
        mTagsSearchV.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                mViewModel.searchTag(p0 ?: "")
                return true
            }
        })
        mUsersSearchV.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                mHandler.removeCallbacksAndMessages(null)
                if (p0.isNullOrBlank()) mViewModel.searchUser("")
                else {
                    mHandler.postDelayed({ mViewModel.searchUser(p0 ?: "") }, 600)
                }
                return true
            }
        })

        mTagsSearchV.setOnCloseListener {
            mViewModel.onTagSearchEnd()
            false
        }
        mTagsSearchV.setOnQueryTextFocusChangeListener { v, isFocused ->
            if (v == mTagsSearchV && isFocused && mTagsSearchV.query.isEmpty()) mViewModel.onTagSearchStart()
            else if (v == mTagsSearchV && !isFocused && mTagsSearchV.query.isEmpty()) mViewModel.onTagSearchEnd()
        }
        mTabbar = findViewById(R.id.appbar)
        mProgress = findViewById(R.id.progress)
        mTagsAndUsersPager = findViewById(R.id.tags_and_users_recycler)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTagsAndUsersPager.isNestedScrollingEnabled = false
        }
        mTagsAndUsersPager.tagClickListener = object : TagsAndUsersPager.OnTagClickListener {
            override fun onClick(tag: LocalizedTag) {
                mViewModel.onTagClick(this@TagAndUsersSearchActivity, tag, false)
            }
        }
        mTagsAndUsersPager.userClickListener = object : TagsAndUsersPager.OnUserClickListener {
            override fun onClick(golosUser: GolosUserWithAvatar) {
                mViewModel.onUserClick(this@TagAndUsersSearchActivity, golosUser)
            }
        }
        findViewById<View>(R.id.back_btn).setOnClickListener { finish() }
        val tabLo: TabLayout = findViewById(R.id.tab_lo)
        tabLo.setupWithViewPager(mTagsAndUsersPager)
        mTagsAndUsersPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        if (mTagsSearchV.query.isNotEmpty()) {
                            mTagsSearchV.setViewVisible()

                        }
                        mTagsSearchV.setViewVisible()
                        mUsersSearchV.setViewGone()
                    }
                    1 -> {
                        if (mUsersSearchV.query.isNotEmpty()) {
                            mUsersSearchV.setViewVisible()

                        }
                        mTagsSearchV.setViewGone()
                        mUsersSearchV.setViewVisible()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.onDestroy()
    }

    companion object {
        public val TAG_TAG = "TAG_TAG"
        fun startForResult(activity: Activity, requestId: Int) {
            val i = Intent(activity, TagAndUsersSearchActivity::class.java)
            activity.startActivityForResult(i, requestId)
        }

        fun start(activity: Activity) {
            val i = Intent(activity, TagAndUsersSearchActivity::class.java)
            activity.startActivity(i)
        }
    }
}