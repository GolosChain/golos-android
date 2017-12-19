package io.golos.golos.screens.stories

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.repository.StoryFilter
import io.golos.golos.repository.model.mapper
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.profile.viewmodel.AuthViewModel
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.utils.Translit
import timber.log.Timber

class FilteredStoriesActivity : GolosActivity() {
    private lateinit var mContentFrameLo: FrameLayout
    private val FRAGMENT_TAG = "FRAGMENT_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_filtered_stories)
        mContentFrameLo = findViewById(R.id.content_lo)

        val type: FeedType = intent.getSerializableExtra(TAG_FEED) as FeedType
        val filterString = intent.getStringExtra(TAG_FILTER)
        val filter: StoryFilter? = if (filterString == null || filterString == "null") null
        else mapper.readValue(filterString, StoryFilter::class.java)
        if (filter == null) {
            Timber.e("filter is null , this should not happen")
            finish()
            return
        }
        setup(filter)
        var fragment = StoriesFragment.getInstance(type, filter)
        supportFragmentManager
                .beginTransaction()
                .replace(mContentFrameLo.id, fragment, FRAGMENT_TAG)
                .commit()


    }

    private fun setup(filter: StoryFilter) {
        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener({ EditorActivity.startPostEditor(this, "") })
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        val titileTv: TextView = findViewById(R.id.title_text)
        titileTv.text = if (filter.tagFilter != null) {
            var text = filter.tagFilter
            if (text.startsWith("ru--")) text = Translit.lat2Ru(text.substring(4))
            getString(R.string.tag_search) + " " +
                    text.capitalize()
        } else getString(R.string.stories)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        val authModel = ViewModelProviders.of(this).get(AuthViewModel::class.java)
        authModel.userAuthState.observe(this, android.arch.lifecycle.Observer {
            if (it?.isLoggedIn == true) {
                if (fab.visibility != View.VISIBLE) fab.show()
            } else if (it?.isLoggedIn == false) {
                fab.visibility = View.GONE
            }
        })

    }

    override fun onResume() {
        super.onResume()
        supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)?.let {
            it.userVisibleHint = true
        }
    }

    companion object {
        private val TAG_FEED = "TAG_FEED"
        private val TAG_FILTER = "TAG_FILTER"
        fun start(context: Context,
                  feedType: FeedType,
                  filter: StoryFilter) {
            val intent = Intent(context, FilteredStoriesActivity::class.java)
            intent.putExtra(TAG_FEED, feedType)
            intent.putExtra(TAG_FILTER, mapper.writeValueAsString(filter))
            context.startActivity(intent)
        }
    }
}
