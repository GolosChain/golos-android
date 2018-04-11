package io.golos.golos.screens

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.design.widget.BottomNavigationView
import android.support.v4.view.ViewPager
import android.view.View
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.utils.asIntentToShareString
import io.golos.golos.utils.showSnackbar
import java.util.*

class MainActivity : GolosActivity(), Observer<CreatePostResult> {
    private var lastTimeTapped: Long = Date().time
    private var mDoubleBack = false
    private lateinit var mInstanceIdTv: TextView
    private lateinit var mLastMessageTv: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_tabs)

       /* mInstanceIdTv = findViewById(R.id.i_id_tv)
        mLastMessageTv = findViewById(R.id.last_m_tv)
        mLastMessageTv.text = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("data", "")
        mInstanceIdTv.text = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("token", "")


        fun makeHadler(){
            Handler().postDelayed({
                mLastMessageTv.text = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("data", "")
                mInstanceIdTv.text = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("token", "")
                makeHadler()
            }, 5000)
        }
        makeHadler()*/

      /*  mInstanceIdTv.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(p0: View?): Boolean {
                val text = mInstanceIdTv.text.toString()
                startActivity(text.asIntentToShareString())
                return true
            }

        })

        mLastMessageTv.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(p0: View?): Boolean {
                val text = mLastMessageTv.text.toString()
                startActivity(text.asIntentToShareString())
                startActivity(text.asIntentToShareString())
                return true
            }
        })*/

        val pager: ViewPager = findViewById(R.id.content_pager)
        pager.adapter = MainPagerAdapter(supportFragmentManager)
        pager.offscreenPageLimit = 4

        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_nav_view)
        bottomNavView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.stories -> {
                    pager.currentItem = STORIES_FRAGMENT_POITION
                    true
                }
                R.id.groups -> {
                    pager.currentItem = FILTERED_BY_TAG_STORIES
                    true
                }
                R.id.notifications -> {
                    bottomNavView.showSnackbar(R.string.unaval_in_current_version)
                    false
                }
                R.id.add -> {
                    if (!Repository.get.isUserLoggedIn()) {
                        bottomNavView.showSnackbar(R.string.must_be_logged_in_for_this_action)
                        false
                    } else {
                        EditorActivity.startPostCreator(this, "")
                        false
                    }
                }
                R.id.profile -> {
                    pager.currentItem = PROFILE_FRAGMENT_POITION
                    true
                }
                else -> true
            }
        }
        Repository.get.lastCreatedPost().observe(this, this)

    }

    override fun onChanged(it: CreatePostResult?) {
        if (it?.isPost == true) {
            StoryActivity.start(this,
                    it.author, it.blog, it.permlink, FeedType.UNCLASSIFIED, null)
        }
    }

    override fun onBackPressed() {
        if ((Date().time - lastTimeTapped) > 3000) {
            mDoubleBack = false
            lastTimeTapped = Date().time
        }
        if (!mDoubleBack) {
            mDoubleBack = true
            findViewById<View>(R.id.content_pager)?.let {
                it.showSnackbar(R.string.tab_back_double_to_enter)
            }
        } else {
            super.onBackPressed()
        }

    }

    companion object {
        val STORIES_FRAGMENT_POITION = 0
        val FILTERED_BY_TAG_STORIES = 1
        val PROFILE_FRAGMENT_POITION = 3

    }
}
