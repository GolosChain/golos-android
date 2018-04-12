package io.golos.golos.screens

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.design.widget.BottomNavigationView
import android.support.v4.view.ViewPager
import android.view.View
import android.widget.TextView
import com.google.firebase.messaging.FirebaseMessaging
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.repository.persistence.model.AppUserData
import io.golos.golos.screens.editor.EditorActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity
import io.golos.golos.utils.asIntentToShareString
import io.golos.golos.utils.setViewGone
import io.golos.golos.utils.setViewVisible
import io.golos.golos.utils.showSnackbar
import timber.log.Timber
import java.util.*

class MainActivity : GolosActivity(), Observer<CreatePostResult> {
    private var lastTimeTapped: Long = Date().time
    private var mDoubleBack = false
    private lateinit var mInstanceIdTv: TextView
    private lateinit var mLastMessageTv: TextView
    private lateinit var mTopicTV: TextView
    private val mHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_tabs)

        mInstanceIdTv = findViewById(R.id.i_id_tv)
        mLastMessageTv = findViewById(R.id.last_m_tv)
        mTopicTV = findViewById(R.id.topic_tv)
        mLastMessageTv.text = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("data", "")
        mInstanceIdTv.text = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("token", "")
        mTopicTV.text = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("topic", "")
        findViewById<View>(R.id.show_modal).setOnClickListener {
            val modalLo: View = findViewById(R.id.modal_win)
            if (modalLo.visibility == View.VISIBLE) {
                mHandler.removeCallbacksAndMessages(null)
                modalLo.setViewGone()
            } else {
                modalLo.setViewVisible()
                fun makeHadler() {
                    mHandler.postDelayed({
                        mLastMessageTv.text = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("data", "")
                        mInstanceIdTv.text = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("token", "")
                        mTopicTV.text = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("topic", "")
                        makeHadler()
                    }, 5000)
                }
                makeHadler()
            }
        }



        mInstanceIdTv.setOnLongClickListener(object : View.OnLongClickListener {
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
                return true
            }
        })
        mTopicTV.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(p0: View?): Boolean {
                val text = mTopicTV.text.toString()
                startActivity(text.asIntentToShareString())
                return true
            }
        })
        Repository.get.getCurrentUserDataAsLiveData().observeForever(object : Observer<AppUserData> {
            override fun onChanged(t: AppUserData?) {
                val userName = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("topic", null)
                if (t?.isUserLoggedIn == true) {
                    if (userName != null) return
                    FirebaseMessaging.getInstance().subscribeToTopic(t.userName)
                    PreferenceManager.getDefaultSharedPreferences(baseContext).edit().putString("topic", (t.userName)).commit()
                } else if (t?.isUserLoggedIn == false) {
                    val userName = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("topic", null)
                    if (userName != null) {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(userName)
                        PreferenceManager.getDefaultSharedPreferences(baseContext).edit().putString("topic", null).commit()
                    }
                }
            }
        })

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
