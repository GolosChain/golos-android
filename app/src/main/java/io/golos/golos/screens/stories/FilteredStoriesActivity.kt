package io.golos.golos.screens.stories

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.repository.model.Tag
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.stories.adapters.StoriesPagerAdapter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.stories.model.FilteredStoriesViewModel
import io.golos.golos.screens.stories.model.FilteredStoriesViewState
import io.golos.golos.screens.tags.TagSubscriptionCancelDialogFr
import io.golos.golos.screens.tags.model.LocalizedTag
import io.golos.golos.utils.Translit
import io.golos.golos.utils.setViewGone
import io.golos.golos.utils.setViewVisible
import io.golos.golos.utils.toArrayList
import timber.log.Timber

class FilteredStoriesActivity : GolosActivity(), Observer<FilteredStoriesViewState>, TagSubscriptionCancelDialogFr.ResultListener {
    private lateinit var mViewPager: ViewPager
    private lateinit var mPostCountTv: TextView
    private lateinit var mTagTitle: TextView
    private lateinit var mSubscribeBtnLo: View
    private lateinit var mSubscribesBtnLo: View
    private lateinit var mSubscribeBtn: View
    private lateinit var mSubscribesBtn: View
    private lateinit var mSimilarTagsLo: ViewGroup
    private lateinit var mSimilarParentLo: ViewGroup
    private lateinit var mViewModel: FilteredStoriesViewModel
    private val FRAGMENT_TAG = "FRAGMENT_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_filtered_stories)
        mViewPager = findViewById(R.id.content_lo)

        val tagName = intent.getStringExtra(TAG_FILTER)

        if (tagName == null) {
            Timber.e("filter is null , this should not happen")
            finish()
            return
        }
        setup(tagName)
        mViewModel = ViewModelProviders.of(this).get(FilteredStoriesViewModel::class.java)
        mViewModel.getLiveData().observe(this, this)
        mViewModel.onCreate(tagName)
        mViewPager.offscreenPageLimit = 3
        val title = findViewById<TabLayout>(R.id.tab_lo)
        title.setupWithViewPager(mViewPager)
        val filter = StoryFilter(tagName)
        mViewPager.adapter = StoriesPagerAdapter(this,
                supportFragmentManager,
                listOf(Pair(FeedType.POPULAR, filter),
                        Pair(FeedType.NEW, filter),
                        Pair(FeedType.ACTUAL, filter)))
    }

    override fun onCancelConfirm() {
        mViewModel.onTagUnsubscribe()
    }

    override fun onCancelCancel() {
    }

    override fun onChanged(t: FilteredStoriesViewState?) {
        t?.let {
            mTagTitle.text = "#${it.mainTag.getLocalizedName().capitalize()}"
            if (it.postsCount == null) mPostCountTv.visibility = View.INVISIBLE
            else {
                mPostCountTv.visibility = View.VISIBLE
                mPostCountTv.text = "${it.postsCount} ${resources.getQuantityText(R.plurals.top_posts, it.postsCount.toInt())}"
            }
            if (it.isUserSubscribedOnThisTag) {
                mSubscribeBtnLo.setViewGone()
                mSubscribesBtnLo.setViewVisible()
                mSubscribesBtn.setOnClickListener {
                    TagSubscriptionCancelDialogFr.getInstance(t.mainTag).show(supportFragmentManager, "tag")
                }
            } else {
                mSubscribeBtnLo.setViewVisible()
                mSubscribesBtnLo.setViewGone()
                mSubscribeBtn.setOnClickListener { mViewModel.onMainTagSubscribe() }
            }
            val normalizedList = it.similarTags.toArrayList()
            normalizedList.add(0, LocalizedTag(Tag("", 0.0, 0L, 0L)))
            (1 until mSimilarTagsLo.childCount)
                    .forEach { index ->
                        if (index < normalizedList.size) {
                            (mSimilarTagsLo.getChildAt(index) as TextView).text = "#${normalizedList[index].getLocalizedName()}"
                            mSimilarTagsLo.getChildAt(index).setOnClickListener { mViewModel.onTagClick(this, normalizedList[index]) }
                        } else {
                            (mSimilarTagsLo.getChildAt(index) as TextView).text = ""
                            mSimilarTagsLo.getChildAt(index).setOnClickListener(null)
                        }
                    }
            if (it.similarTags.isEmpty()) mSimilarParentLo.setViewGone()
            else mSimilarParentLo.setViewVisible()
        }
    }

    private fun setup(tagName: String) {
        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener({ EditorActivity.startPostCreator(this, "") })
        mTagTitle = findViewById(R.id.title_text)
        mPostCountTv = findViewById(R.id.posts_count)
        mSubscribeBtnLo = findViewById(R.id.subscribe_btn_lo)
        mSubscribesBtnLo = findViewById(R.id.subsbs_btn_lo)
        mSubscribeBtn = findViewById(R.id.subscribe_tv)
        mSubscribesBtn = findViewById(R.id.subs_tv)
        mSimilarTagsLo = findViewById(R.id.similar_tags_lo)
        mSimilarParentLo = findViewById(R.id.similar_p_lo)

        findViewById<View>(R.id.back_ibtn).setOnClickListener { onBackPressed() }
        mTagTitle.text =
                if (tagName.startsWith("ru--")) {
                    "#${Translit.lat2Ru(tagName.substring(4)).capitalize()}"
                } else "#${tagName.capitalize()}"
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.onDestroy()
    }

    companion object {
        private val TAG_FILTER = "TAG_FILTER"
        fun start(context: Context,
                  tagName: String) {
            val intent = Intent(context, FilteredStoriesActivity::class.java)
            intent.putExtra(TAG_FILTER, tagName)
            context.startActivity(intent)
        }
    }
}
